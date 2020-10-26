(ns repl-sessions.experiments
  (:refer-clojure :exclude [bean])
  (:import (net.glowstone GlowServer)
           (org.bukkit Location))
  (:require [clojure.string :as str]
            [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.safe-bean :refer [bean]]
            [lambdaisland.witchcraft.bukkit :as bukkit :refer [entities materials]]))

(wc/start!)
(fly!)
(world)



(let [{:keys [x y z]} (bean (player-location))]
  )

(fill (player-location) [10 3 5] :jack-o-lantern)
materials

(let [radius 100
      {:keys [x y z]} (datafy (location (wc/player)))]
  (doseq [x (range (- x 100) (+ x 100))
          y (range y (+ y 30))
          z (range (- z 100) (+ z 100))]
    (set-block-type (world (wc/player)) {:x x :y y :z z} (:air materials))))

(let [{:keys [x y z]} (datafy (location (wc/player)))]
  (doseq [x (range (- x 100) (+ x 100))
          y (range y (+ y 30))
          z (range (- z 100) (+ z 100))]
    (set-block-type (world (wc/player)) {:x x :y y :z z} (:air materials))))

(defn kubus [{:keys [x y z]}
             {:keys [hoogte breedte lengte]}
             materiaal]
  (doseq [x (range x (+ x breedte))
          y (range y (+ y hoogte))
          z (range z (+ z lengte))]
    (set-block-type (world (wc/player))
                    {:x x :y y :z z}
                    (get materials
                         materiaal))))

(doseq [[m] (shuffle (seq materials))]
  (bukkit/inventory-add (wc/player) m))

(bukkit/empty-inventory (wc/player))

(fly!)

(datafy(location (wc/player)))

(.isFlying (wc/player))

(datafy (location (wc/player)))


(future
  (dotimes [i 1000]
    (update-location (wc/player) update :yaw inc)
    (Thread/sleep 50)))

(fly!)
(update-location (wc/player) update :y + 5)

(datafy (location (wc/player)))
{:x -470, :y 75, :z 131, :yaw 98.16312, :pitch 4.5000563, :world #object[net.glowstone.GlowWorld 0x171c38da "GlowWorld(name=world)"]}
{:x -470, :y 75, :z 131, :yaw 98.16312, :pitch 4.5000563, :world #object[net.glowstone.GlowWorld 0x171c38da "GlowWorld(name=world)"]}
(defn verwijder-huis [{:keys [x y z] :as locatie}
                      {:keys [hoogte breedte lengte]}]
  (kubus locatie
         {:hoogte hoogte
          :breedte breedte
          :lengte lengte}
         :air))

(defn maak-huis [{:keys [x y z] :as locatie}
                 {:keys [hoogte breedte lengte]}]
  (kubus locatie {:hoogte hoogte :breedte breedte :lengte lengte} :acacia-stairs)
  (kubus {:x (inc x)
          :y (inc y)
          :z (inc z)}
         {:hoogte (- hoogte 2) :breedte (- breedte 2) :lengte (- lengte 2)} :air)
  #_(kubus {:x (inc x)
            :y (inc y)
            :z (inc z)}
           {:hoogte 10 :breedte (- breedte 2) :lengte 5} :sand)
  #_(kubus {:x (inc x)
            :y (inc y)
            :z (+ (inc z) 5)}
           {:hoogte 10 :breedte (- breedte 2) :lengte (- lengte 2 5)} :water)

  )

(.spawnEntity (world (wc/player))
              (location (merge
                         (datafy (location (wc/player)))
                         {:x (+ -471 5)
                          :y (+ 63 15)
                          :z (+ 129 13)}))
              (:squid entities))
materials
;; (maak-huis {:x -471, :y 63, :z 129}
;;            {:hoogte 20
;;             :breedte 15
;;             :lengte 20 })

;; (verwijder-huis {:x -471, :y 63, :z 129}
;;                 {:hoogte 20
;;                  :breedte 18
;;                  :lengte 22 })

(maak-huis {:x -766
            :y 63
            :z -59}
           {:hoogte 10
            :breedte 10
            :lengte 10})




(defn rand-loc []
  (let [loc (bean (location))]
    (map->location (-> loc
                       (update :x + (- (rand-int 10) 5))
                       (update :y + 10)
                       (update :z + (- (rand-int 10) 5))))))

(rand-loc)

(defn spawn-random-chicken []
  (.spawnEntity (world) (rand-loc) (:chicken entities)))

(doseq [_ (range 100)]
  (spawn-random-chicken)
  (Thread/sleep 500))

(.spawnEntity (world me) (-> me
                             location
                             datafy
                             (update :x - 5)
                             (update :z - 2)
                             location) #_(rand-loc) org.bukkit.entity.EntityType/CHICKEN)

(= server (org.bukkit.Bukkit/getServer))

(.getGameMode me)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
