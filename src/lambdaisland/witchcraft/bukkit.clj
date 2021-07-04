(ns lambdaisland.witchcraft.bukkit
  "Bukkit wrapper and"
  (:require [lambdaisland.witchcraft.util :as util])
  (:import (org.bukkit.inventory ItemStack)))

(def entities (util/enum->map org.bukkit.entity.EntityType))
(def materials (util/enum->map org.bukkit.Material))
(def block-faces (util/enum->map org.bukkit.block.BlockFace))
(def tree-species (util/enum->map org.bukkit.TreeSpecies))

(defn inventory [player]
  (.getInventory player))

(defn item-stack [material count]
  (ItemStack. (get materials material) count))

(defn inventory-add [player item]
  (.addItem (inventory player)
            (into-array ItemStack [(item-stack item 1)])))

(defn empty-inventory [player]
  (let [i (inventory player)
        c (.getContents i)]
    (.removeItem i c)))
