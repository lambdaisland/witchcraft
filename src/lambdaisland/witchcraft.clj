(ns lambdaisland.witchcraft
  "Clojure API for Minecraft/Bukkit"
  (:refer-clojure :exclude [bean time])
  (:require [clojure.java.io :as io]
            [lambdaisland.witchcraft.config :as config]
            [lambdaisland.witchcraft.events :as events]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.util :as util])
  (:import (org.bukkit Bukkit Material Location World Server)
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

(defn start!
  "Start an embedded Glowstone server

  Optionally provided with a map of config options.
  See [[lambdaisland.witchcraft.config/key-values]]"
  [& args]
  (apply @(requiring-resolve 'lambdaisland.witchcraft.glowstone/start!) args))

(defn stop!
  "Stop the embedded Glowstone server"
  []
  (@(requiring-resolve 'lambdaisland.witchcraft.glowstone/stop!)))

(declare in-front-of map->Location)

(def ^Plugin plugin
  "For the rare API calls that really want a plugin instance"
  (proxy [org.bukkit.plugin.PluginBase] []
    (getDescription []
      (org.bukkit.plugin.PluginDescriptionFile. "Witchcraft" "0.0" ""))
    (isEnabled []
      true)))

(defonce undo-history (atom ()))
(defonce redo-history (atom ()))

(def EMPTY_BLOCK_SET
  "Make sure there is only one block for each coordinate in the set, and keep them
  sorted by x/z/y."
  (sorted-set-by #(compare ((juxt :x :z :y) %1)
                           ((juxt :x :z :y) %2))))

(defn block-set
  "Create a block set, a set of maps where every :x/:y/:z value can only occur
  once."
  ([coll]
   (into EMPTY_BLOCK_SET coll))
  ([xform coll]
   (into EMPTY_BLOCK_SET xform coll)))

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
  (^org.bukkit.World world [_]
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
  (material-name [_])
  (material-data [_])
  (with-xyz [_ xyz] "Return the same type, but with x/y/z updated"))

(defn server
  "Get the currently active server."
  ^Server []
  (Bukkit/getServer))

(defn plugin-manager
  "Get the Bukkit plugin manager"
  ^PluginManager
  []
  (Bukkit/getPluginManager))

(defn scheduler
  "The Bukkit scheduler"
  ^BukkitScheduler
  []
  (Bukkit/getScheduler))

(defn players
  "List all online players"
  []
  (.getOnlinePlayers (server)))

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
  ([^Server s]
   (.getWorlds (server))))

(defn in-front-of
  "Get the location `n` blocks in front of the given location, based on the
  location's direction. Polymorphic, can work with most things that have a
  location. Returns plain data."
  ([loc]
   (in-front-of loc 1))
  ([loc n]
   (let [dir (direction loc)]
     (add (location loc) {:x (Math/ceil (* n (x dir)))
                          :y (Math/ceil (* n (y dir)))
                          :z (Math/ceil (* n (z dir)))}))))

(defn time
  "Get the current time in the world in ticks."
  ([]
   (time (world (server))))
  ([world]
   (.getTime ^World world)))

(defn set-time
  "Set the time in a world, or the first world on the current server.
  Time is given in ticks, with 20 ticks per second, or 24000 ticks in a
  Minecraft day."
  ([time]
   (set-time (world (server)) time))
  ([^World world time]
   (.setTime world time)))

(defn fast-forward
  "Fast forward the clock, time is given in ticks, with 20 ticks per second, or
  24000 ticks in a Minecraft day."
  ([time]
   (fast-forward (world (server)) time))
  ([^World world time]
   (.setTime world (+ (.getTime world) time))))

(defn loc
  "Get the location of a thing, as a clojure map. Can take most objects that have
  some kind of location, e.g. Location, Player, Vector, Block, Entity. Can also
  take Clojure maps or vectors, so you can use it to coerce input of unknown
  type."
  [obj]
  (cond-> {:x (x obj)
           :y (y obj)
           :z (z obj)
           :pitch (pitch obj)
           :yaw (yaw obj)}
    (world obj)
    (assoc :world (.getName (world obj)))))

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

(defn block
  "Get the block at the given location, returns a Clojure map. See [[get-block]]
  if you need the actual block object."
  [loc]
  (let [block (get-block loc)]
    {:x (long (x block))
     :y (long (y block))
     :z (long (z block))
     :material (material-name block)
     :data (.getData (.getData (.getState block)))}))

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
  ([loc]
   (set-block loc (material loc) (material-data loc)))
  ([loc material]
   (if (vector? material)
     (set-block loc (first material) (second material))
     (set-block loc material nil)))
  ([loc material data]
   (swap! undo-history conj {:before [(block loc)]
                             :after [loc]})
   (let [material (if (keyword? material)
                    (get materials material)
                    material)
         block (get-block (location loc))]
     (prn [material data])
     (if data
       (.setTypeIdAndData block
                          (.getId ^Material material)
                          (if (number? data)
                            (byte data)
                            (.getData ^MaterialData data))
                          true)
       (.setType block material)))
   (when (and (map? loc) (:dir loc))
     (set-block-direction loc))
   loc))

(defn set-blocks
  "Optimized way to set multiple blocks, takes a sequence of maps
  with :x, :y, :z, :material, and optionally :world and :data."
  ([blocks]
   (set-blocks blocks {:keep-history? true}))
  ([blocks {:keys [keep-history?]}]
   (let [^net.glowstone.util.BlockStateDelegate delegate (net.glowstone.util.BlockStateDelegate.)
         blocks (remove nil? blocks)]
     (when keep-history?
       (swap! undo-history conj {:before (doall (map block blocks))
                                 :after blocks}))
     (doseq [{:keys [world x y z data]
              :or {world (world (server))}
              :as block} blocks
             :let [^Material material (material (:material block))
                   _ (assert material)
                   ^MaterialData data (if (number? data) (MaterialData. material (byte data)) data)]]
       (if data
         (.setTypeAndData delegate world x y z material data)
         (.setType delegate world x y z material)))
     (.updateBlockStates delegate))))

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

(defn fly!
  "Set a player as allowing flight and flying.
  Note: doesn't seem to actually cause flying, but it does make flying
  possible."
  ([]
   (fly! (player)))
  ([^Player player]
   (.setAllowFlight player true)
   (.setFlying player true)
   (teleport player [(x player) (inc (y player)) (z player)])))

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
  (let [[material data] (if (vector? material)
                          material
                          [material])
        material ^Material (get materials material)
        stack (ItemStack. material (int count))]
    (when data
      (.setData stack (.getNewData material (byte data))))
    stack))

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

(defn xyz
  "Get the x/y/z values of an object as a vector, can work with maps containing
  `:x`/`:y`/`:z` keys, or virtually any Glowstone object that encodes or has a
  location."
  [o]
  [(x o) (y o) (z o)])

(defn set-bed-spawn-location
  "Set the location where the player will respawn, set `force?` to true to update
  the spawn location even if there is no valid bed available."
  ([^Player player loc]
   (.setBedSpawnLocation player (location loc)))
  ([^Player player loc force?]
   (.setBedSpawnLocation player (location loc) force)))

(defn xyz1
  "Like [[xyz]], but add an extra `1` at the end, for affine transformations"
  [o]
  [(x o) (y o) (z o) 1])

(defn health
  "Get the player's health, a number between 0 and 20"
  [^Player player]
  (.getHealth player))

(defn set-health
  "Set the player's health, a number between 0 and 20"
  [^Player player health]
  (.setHealth player health))

(defn food-level
  "Get the player's food level, a number between 0 and 20"
  [^Player player]
  (.getFoodLevel player))

(defn set-food-level
  "Set the player's food level, a number between 0 and 20"
  [^Player player food-level]
  (.setFoodLevel player health))

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
  (x [e] (x (location e)))
  (y [e] (y (location e)))
  (z [e] (z (location e)))
  (yaw [e] (yaw (location e)))
  (pitch [e] (pitch (location e)))
  (^org.bukkit.util.Vector direction [e] (direction (location e)))
  (material [b] (.getType b))
  (material-name [b] (material-name (material b)))
  (material-data [b] (.getData (.getData (.getState b))))
  (add [this that]
    (add (location this) that))

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
  (material-data [l] (material-data (get-block l)))
  (with-xyz [this [x y z]]
    (Location. (world this)
               x
               y
               z
               (yaw this)
               (pitch this)))

  Vector
  (as-vec [v] v)
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
  (world [v] )
  (location [l] (location [(x l) (y l) (z l)]))
  (material [l] (material (location l)))
  (material-name [l] (material-name (material l)))
  (material-data [l] (material-data (get-block l)))
  (with-xyz [_ [x y z]] (vec3 x y z))

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
  (material [m] (material (or (.get m :material)
                              (location m))))
  (material-name [m] (or (.get m :material)
                         (material-name (material m))))
  (material-data [m] (or (.get m :data)
                         (material-data (get-block m))))
  (add [this that]
    (-> this
        (update :x + (x that))
        (update :y + (y that))
        (update :z + (z that))))
  (with-xyz [m [x y z]]
    (assoc m :x x :y y :z z))

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
  (material-data [l] (material-data (get-block l)))
  (with-xyz [m [x y z]]
    (assoc m 0 x 1 y 2 z))

  Material
  (material [m] m)
  (material-name [m] (get material-names m))

  Server
  (world [s]
    (first (worlds s))))

(defn undo!
  "Undo the last build. Can be repeated to undo multiple builds."
  []
  (swap! undo-history (fn [[{:keys [before after] :as op} & rest]]
                        (when op
                          (set-blocks before {:keep-history? false})
                          (swap! redo-history conj op))
                        rest))
  :undo)

(defn redo!
  "Redo the last build that was undone with [[undo!]]"
  []
  (swap! redo-history (fn [[{:keys [before after] :as op} & rest]]
                        (when op
                          (set-blocks after {:keep-history? false})
                          (swap! undo-history conj op))
                        rest))
  :redo)

(load "witchcraft/printers")
