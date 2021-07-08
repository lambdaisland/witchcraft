(ns lambdaisland.witchcraft
  "Clojure API for Glowstone"
  (:refer-clojure :exclude [bean])
  (:require [clojure.java.io :as io]
            [lambdaisland.witchcraft.config :as config]
            [lambdaisland.witchcraft.events :as events]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.util :as util])
  (:import (net.glowstone GlowWorld GlowServer GlowOfflinePlayer)
           (net.glowstone.block.entity.state GlowDispenser)
           (net.glowstone.chunk ChunkManager)
           (net.glowstone.constants GlowEnchantment GlowPotionEffect)
           (net.glowstone.io WorldStorageProvider)
           (net.glowstone.util.config WorldConfig ServerConfig)
           (org.bukkit Bukkit Material Location World)
           (org.bukkit.block Block)
           (org.bukkit.configuration.serialization ConfigurationSerialization)
           (org.bukkit.enchantments Enchantment)
           (org.bukkit.entity Entity Player HumanEntity)
           (org.bukkit.inventory ItemStack Inventory)
           (org.bukkit.material MaterialData Directional)
           (org.bukkit.plugin PluginManager Plugin)
           (org.bukkit.potion Potion PotionEffectType)
           (org.bukkit.scheduler BukkitScheduler)
           (org.bukkit.util Vector)))

(set! *warn-on-reflection* true)

(declare in-front-of map->Location)

(def ^Plugin plugin
  "For the rare API calls that really want a plugin instance"
  (proxy [org.bukkit.plugin.PluginBase] []
    (getDescription []
      (org.bukkit.plugin.PluginDescriptionFile. "Witchcraft" "0.0" ""))
    (isEnabled []
      true)))

;; ================================================================================

(def entities
  "Map from keyword to EntityType value"
  (util/enum->map org.bukkit.entity.EntityType))

(def materials
  "Map from keyword to Material value"
  (util/enum->map org.bukkit.Material))

(def material-names
  "Map from Material to keyword"
  (into {} (map (juxt val key)) materials))

(def block-faces
  "Map from keyword to BlockFace value"
  (util/enum->map org.bukkit.block.BlockFace))

(def tree-species
  "Map from keyword to TreeSpecies value"
  (util/enum->map org.bukkit.TreeSpecies))

(def block-actions
  "Map from keyword to block.Action value"
  (util/enum->map org.bukkit.event.block.Action))

;; =============================================================================

;; Functions for retrieving things from objects, like the location, x/y/z
;; values, world, etc. We also implement these for maps, where they do keyword
;; lookups. By using these in other functions we can accept a large range of
;; inputs, including both Glowstone/Bukkit objects and Clojure maps.
(defprotocol PolymorphicFunctions
  (^org.bukkit.Location location
   [_] [_ n]
   "Get the location of the given object, or `n` blocks in front of it.")
  (^net.glowstone.GlowWorld world [_]
   "Get the world for a player, by its name, by UUID, etc.")
  (add [this that]
    "Add locations, vectors, etc. That can also be a map of `:x`, `:y`, `:z`")
  (distance [this that]
    "Get the distance between locations/vectors/entities")
  (^double x [_])
  (^double y [_])
  (^double z [_])
  (yaw [_])
  (pitch [_])
  (^org.bukkit.util.Vector direction [_])
  (^org.bukkit.Material material [_])
  (^Vector as-vec [_] "Coerce to Vector")
  (material-name [_]))

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
  ^GlowServer []
  (Bukkit/getServer))

(defn plugin-manager
  "Get the Bukkit plugin manager"
  ^PluginManager
  []
  (Bukkit/getPluginManager))

(defn chunk-manager
  "Get the world's chunk manager"
  (^ChunkManager []
   (chunk-manager (world (server))))
  (^ChunkManager [^GlowWorld world]
   (.getChunkManager world)))

(defn storage
  "Get the world's storage"
  ^WorldStorageProvider []
  ([]
   (storage (world (server))))
  ([world]
   (.getStorage ^GlowWorld world)))

(defn scheduler
  "The Bukkit scheduler"
  ^BukkitScheduler
  []
  (Bukkit/getScheduler))

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
  (GlowServer. config))

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

(defn worlds
  "Get all worlds on the server"
  ([]
   (worlds (server)))
  ([^GlowServer s]
   (.getWorlds (server))))

(defn in-front-of
  "Get the location `n` blocks in front of the given location, based on the
  location's direction. Polymorphic, can work with most things that have a
  location."
  ([loc]
   (in-front-of loc 1))
  ([loc n]
   (let [dir (direction loc)]
     (add (location loc) {:x (Math/ceil (* n (x dir)))
                          :y (Math/ceil (* n (y dir)))
                          :z (Math/ceil (* n (z dir)))}))))

(defn fast-forward
  "Fast forward the clock, time is given in ticks, with 20 ticks per second, or
  24000 ticks in a Minecraft day."
  [time]
  (.setTime (world (server)) (+ (.getTime (world (server))) time)))

(defn fly!
  "Set a player as allowing flight and flying.
  Note: doesn't seem to actually cause flying, but it does make flying
  possible."
  ([]
   (fly! (player)))
  ([^Player player]
   (.setAllowFlight player true)
   (.setFlying player true)))

(defn map->Location
  "Convert a map/bean to a Location instance"
  ^Location [{:keys [x y z yaw pitch wrld]
              :or {x 0 y 0 z 0 yaw 0 pitch 0 wrld (server)}}]
  (Location. (world wrld) x y z yaw pitch))

(defn player-chunk
  "Return the chunk the player is in"
  [^Player player]
  (let [{:keys [world location]} (bean (player))]
    (.getChunkAt ^World (.getWorld player) ^Location (location player))))

(defn get-block
  "Get the block at a given location"
  ^Block [loc]
  (.getBlockAt ^World (world loc) (x loc) (y loc) (z loc)))

(defn set-block-direction
  "Set the direction of a block, takes a keyword or BlockFace,
  see [[block-faces]]"
  ([loc dir]
   (let [block (get-block loc)
         ^MaterialData data (.getData (.getState block))]
     (try
       (.setFacingDirection ^Directional data (if (keyword? dir) (block-faces dir) dir))
       (.setData block (.getData data))
       ;; Not all blocks have a direction
       (catch Exception e)))
   loc))

(defn set-block
  "Set the block at a specific location to a specific material"
  ([loc material]
   (set-block loc material nil))
  ([loc material data]
   (let [material (if (keyword? material)
                    (get materials material)
                    material)
         block (get-block (location loc))]
     (.setType block material)
     (when data
       (.setData block data)
       (if (number? data)
         (MaterialData. ^Material material (byte data))
         data)))
   (when (and (map? loc) (:dir loc))
     (set-block-direction loc))
   loc))

(defn set-blocks
  "Optimized way to set multiple blocks, takes a sequence of maps
  with :x, :y, :z, :material, and optionally :world and :data."
  [blocks]
  (let [^net.glowstone.util.BlockStateDelegate delegate (net.glowstone.util.BlockStateDelegate.)]
    (doseq [{:keys [world x y z material data]
             :or {world (world (server))}} blocks
            :let [^Material material (if (keyword? material) (get materials material) material)
                  _ (assert material)
                  ^MaterialData data (if (number? data) (MaterialData. material (byte data)) data)]]
      (if data
        (.setTypeAndData delegate world x y z material data)
        (.setType delegate world x y z material)))
    (.updateBlockStates delegate)))

(defn box
  "Draw a box with width, height, depth of a certain block type"
  [loc [w h d] type]
  (let [{:keys [x y z]} (bean loc)
        range (fn [x y] (if (< x y) (range x y) (range y x)))]
    (doseq [x (range x (+ x w))
            y (range y (+ y h))
            z (range z (+ z d))]
      (set-block {:x x :y y :z z} type)))
  loc)

(defn highest-block-at
  "Retrieve the highest block at the given location"
  [loc]
  (let [loc (location loc)]
    (.getHighestBlockAt ^World (world loc) loc)))

(defn spawn
  "Spawn a new entity"
  [loc entity]
  (let [loc (location loc)]
    (.spawnEntity (world loc)
                  loc
                  ^Entity (if (keyword? entity)
                            (get entities entity)
                            entity))))

(defn game-mode
  "Get the current game mode"
  ([]
   (game-mode (player)))
  ([^Player player]
   (keyword (str (.getGameMode player)))))

(defn teleport
  "Teleport to a given location"
  ([loc]
   (teleport (player) loc))
  ([^Entity entity loc]
   (.teleport entity
              (location
               (if (map? loc)
                 (merge (bean (.getSpawnLocation (world loc)))
                        loc)
                 loc)))))

(defn clear-weather
  "Get clear weather"
  []
  (doto (world (server))
    (.setThundering false)
    (.setStorm false)))

(defn inventory
  "Get the player's inventory"
  ^Inventory [player]
  (.getInventory ^Player player))

(defn item-stack
  "Create an ItemStack object"
  ^ItemStack [material count]
  (ItemStack. ^Material (get materials material) (int count)))

(defn add-inventory
  "Add the named item to the player's inventory, or n copies of it"
  ([player item]
   (add-inventory player item 1))
  ([player item n]
   (.addItem (inventory player)
             (into-array ItemStack [(item-stack item n)]))))

(defn remove-inventory
  "Remove the named items from the player's inventory, or n copies of it"
  ([player item]
   (remove-inventory player item 1))
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
  (material-name (.getType (item-in-hand entity))))

(defn item-in-hand-count [entity]
  (.getAmount (item-in-hand entity)))

(defn nearby-entities
  ([^Entity entity x y z]
   (.getNearbyEntities entity x y z)))

(defn listen!
  "Listen for an event

  Event is a keyword based on the event type, like `:player-interact` or
  `:item-spawn`, `k`` is a key this event is registered under, so you can
  `unlisten!` with the same key afterwards. Re-registering a listener with the
  same event and key will replace the old listener.

  See `(keys `[[lambdaisland.witchcraft.events/events]]`)` for all known
  events."
  [event k f]
  (events/listen! event k f))

(defn unlisten!
  "Remove an event listener"
  [event k]
  (events/unlisten! event k))

(defn send-message
  "Send a message to a player"
  [^Player player ^String msg]
  (.sendMessage player msg))

(defn run-task
  "Schedule a task for the next server tick"
  [^Runnable f]
  (.runTask (scheduler) plugin f))

(defn run-task-later
  "Schedule a task for after n ticks"
  [^Runnable f ^long ticks]
  (.runTaskLater (scheduler) plugin f ticks))

(defn vec3
  "Create a vector instance"
  ^Vector [^double x ^double y ^double z]
  (Vector. x y z))

(extend-protocol PolymorphicFunctions
  Entity
  (location
    ([entity]
     (.getLocation entity))
    ([entity n]
     (in-front-of (.getLocation entity) n)))
  (world [e]
    (.getWorld e))
  (as-vec [e]
    (as-vec (location e)))
  (^double x [e] (x (location e)))
  (^double y [e] (y (location e)))
  (^double z [e] (z (location e)))
  (yaw [e] (yaw (location e)))
  (pitch [e] (pitch (location e)))
  (^org.bukkit.util.Vector direction [e] (direction (location e)))

  Block
  (location
    ([entity]
     (.getLocation entity))
    ([entity n]
     (in-front-of (.getLocation entity) n)))
  (world [b]
    (.getWorld b))
  (as-vec [e] (as-vec (location e)))
  (^double x [e] (x (location e)))
  (^double y [e] (y (location e)))
  (^double z [e] (z (location e)))
  (yaw [e] (yaw (location e)))
  (pitch [e] (pitch (location e)))
  (^org.bukkit.util.Vector direction [e] (direction (location e)))
  (material [b] (.getType b))
  (material-name [b] (material-name (material b)))

  Location
  (location [l] l)
  (as-vec [l] (vec3 (x l) (y l) (z l)))
  (direction [l] (.getDirection l))
  (world [l] (.getWorld l))
  (x [l] (.getX l))
  (y [l] (.getY l))
  (z [l] (.getZ l))
  (yaw [l] (.getYaw l))
  (pitch [l] (.getPitch l))
  (add ^Location [this that]
    (Location. (world this)
               (+ (x this)
                  (x that))
               (+ (y this)
                  (y that))
               (+ (z this)
                  (z that))
               (+ (yaw this)
                  (yaw that))
               (+ (pitch this)
                  (pitch that))))
  (distance [this that]
    (distance (as-vec this) that))
  (material [l] (material (get-block l)))
  (material-name [l] (material-name (material l)))

  Vector
  (vector [v] v)
  (x [v] (.getX v))
  (y [v] (.getY v))
  (z [v] (.getZ v))
  (add ^Vector [this that]
    (Vector. (+ (x this)
                (x that))
             (+ (y this)
                (y that))
             (+ (z this)
                (z that))))
  (distance [this that]
    (.distance this (as-vec that)))
  (yaw [v] 0)
  (pitch [v] 0)
  (distance [this that]
    (distance (as-vec this) that))
  (location [l] (location [(x l) (y l) (z l)]))
  (material [l] (material (location l)))
  (material-name [l] (material-name (material l)))

  java.util.Map
  (x [m] (or (.get m :x) 0))
  (y [m] (or (.get m :y) 0))
  (z [m] (or (.get m :z) 0))
  (pitch [m] (or (.get m :pitch) 0))
  (yaw [m] (or (.get m :yaw) 0))
  (world [m] (world (location m)))
  (location [m] (or (.get m :location)
                    (map->Location m)))
  (distance [this that]
    (distance (as-vec this) that))
  (as-vec [m]
    (vec3 (x m) (y m) (z m)))
  (location [l] (location [(x l) (y l) (z l)]))
  (material [l] (material (location l)))
  (material-name [l] (material-name (material l)))

  String
  (world [s]
    (.getWorld (server) s))

  java.util.UUID
  (world [u]
    (.getWorld (server) u))

  clojure.lang.Keyword
  (material [k]
    (get materials k))

  clojure.lang.PersistentVector
  (x [[x _ _]] (or x 0))
  (y [[_ y _]] (or y 0))
  (z [[_ _ z]] (or z 0))
  (yaw [[_ _ _ yaw]] (or yaw 0))
  (pitch [[_ _ _ _ pitch]] (or pitch 0))
  (world [[_ _ _ _ _ w]] (if w (world w) (world (server))))
  (distance [this that]
    (distance (as-vec this) that))
  (location [[x y z yaw pitch world]]
    (map->Location (into {}
                         (remove (comp nil? val))
                         {:x x
                          :y y
                          :z z
                          :yaw yaw
                          :pitch pitch
                          :world world})))
  (add [this that]
    (into []
          (take (count this))
          [(+ (x this) (x that))
           (+ (y this) (y that))
           (+ (z this) (z that))
           (+ (yaw this) (yaw that))
           (+ (pitch this) (pitch that))
           (world this)]))
  (as-vec [[x y z]]
    (vec3 x y z))
  (material [v] (material (location v)))
  (material-name [l] (material-name (material l)))

  Material
  (material [m] m)
  (material-name [m] (get material-names m))

  GlowServer
  (world [s]
    (first (worlds s))))

(load "witchcraft/printers")
