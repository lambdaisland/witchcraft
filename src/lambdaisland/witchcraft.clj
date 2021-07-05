(ns lambdaisland.witchcraft
  "Clojure API for Glowstone"
  (:refer-clojure :exclude [bean])
  (:require [clojure.java.io :as io]
            [lambdaisland.witchcraft.util :as util]
            [lambdaisland.witchcraft.config :as config]
            [lambdaisland.witchcraft.events :as events]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]])
  (:import (net.glowstone GlowWorld GlowServer GlowOfflinePlayer)
           (org.bukkit Bukkit Material Location World)
           (org.bukkit.material MaterialData Directional)
           (org.bukkit.entity Entity Player HumanEntity)
           (org.bukkit.enchantments Enchantment)
           (org.bukkit.block Block)
           (org.bukkit.potion Potion PotionEffectType)
           (org.bukkit.inventory ItemStack Inventory)
           (net.glowstone.util.config ServerConfig)
           org.bukkit.configuration.serialization.ConfigurationSerialization
           net.glowstone.constants.GlowEnchantment
           net.glowstone.block.entity.state.GlowDispenser
           net.glowstone.constants.GlowPotionEffect
           net.glowstone.util.config.WorldConfig))

(set! *warn-on-reflection* true)

(defn listen!
  "Listen for an event

  Event is a keyword based on the event type, like `:player-interact` or
  `:item-spawn`, `k`` is a key this event is registered under, so you can
  `unlisten!` with the same key afterwards. Re-registering a listener with the
  same event and key will replace the old listener."
  [event k f]
  (events/listen! event k f))

(defn unlisten!
  "Remove an event listener"
  [event k]
  (events/unlisten! event k))

(def entities (util/enum->map org.bukkit.entity.EntityType))
(def materials (util/enum->map org.bukkit.Material))
(def material-names (into {} (map (juxt val key)) materials))
(def block-faces (util/enum->map org.bukkit.block.BlockFace))
(def tree-species (util/enum->map org.bukkit.TreeSpecies))
(def block-actions (util/enum->map org.bukkit.event.block.Action))

(defn init-registrations!
  "Perform some internal wiring for Glowstone"
  []
  (ConfigurationSerialization/registerClass GlowOfflinePlayer)
  (GlowPotionEffect/register)
  (GlowEnchantment/register)
  (GlowDispenser/register))

(defn server
  "Get the currently active server."
  ^GlowServer []
  (Bukkit/getServer))

(defn create-server
  "Create a new GlowServer based on a config

  This also sets the static worldConfig field via reflection. When creating a
  server directly like this (instead of via `createFromArguments`) this
  otherwise doesn't get set."
  ^GlowServer
  [^ServerConfig config]
  (util/set-static! GlowServer "worldConfig" (WorldConfig. (.getFile config "") (.getFile config "worlds.yml")))
  (GlowServer. config))

(defn start!
  "Start a server, optionally provided with a map of config options. See [[lambdaisland.witchcraft.config/key-values]]"
  ([]
   (start! nil))
  ([opts]
   (future
     (try
       (init-registrations!)
       (.run (create-server (config/data-config opts)))
       (finally
         (println ::started))))))

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

(defn players
  "List all online players"
  []
  (.getOnlinePlayers ^GlowServer (server)))

(defn player
  "Get a player by name, or simply the first player found."
  (^Player []
   (first (players)))
  (^Player [name]
   (some #(when (= name (:name (bean %)))
            %)
         (players))))

(defn in-front-of [^Location loc n]
  (let [dir (bean (.getDirection loc))]
    (-> (bean loc)
        (update :x + (* (:x dir) n))
        (update :y + (* (:y dir) n))
        (update :z + (* (:z dir) n)))))

(defprotocol CommonProperties
  (location [_] [_ n]))

(extend-protocol CommonProperties
  Entity
  (location
    ([entity]
     (.getLocation entity))
    ([entity n]
     (in-front-of (.getLocation entity) n)))
  Block
  (location
    ([entity]
     (.getLocation entity))
    ([entity n]
     (in-front-of (.getLocation entity) n))))

(defn worlds []
  (:worlds (bean (server))))

(defn world
  (^GlowWorld []
   (or (:world (bean (player)))
       (first (worlds))))
  (^GlowWorld [name]
   (some #(when (= name (:name (bean %)))
            %)
         (worlds))))

(defn fast-forward
  "Fast forward the clock, time is given in ticks, with 20 ticks per second, or
  24000 ticks in a Minecraft day."
  [time]
  (.setTime (world) (+ (.getTime (world)) time)))

(defn fly!
  ([]
   (fly! (player)))
  ([player]
   (.setAllowFlight player true)
   (.setFlying player true)))

(defn map->location
  "Convert a map to a Location instance"
  ^Location [{:keys [x y z yaw pitch world]
              :or {x 0 y 0 z 0 yaw 0 pitch 0 world (world)}}]
  (Location. world x y z yaw pitch))

(defn ->location
  "Coerce to Location instance"
  ^Location [loc]
  (or (and (instance? Location loc) loc)
      (:location (bean loc))
      (map->location (bean loc))))

(defn update-location! [entity f & args]
  (.teleport ^Entity entity
             (map->location (apply f  (bean (:location (bean (player)))) args))
             org.bukkit.event.player.PlayerTeleportEvent$TeleportCause/PLUGIN))

(defn player-chunk []
  (let [{:keys [world location]} (bean (player))]
    (.getChunkAt ^World world ^Location location)))

(defn chunk-manager []
  (:chunkManager (bean (world))))

(defn storage []
  (:storage (bean (world))))

(defn get-block ^Block [loc]
  (let  [{:keys [x y z world] :or {world (world)}} (bean loc)]
    (.getBlockAt ^World world (int x) (int y) (int z))))

(defn set-block-direction
  ;; ([cursor]
  ;;  ;; Try to get stairs to line up
  ;;  (set-block-direction cursor (c/rotate-dir (:dir cursor) 2))
  ;;  cursor)
  ([loc dir]
   (let [block (get-block loc)
         ^MaterialData data (bean-> block :state :data)]
     (try
       (.setFacingDirection ^Directional data (if (keyword? dir) (block-faces dir) dir))
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

(defn set-blocks
  "Optimized way to set multiple blocks, takes a sequence of maps
  with :x, :y, :z, :material, and optionally :world and :data."
  [blocks]
  (let [^net.glowstone.util.BlockStateDelegate delegate (net.glowstone.util.BlockStateDelegate.)]
    (doseq [{:keys [world x y z material data]
             :or {world (world)}} blocks
            :let [^Material material (if (keyword? material) (get materials material) material)
                  ^MaterialData data (if (number? data) (MaterialData. material (byte data)) data)]]
      (if data
        (.setTypeAndData delegate world x y z material data)
        (.setType delegate world x y z material)))
    (.updateBlockStates delegate)))

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
    (.getHighestBlockAt ^World world (map->location {:x x :y 0 :z z :world world}))))

(defn spawn [loc entity]
  (let [{:keys [x y z ^World world] :or {world (world)}} (bean loc)]
    (.spawnEntity world
                  (map->location {:x x :y y :z z :world world})
                  ^Entity (if (keyword? entity)
                            (get entities entity)
                            entity))))

(defn game-mode
  ([]
   (game-mode (player)))
  ([^Player player]
   (keyword (str (.getGameMode player)))))

(defn plugin-manager []
  (Bukkit/getPluginManager))

(defn teleport
  ([loc]
   (teleport (player) loc))
  ([^Entity entity loc]
   (let [{:keys [^World world] :or {world (world)} :as loc} (bean loc)]
     (.teleport entity (->location (merge (bean (.getSpawnLocation world)) loc))))))

(defn clear-weather []
  (doto (world)
    (.setThundering false)
    (.setStorm false)))

(defn inventory ^Inventory [player]
  (.getInventory ^Player player))

(defn item-stack [material count]
  (ItemStack. ^Material (get materials material) (int count)))

(defn inventory-add
  ([player item]
   (inventory-add player item 1))
  ([player item n]
   (.addItem (inventory player)
             (into-array ItemStack [(item-stack item n)]))))

(defn inventory-remove
  ([player item]
   (inventory-remove player item 1))
  ([player item n]
   (.removeItem (inventory player)
                (into-array ItemStack [(item-stack item n)]))))

(defn empty-inventory [player]
  (let [i (inventory player)
        c (.getContents i)]
    (.removeItem i c)))

(defn item-in-hand ^ItemStack [^HumanEntity entity]
  (.getItemInHand entity))

(defn item-in-hand-type [entity]
  (get material-names (.getType (item-in-hand entity))))

(defn item-in-hand-count [entity]
  (.getAmount (item-in-hand entity)))

(defn nearby-entities
  ([^Entity entity x y z]
   (.getNearbyEntities entity x y z)))
