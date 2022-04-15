(ns lambdaisland.witchcraft.events
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.util :as util]
            [clojure.java.classpath :as cp]
            [clojure.string :as str])
  (:import (java.util.jar JarFile JarEntry)
           (java.io File)
           (org.bukkit Bukkit)
           (org.bukkit.event Event)
           (org.bukkit.event.block Action)))

(require 'lambdaisland.witchcraft.classpath-hacks)

;; Inlined from lambdaisland.classpath so we don't pull in all of tools.deps

(defn classpath
  "clojure.java.classpath does not play well with the post-Java 9 application
  class loader, which is no longer a URLClassLoader, even though ostensibly it
  tries to cater for this, but in practice if any URLClassLoader or
  DynamicClassLoader higher in the chain contains a non-empty list of URLs, then
  this shadows the system classpath."
  []
  (distinct (concat (cp/classpath) (cp/system-classpath))))

(defn classpath-directories
  "Returns a sequence of File objects for the directories on classpath."
  []
  (filter #(.isDirectory ^File %) (classpath)))

(defn classpath-jarfiles
  "Returns a sequence of JarFile objects for the JAR files on classpath."
  []
  (map #(JarFile. ^File %) (filter cp/jar-file? (classpath))))

(defn find-resources
  "Scan 'the classpath' for resources that match the given regex."
  [regex]
  ;; FIXME currently jar entries always come first in the result, this should be
  ;; in classpath order.
  (concat
   (sequence
    (comp
     (mapcat #(iterator-seq (.entries ^JarFile %)))
     (map #(.getName ^JarEntry %))
     (filter #(re-find regex %)))
    (classpath-jarfiles))

   (sequence
    (comp
     (mapcat file-seq)
     (map str)
     (filter #(re-find regex %)))
    (classpath-directories))))

(defn find-event-classes []
  (find-resources #"(bukkit|paper|spigot|net/citizensnpcs).*event.*Event\.class"))

(def event-classes
  (let [classes (find-event-classes)
        classes (if (empty? classes)
                  (find-event-classes)
                  classes)]
    (map #(-> % (str/replace #"/" ".") (str/replace #".class$" ""))
         classes)))

(defn class->kw [name]
  (let [shortname (-> name
                      (str/replace #".*\." "")
                      (str/replace #"Event$" "")
                      (str/replace #"([a-z])([A-Z])" (fn [[_ a A]]
                                                       (str a "-" A)))
                      (str/lower-case)
                      )]
    (if (str/starts-with? name "net.citizensnpcs")
      (keyword "citizens" shortname)
      (keyword shortname))))

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

(def actions (into {} (map (juxt val key)) (util/enum->map Action)))

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

(defn listen-raw!
  "Like [[listen!]], but skips calling [[bean]], you get the raw Bukkit event instance. "
  [event k f]
  (let [event-class (resolve-event-class event)]
    (unlisten! event-class k)
    (when-let [pm (and (Bukkit/getServer)
                       (Bukkit/getPluginManager))]
      (.registerEvent
       pm
       event-class
       (with-meta
         (reify org.bukkit.event.Listener)
         {::key k})
       (priority :normal)
       (reify org.bukkit.plugin.EventExecutor
         (execute [this listener event]
           (try
             (f event)
             (catch Throwable t
               (println "Error in event handler" event k t)))))
       (proxy [org.bukkit.plugin.PluginBase] []
         (getDescription []
           (org.bukkit.plugin.PluginDescriptionFile. (str "Listen for " (name event)) "1.0" (str k)))
         (isEnabled []
           true))))))

(defn listen!
  "Listen to the given event (keyword, see `(keys
  lambdaisland.witchcraft.events/events)`). `k` is a unique key for this
  handler, subsequent calls with the same key will replace the previous handler.
  `f` is a single argument function, it receives the bukkit event converted to a
  Clojure map with [[bean]]. The original event class can be accessed with
  `:lambdaisland.witchcraft.events/raw`"
  [event k f]
  (listen-raw!
   event k
   (fn [e]
     (let [e (assoc (bean e) ::raw e)]
       (f (cond-> e
            (:action e)
            (update :action actions)))))))

(defn cancel!
  "Cancel an event, use in event handler to stop things from happening"
  [event]
  (.setCancelled ^Event (::raw event event) true))

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

  (resolve-event-class :inventory-click)
  )
