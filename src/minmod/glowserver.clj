(ns minmod.glowserver
  (:import (net.glowstone GlowServer)
           (org.bukkit Location))
  (:require [clojure.string :as str]))

(def server (atom nil))

(defn start! []
  (let [gs (GlowServer/createFromArguments (into-array String []))]
    (future
      (try
        (.run gs)
        (finally
          (println ::started))))
    (reset! server gs)))

(defn players []
  (.getOnlinePlayers ^GlowServer @server))

(defn player []
  (first (players)))
