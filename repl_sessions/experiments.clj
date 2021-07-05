(ns repl-sessions.experiments
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.config :as config]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

;; Start the server
(wc/start! {:level-type "FLAT"})

(wc/get-block (wc/map->location {:x 0 :y 0 :z 0}))

;; Best to do this before you finish, so your world doesn't get corrupted
#_(stop!)

;; Now start the minecraft launcher (https://www.minecraft.net/en-us/download),
;; and download version 1.12.2 (the latest that works with Glowkit). Start a
;; multiplayer game, and add a server with address 127.0.0.1:25565. Boom, you're
;; in.

;; put a 3x3 cube of jack-o-lanterns 5 blocks ahead of the player. Works best if
;; you are looking slightly upwards, otherwise it'll be partly underground
(fill (player-location 5) [3 3 3] :jack-o-lantern)

;; Fill the inventory with random stuff
(doseq [[m] (shuffle (seq materials))]
  (bukkit/inventory-add (wc/player) m))

;; Or empty it again
(bukkit/empty-inventory (wc/player))

;; Enables flying. This does not fully work yet, but at least it *allows* the
;; player to fly
(fly!)

(wc/add (wc/->Location {:x 1 :y 2 :z 3}) {:x 4 :y 5 :z 6})

;; Work in progress
(defn fish-bowl [{:keys [x y z] :as locatie}
                 {:keys [hoogte breedte lengte]}]
  (fill locatie [hoogte breedte lengte] :glass)
  (fill {:x (inc x) :y (inc y) :z (inc z)} [(- hoogte 2) (- breedte 2) (- lengte 2)] :air)
  #_(fill {:x (inc x) :y (inc y) :z (inc z)} [ 10 (- breedte 2) 5] :sand)
  (fill {:x (inc x) :y (inc y) :z (+ (inc z) 5)} [ 10 (- breedte 2) (- lengte 2 5)] :water))

(fish-bowl (player-location 20)
           {:hoogte 10
            :breedte 10
            :lengte 10})

;; Chickens!
(spawn (player-location 3) :villager)

(defn rand-loc []
  (let [loc (bean (player-location))]
    (highest-block-at (-> loc
                          (update :x + (- (rand-int 10) 5))
                          (update :z + (- (rand-int 10) 5))))))


(defn spawn-random-chicken []
  (spawn (rand-loc) (:chicken entities)))

;; Lots of chickens!
(doseq [_ (range 10)]
  (spawn-random-chicken)
  (Thread/sleep 500))

(wc/game-mode)
;; => :SURVIVAL

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
    (fill [10 10 10] :jack-o-lantern))

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

(set-blocks [(assoc (player-location 3) :material :grass)
             (assoc (player-location 4) :material :grass)
             (assoc (player-location 5) :material :grass)])
