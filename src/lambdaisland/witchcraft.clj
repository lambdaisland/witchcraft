(ns lambdaisland.witchcraft
  (:refer-clojure :exclude [bean])
  (:import (net.glowstone GlowServer)
           (org.bukkit Location))
  (:require [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.bukkit :as bukkit :refer [entities materials]]
            [lambdaisland.witchcraft.cursor :as c]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defonce server (atom nil))
(defonce default-player (atom nil))

(defn start! []
  (let [gs (GlowServer/createFromArguments (into-array String []))]
    (future
      (try
        (.run gs)
        (finally
          (println ::started))))
    (reset! server gs)))

(defn stop! []
  (.shutdown @server)
  (reset! server nil))

(defn players []
  (.getOnlinePlayers ^GlowServer @server))

(defn player
  ([]
   (if @default-player
     (player @default-player)
     (first (players))))
  ([name]
   (some #(when (= name (:name (bean %)))
            %)
         (players))))

(defn player-location
  "The location of the default player (see [[default-player]]). With a numeric
  argument, returns a location that is that many blocks in front of the player,
  so it is directly in view."
  ([]
   (:location (bean (player))))
  ([n]
   (let [loc (bean (player-location))
         dir (bean-> (player-location) :direction)]
     (-> loc
         (update :x + (* (:x dir) n))
         (update :y + (* (:y dir) n))
         (update :z + (* (:z dir) n))))))

(defn worlds []
  (:worlds (bean @server)))

(defn world []
  (or (:world (bean (player)))
      (first (worlds))))

(defn fast-forward [time]
  (.setTime (world) (+ (.getTime (world)) time)))

(defn fly! []
  (.setAllowFlight (player) true)
  (.setFlying (player) true))

(defn map->location [{:keys [x y z yaw pitch world]
                      :or {x 0 y 0 z 0 yaw 0 pitch 0 world (world)}}]
  (Location. world x y z yaw pitch))

(defn update-location! [entity f & args]
  (.teleport entity
             (map->location (apply f  (bean (:location (bean (player)))) args))
             org.bukkit.event.player.PlayerTeleportEvent$TeleportCause/PLUGIN))

(defn player-chunk []
  (let [{:keys [world location]} (bean (player))]
    (.getChunkAt world location)))

(defn chunk-manager []
  (:chunkManager (bean (world))))

(defn storage []
  (:storage (bean (world))))

(defn get-block [loc]
  (let  [{:keys [x y z world] :or {world (world)}} (bean loc)]
    (.getBlockAt world (int x) (int y) (int z))))

(defn set-block-direction
  ([cursor]
   ;; Try to get stairs to line up
   (set-block-direction cursor (c/rotate-dir (:dir cursor) 2))
   cursor)
  ([loc dir]
   (let [block (get-block loc)
         data (bean-> block :state :data)]
     (try
       (.setFacingDirection data (if (keyword? dir) (bukkit/block-faces dir) dir))
       (.setData block (.getData data))
       ;; Not all blocks have a direction
       (catch Exception e)))
   loc))

(defn set-block [loc material]
  (.setType (get-block loc)
            (if (keyword? material)
              (get materials material)
              material))
  (when (and (map? loc) (:dir loc))
    (set-block-direction loc))
  loc)

(defn fill [loc [w h d] type]
  (let [{:keys [x y z]} (bean loc)
        range (fn [x y] (if (< x y) (range x y) (range y x)))]
    (doseq [x (range x (+ x w))
            y (range y (+ y h))
            z (range z (+ z d))]
      (set-block {:x x :y y :z z} type)))
  loc)

(defn offset [loc [x' y' z']]
  (-> (bean loc)
      (update :x + x')
      (update :y + y')
      (update :z + z')))

(defn highest-block-at [loc]
  (let [{:keys [x y z world] :or {world (world)}} (bean loc)]
    (.getHighestBlockAt world (map->location {:x x :y 0 :z z :world world}))))

(defn spawn [loc entity]
  (let [{:keys [x y z world] :or {world (world)}} (bean loc)]
    (.spawnEntity world
                  (map->location {:x x :y y :z z :world world})
                  (if (keyword? entity)
                    (get bukkit/entities entity)
                    entity))))

(defn game-mode
  ([]
   (game-mode (player)))
  ([player]
   (keyword  (str (.getGameMode player)))))

;; Best to do this, so your world doesn't get corrupted
(stop!)
