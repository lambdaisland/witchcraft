(ns lambdaisland.witchcraft.events
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.util :as util]
            [clojure.java.classpath :as cp]
            [clojure.string :as str])
  (:import (java.util.jar JarFile JarEntry)
           (org.bukkit Bukkit)))

(def event-classes (sequence
                    (comp
                     (mapcat #(iterator-seq (.entries ^JarFile %)))
                     (map #(.getName ^JarEntry %))
                     (filter #(re-find #"(bukkit|paper|spigot).*event.*Event\.class" %))
                     (map #(-> % (str/replace #"/" ".") (str/replace #".class$" ""))))
                    (cp/classpath-jarfiles)))

(defn class->kw [name]
  (-> name
      (str/replace #".*\." "")
      (str/replace #"Event$" "")
      (str/replace #"([a-z])([A-Z])" (fn [[_ a A]]
                                       (str a "-" A)))
      (str/lower-case)
      keyword))

(def events
  (into {}
        (map (juxt class->kw identity))
        event-classes))

(def resolve-event-class
  (memoize
   (fn [event]
     (if (class? event)
       event
       (Class/forName (get events event))))))

(def priority (util/enum->map org.bukkit.event.EventPriority))

(defn unregister-all-event-listeners [event]
  (let [^Class event-class (resolve-event-class event)
        getHandlerList (.getMethod event-class "getHandlerList" (into-array Class []))
        ^org.bukkit.event.HandlerList handler-list (.invoke getHandlerList nil nil)]
    (doseq [^org.bukkit.event.Listener handler (.getRegisteredListeners handler-list)]
      (.unregister handler-list handler))))

(defn unlisten! [event key]
  (let [^Class event-class (resolve-event-class event)
        getHandlerList (.getMethod event-class "getHandlerList" (into-array Class []))
        ^org.bukkit.event.HandlerList handler-list (.invoke getHandlerList nil nil)]
    (doseq [handler (.getRegisteredListeners handler-list)
            :let [^org.bukkit.event.Listener listener (:listener (bean handler))]]
      (when (= key (::key (meta listener)))
        (.unregister handler-list listener)))))

(defn listen! [event k f]
  (let [event-class (resolve-event-class event)]
    (unlisten! event-class k)
    (.registerEvent (Bukkit/getPluginManager)
                    event-class
                    (with-meta
                      (reify org.bukkit.event.Listener)
                      {::key k})
                    (priority :normal)
                    (reify org.bukkit.plugin.EventExecutor
                      (execute [this listener event]
                        (try
                          (f (bean event))
                          (catch Throwable t
                            (println "Error in event handler" event k t)))))
                    (proxy [org.bukkit.plugin.PluginBase] []
                      (getDescription []
                        (org.bukkit.plugin.PluginDescriptionFile. (str "Listen for " (name event)) "1.0" (str k)))
                      (isEnabled []
                        true)))))

(comment
  (listen! :async-player-chat
           ::print-chat
           (fn [e]
             (prn "heyyyya" (:message e))))

  (unlisten! :async-player-chat ::print-chat)

  (listen! :block-damage
           ::show-block-dmg
           (fn [e]
             (prn "You broke it!" e)))

  (unlisten! :block-damage ::show-block-dmg)

  (listen! :block-damage
           ::self-heal
           (fn [e]
             (let [block (:block (bean e))
                   type (:type (bean block))]
               (future
                 (Thread/sleep 500)
                 (prn type)
                 (fill (offset (->location block) [-1 -1 -1])
                       [3 3 3]
                       type)))))



  (unlisten! :block-damage ::show-block-dmg)

  (unregister-all-event-listeners :async-player-chat)
  )
