(ns lambdaisland.witchcraft.paper
  "Start an embedded Paper server, and paper-specific extensions.

  To use this you need to download Paper yourself, and start Clojure/JVM with the right args.

  deps.edn:
  ```
  :aliases {
  :cider/nrepl
  {:extra-deps
   {nrepl/nrepl                   {:mvn/version \"0.8.3\"}
    refactor-nrepl/refactor-nrepl {:mvn/version \"2.5.1\"}
    cider/cider-nrepl             {:mvn/version \"0.26.0\"}}}

  :papermc
  {:extra-deps {io.papermc/paper {:local/root \"/home/arne/Downloads/paper-1.17.1-157.jar\"}}
   :main-opts [\"-m\" \"paper-witch\"]}}
  }
  ```

  Command line:
  ```
  clj -J-Dcom.mojang.eula.agree=true -J-javaagent:/home/arne/Downloads/paper-1.17.1-157.jar -A:cider/nrepl -M:papermc
  ```
  "
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.util :as util])
  (:import (org.bukkit Bukkit Server)
           (org.bukkit.block Block BlockFace)))

(set! *warn-on-reflection* true)

(defn server
  "Get the currently active server."
  ^Server []
  (Bukkit/getServer))

(defn start! [gui?]
  (if (server)
    (println "Server already started...")
    (do
      (future
        (loop []
          (Thread/sleep 1000)
          (if (try (server) (catch Throwable t))
            (wc/init-xmaterial!)
            (recur))))
      (future
        (try
          (util/if-class-exists
           io.papermc.paperclip.Paperclip
           (io.papermc.paperclip.Paperclip/main (into-array String (if gui? [] ["nogui"])))
           (println "Class not found: io.papermc.paperclip.Paperclip" ))
          (finally
            (println ::started)))))))

;; The XSeries implmenentation doesn't call `setBlockData`, which is necessary
;; on Paper, so we do it ourselves.
(util/when-class-exists org.bukkit.block.data.Directional
  (defmethod wc/-set-direction :paper [_ ^Block block ^BlockFace face]
    (let [data (.getBlockData block)]
      (when (instance? org.bukkit.block.data.Directional data)
        (.setFacing ^org.bukkit.block.data.Directional data face)
        (.setBlockData block data)))))
