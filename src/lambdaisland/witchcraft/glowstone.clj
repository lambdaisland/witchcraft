(ns lambdaisland.witchcraft.glowstone
  "Glowstone specific code, in particular server start/stop logic. Kept separate
  to allow using the Witchcraft API in non-Glowstone Bukkit-compatible servers."
  (:refer-clojure :exclude [bean time])
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.glowstone.config :as config]
            [lambdaisland.witchcraft.util :as util])
  (:import (net.glowstone ConsoleManager GlowOfflinePlayer GlowServer GlowWorld)
           (net.glowstone.block.entity.state GlowDispenser)
           (net.glowstone.chunk ChunkManager)
           (net.glowstone.constants GlowEnchantment GlowPotionEffect)
           (net.glowstone.io WorldStorageProvider)
           (net.glowstone.util.config ServerConfig WorldConfig)
           (com.cryptomorin.xseries XMaterial XBlock)
           (org.bukkit Material Bukkit Server)
           (org.bukkit.block Block)
           (org.bukkit.configuration.serialization ConfigurationSerialization)
           (org.bukkit.enchantments Enchantment)
           (org.bukkit.plugin PluginManager)
           (org.bukkit.potion Potion PotionEffectType)
           (org.bukkit.scheduler BukkitScheduler)
           (org.bukkit.material MaterialData Directional)))

(set! *warn-on-reflection* true)

;; =============================================================================

(defn init-registrations!
  "Perform some internal wiring for Glowstone. This populates several
  global (static) registries"
  []
  (ConfigurationSerialization/registerClass GlowOfflinePlayer)
  (GlowPotionEffect/register)
  (GlowEnchantment/register)
  (GlowDispenser/register))

(defn server
  "Get the currently active server."
  ^Server []
  (Bukkit/getServer))

(defn plugin-manager
  "Get the Bukkit plugin manager"
  ^PluginManager
  []
  (Bukkit/getPluginManager))

(defn chunk-manager
  "Get the world's chunk manager"
  (^ChunkManager []
   (chunk-manager (first (.getWorlds (server)))))
  (^ChunkManager [^GlowWorld world]
   (.getChunkManager world)))

(defn storage
  "Get the world's storage"
  ^WorldStorageProvider
  ([]
   (storage (first (.getWorlds (server)))))
  ([world]
   (.getStorage ^GlowWorld world)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init/exit

(defn create-server
  "Create a new GlowServer based on a config

  This also sets the static worldConfig field via reflection. When creating a
  server directly like this (instead of via `createFromArguments`) this
  otherwise doesn't get set."
  ^GlowServer
  [^ServerConfig config]
  (util/set-static! GlowServer "worldConfig" (WorldConfig. (.getFile config "") (.getFile config "worlds.yml")))
  (let [server (GlowServer. config)
        console-manager (.get (util/accessible-field GlowServer "consoleManager") server)]
    (.set (util/accessible-field ConsoleManager "running") console-manager false)
    server))

(defn start!
  "Start a server, optionally provided with a map of config options. See [[lambdaisland.witchcraft.config/key-values]]"
  ([]
   (start! nil))
  ([opts]
   (if (server)
     (println "Server already started...")
     (future
       (try
         (init-registrations!)
         (.run (create-server (if (:config-dir opts)
                                (config/server-config opts)
                                (config/data-config opts))))
         (finally
           (wc/init-xmaterial!)
           (println ::started)))))))

(defn reset-globals!
  "Glowstone and Bukkit keep tons of globals, do our best to unset/reset them so
  it's possible to restart a server without restarting the process."
  []
  (util/set-static! Potion "brewer" nil)
  (util/set-static! Bukkit "server" nil)
  (util/set-static! GlowServer "worldConfig" nil)
  (util/set-static! PotionEffectType "acceptingNew" true)
  (util/set-static! Enchantment "acceptingNew" true)
  (let [byId (.get (util/accessible-field PotionEffectType "byId") PotionEffectType)
        byName (.get (util/accessible-field PotionEffectType "byName") PotionEffectType)]
    (dotimes [i (count byId)]
      (aset ^"[Lorg.bukkit.potion.PotionEffectType;" byId i nil))
    (.clear ^java.util.Map byName))
  (let [byId (.get (util/accessible-field Enchantment "byId") Enchantment)
        byName (.get (util/accessible-field Enchantment "byName") Enchantment)]
    (.clear ^java.util.Map byId)
    (.clear ^java.util.Map byName)))

(defn stop! []
  (.shutdown ^GlowServer (server))
  (reset-globals!))

(def terracotta-materials
  "These have their direction as the lower two bits of their data value"
  #{:gray-glazed-terracotta :yellow-glazed-terracotta :blue-glazed-terracotta :light-blue-glazed-terracotta :lime-glazed-terracotta :magenta-glazed-terracotta :cyan-glazed-terracotta :green-glazed-terracotta :black-glazed-terracotta :purple-glazed-terracotta :white-glazed-terracotta :silver-glazed-terracotta :brown-glazed-terracotta :pink-glazed-terracotta :orange-glazed-terracotta :red-glazed-terracotta})

(def stair-materials
  "These also have their direction in the lower two bits, but the values differ"
  #{:birch-wood-stairs :nether-brick-stairs :acacia-stairs :jungle-wood-stairs :cobblestone-stairs :smooth-stairs :purpur-stairs :sandstone-stairs :spruce-wood-stairs :quartz-stairs :brick-stairs :dark-oak-stairs :red-sandstone-stairs :wood-stairs})

(def door-materials
  "Regular doors"
  #{:wood-door
    :jungle-door
    :iron-door
    :dark-oak-door
    :spruce-door
    :birch-door
    :wooden-door
    :acacia-door})

(defn direction-data [data ^XMaterial xm direction]
  (cond
    (.isOneOf xm ["CONTAINS:TERRACOTTA"])
    (bit-or
     (bit-and data 2r11111100)
     (case direction
       :down       0
       :north      0
       :north-east 0
       :east       1
       :south-east 1
       :south      2
       :south-west 2
       :west       3
       :north-west 3
       :up         3
       0))

    (.isOneOf xm ["CONTAINS:STAIRS"])
    (bit-or
     (bit-and data 2r11111100)
     (case direction
       :down 4
       :north 0
       :north-east 0
       :east 2
       :south-east 2
       :south 1
       :south-west 1
       :west 3
       :north-west 3
       :up 0
       0))

    ;; Based on the wiki, but getting errors when actually trying to use banners
    (.isOneOf xm ["CONTAINS:BANNER"])
    (bit-or
     (bit-and data 2r11110001)
     (case direction
       :south 0
       :south-west 2
       :west 4
       :north-west 6
       :north 8
       :north-east 10
       :east 12
       :south-east 14
       0))

    (.isOneOf xm ["CONTAINS:DOOR"])
    (bit-or
     (bit-and data 2r11111100)
     (case direction
       :north 0
       :north-east 0
       :east 1
       :south-east 1
       :south 2
       :south-west 2
       :west 3
       :north-west 3
       0))

    (.isOneOf xm ["CONTAINS:LOG" "CONTAINS:WOOD"])
    (bit-or
     (bit-and data 2r11110011)
     (case direction
       :north 8
       :north-east 8
       :east 4
       :south-east 4
       :south 8
       :south-west 8
       :west 4
       :north-west 4
       0))
    :else
    data))

(defmethod wc/-set-direction :glowstone [_ ^Block block face]
  (prn [block face])
  (let [xm        (wc/xmaterial block)
        state     (.getState block)
        mdata     (.getData state)
        data      (.getData mdata)
        direction (get wc/block-face-names face)
        new-data (direction-data data xm direction)]
    (when (not= data new-data)
      (.setData block new-data))))

(defmethod wc/-set-blocks :glowstone [_ blocks]
  (let [^net.glowstone.util.BlockStateDelegate delegate (net.glowstone.util.BlockStateDelegate.)]
    (doseq [{:keys [world x y z material data direction]
             :or {world (wc/world (wc/server))
                  data 0}
             :as block} blocks
            :let [^Material material (wc/material (:material block))
                  _ (assert material)
                  ^MaterialData mdata (if (number? data)
                                        (MaterialData. material (byte (direction-data data (wc/xmaterial material) direction)))
                                        data)]]
      ;; (when (and direction (instance? Directional mdata))
      ;;   (.setFacingDirection ^Directional mdata (wc/block-faces direction)))
      (.setTypeAndData delegate world x y z material mdata))
    (.updateBlockStates delegate)))
