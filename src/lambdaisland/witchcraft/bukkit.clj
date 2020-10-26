(ns lambdaisland.witchcraft.bukkit
  (:import (net.glowstone GlowServer)
           (org.bukkit Location)
           (org.bukkit.inventory ItemStack))
  (:require [clojure.string :as str]))

(def entities
  (into {}
        (comp (filter #(.getName %))
              (map (juxt #(keyword (str/replace (str/lower-case (.getName %)) "_" "-")) identity)))
        (java.util.EnumSet/allOf org.bukkit.entity.EntityType)))

(def materials
  (into {}
        (comp (filter #(.name %))
              (map (juxt #(keyword (str/replace (str/lower-case (.name %)) "_" "-")) identity)))
        (java.util.EnumSet/allOf org.bukkit.Material)))

(def block-faces
  (into {}
        (comp (filter #(.name %))
              (map (juxt #(keyword (str/replace (str/lower-case (.name %)) "_" "-")) identity)))
        (java.util.EnumSet/allOf org.bukkit.block.BlockFace)))

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
