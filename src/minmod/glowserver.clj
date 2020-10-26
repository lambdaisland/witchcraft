(ns minmod.glowserver
  (:refer-clojure :exclude [bean])
  (:import (net.glowstone GlowServer)
           (org.bukkit Location))
  (:require [minmod.safe-bean :refer [bean bean->]]
            [minmod.bukkit :as bukkit :refer [entities materials]]
            [minmod.cursor :as c]
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
#_
(.shutdown @server)

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

(defn player-location []
  (:location (bean (player))))

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

(defn update-location [entity f & args]
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

(comment
  (-> (player-location)
      (offset [-3 -1 -3])
      (fill [6 1 6] :wood))

  (-> (player-location)
      highest-block-at
      bean
      :location
      (fill [5 5 5] :acacia-stairs))

  (loop  [pos (player-location)
          dir :north-north-east
          off [1 1 0]
          count 0]
    (set-block pos :acacia-stairs )
    (fill (offset pos [0 1 0]) [1 3 1] :air)
    (set-block-direction pos dir)
    (cond
      (< count 6)
      (recur (offset pos off) dir off (inc count))
      (< count 12)
      (recur (offset pos off) :south-west [0 1 1] (inc count))))

  (-> (player-location)
      (offset [-5 2 -5])
      (fill [10 10 10] :jack-o-lantern)))

(def curs (select-keys  (bean-> (player-location) #(assoc % :dir :east)) [:x :y :z :dir]))

(def curs (-> curs
              (c/rotate 2)
              c/step
              (set-block :wood)
              c/step
              c/up
              (set-block :acacia-stairs)
              c/step
              c/up
              (set-block :acacia-stairs)
              c/step
              c/up
              (set-block :acacia-stairs)
              c/step
              c/up
              (set-block :acacia-stairs)
              ))

:grass

(set-block curs :acacia-stairs)
(set-block-direction curs :south)
(fill (offset (player-location) [-40 0 -40]) [80 80 80] :air)
(fill (offset (player-location) [-40 -1 -40]) [80 1 80] :grass)

(keys  bukkit/materials)

;; (chunk-manager)

;; (.setPopulated (player-chunk) false)
;; (bean (player-chunk))

;; (doseq [chunk (.getLoadedChunks (chunk-manager))]

;;   (.populate (net.glowstone.generator.populators.overworld.MegaSpruceTaigaPopulator.)
;;              (world)
;;              (java.util.Random.)
;;              chunk))

;; (:populators (bean (world)))
;; (:x  (bean  (player-chunk)))

(fly!)
(fast-forward 5000)

;; (.populate (net.glowstone.generator.populators.overworld.SavannaPopulator.)
;;            )



;; (bean (chunk-manager))




;; (let [superflat (net.glowstone.chunk.ChunkManager. (world)
;;                                                    (:chunkIoService (bean (storage)))
;;                                                    (net.glowstone.generator.NetherGenerator. #_ SuperflatGenerator.))])
;; (doseq [chunk [(player-chunk)]#_(seq  (.getLoadedChunks (chunk-manager)))
;;         :let [{:keys [x z]} (bean chunk)]]
;;   (let [data (.generateChunkData  (net.glowstone.generator.SuperflatGenerator.)
;;                                   (world)
;;                                   (java.util.Random.)
;;                                   x
;;                                   z
;;                                   (reify org.bukkit.generator.ChunkGenerator$BiomeGrid
;;                                     (getBiome [_ x z]
;;                                       org.bukkit.block.Biome/MESA)
;;                                     (setBiome [_  x z bio])))]
;;     (doseq [x (range 16)
;;             y (range 256)
;;             z (range 16)]
;;       (.setData (.getBlock chunk x y z) (.getData data x y z)))

;;     ))
