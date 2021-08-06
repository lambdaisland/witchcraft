(ns lambdaisland.witchcraft.glowstone
  "Glowstone specific code, in particular server start/stop logic. Kept separate
  to allow using the Witchcraft API in non-Glowstone Bukkit-compatible servers."
  (:refer-clojure :exclude [bean time])
  (:require [lambdaisland.witchcraft.config :as config]
            [lambdaisland.witchcraft.util :as util])
  (:import (net.glowstone ConsoleManager GlowOfflinePlayer GlowServer GlowWorld)
           (net.glowstone.block.entity.state GlowDispenser)
           (net.glowstone.chunk ChunkManager)
           (net.glowstone.constants GlowEnchantment GlowPotionEffect)
           (net.glowstone.io WorldStorageProvider)
           (net.glowstone.util.config ServerConfig WorldConfig)
           (org.bukkit Bukkit Server)
           (org.bukkit.configuration.serialization ConfigurationSerialization)
           (org.bukkit.enchantments Enchantment)
           (org.bukkit.plugin PluginManager)
           (org.bukkit.potion Potion PotionEffectType)
           (org.bukkit.scheduler BukkitScheduler)))

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
