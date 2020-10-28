(ns lambdaisland.witchcraft.worlds
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.bukkit :as bukkit :refer [entities materials]]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

(defn create-world [name gen-fn spawn-loc]
  (.createWorld
   @server
   (.generator (org.bukkit.WorldCreator. name)
               (proxy [net.glowstone.generator.GlowChunkGenerator] [(into-array org.bukkit.generator.BlockPopulator [])]
                 (generateChunkData [world random chunk-x chunk-z biomes]
                   (let [chunk (net.glowstone.generator.GlowChunkData. world)]
                     (doseq [x (range 16)
                             y (range 128)
                             z (range 16)
                             :let [real-x (+ (* chunk-x 16) x)
                                   real-z (+ (* chunk-z 16) z)
                                   material (gen-fn real-x y real-z #_(.getBiome biomes real-x real-z))]
                             :when material]
                       (.setBlock chunk x y z (if (keyword? material)
                                                (get materials material)
                                                material)))
                     chunk)))))
  (.setSpawnLocation (world name) (->location (assoc spawn-loc :world (world name)))))

(comment
  (create-world "stone-flats"
                (fn [x y z]
                  (when (or (< y 5))
                    :stone))
                {:x 0 :y 6 :z 0})

  (teleport {:world (world "stone-flats")
             :x 100
             :z 100}))
