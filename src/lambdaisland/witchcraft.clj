(ns lambdaisland.witchcraft
  "Clojure API for Minecraft/Bukkit"
  (:refer-clojure :exclude [time bean chunk])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [lambdaisland.witchcraft.events :as events]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.util :as util]
            [lambdaisland.witchcraft.markup :as markup]
            [lambdaisland.witchcraft.reflect :as reflect])
  (:import (java.util UUID)
           (com.cryptomorin.xseries XMaterial XBlock)
           (org.bukkit Bukkit Chunk Location Material Server World WorldCreator)
           (org.bukkit.block Block BlockFace)
           (org.bukkit.configuration.serialization ConfigurationSerialization)
           (org.bukkit.enchantments Enchantment)
           (org.bukkit.entity Entity Player HumanEntity LivingEntity)
           (org.bukkit.inventory ItemStack Inventory)
           (org.bukkit.inventory.meta ItemMeta)
           (org.bukkit.material MaterialData Directional)
           (org.bukkit.plugin PluginManager Plugin)
           (org.bukkit.potion Potion PotionEffectType)
           (org.bukkit.scheduler BukkitScheduler)
           (org.bukkit.util Vector)))

(set! *warn-on-reflection* true)

(defonce server-type nil)

(def ^:dynamic *default-world* nil)

(defn pre-flattening? []
  (not (XMaterial/supports 13)))

(defn start-glowstone!
  "Start an embedded Glowstone server

  Optionally provided with a map of config options.
  See [[lambdaisland.witchcraft.config/key-values]]"
  [& args]
  (alter-var-root #'server-type (constantly :glowstone))
  (apply @(requiring-resolve 'lambdaisland.witchcraft.glowstone/start!) args))

(defn start-paper!
  "Start an embedded PaperMC server"
  [& [gui?]]
  (alter-var-root #'server-type (constantly :paper))
  (@(requiring-resolve 'lambdaisland.witchcraft.paper/start!) gui?))

(defn stop!
  "Stop the embedded server"
  []
  (case server-type
    :glowstone
    (@(requiring-resolve 'lambdaisland.witchcraft.glowstone/stop!))
    :paper
    (throw (ex-info "stop! not implemented for PaperMC" {}))))

(declare in-front-of map->Location)

(def ^Plugin plugin
  "For the rare API calls that really want a plugin instance"
  (proxy [org.bukkit.plugin.PluginBase] []
    (getDescription []
      (org.bukkit.plugin.PluginDescriptionFile. "Witchcraft" "0.0" ""))
    (isEnabled []
      true)
    (getLogger []
      (java.util.logging.Logger/getLogger "witchcraft"))))

(defonce undo-history (atom ()))
(defonce redo-history (atom ()))

;; FIXME: these don't actually work the way I inteneded them to work, might
;; require a custom set implementation.
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

(def entity-types
  "Map from keyword to EntityType value"
  (util/enum->map org.bukkit.entity.EntityType))

(util/when-class-exists
 org.bukkit.GameRule
 (def game-rule-types
   "Map from keyword to GameRule"
   (into {}
         (map (juxt #(keyword (util/dasherize (.getName ^org.bukkit.GameRule %))) identity))
         (org.bukkit.GameRule/values))))


(defonce ^{:doc "Map from keyword to XMaterial value"} materials {})
(defonce ^{:doc "Map from XMaterial value to keyword"} material-names {})

(def block-faces
  "Map from keyword to BlockFace value"
  (util/enum->map org.bukkit.block.BlockFace))

(def block-face-names
  "Map from BlockFace value to keyword"
  (into {} (map (juxt val key)) block-faces))

(def tree-species
  "Map from keyword to TreeSpecies value"
  (util/enum->map org.bukkit.TreeSpecies))

(def block-actions
  "Map from keyword to block.Action value"
  (util/enum->map org.bukkit.event.block.Action))

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

;; =============================================================================

;; Functions for retrieving things from objects, like the location, x/y/z
;; values, world, etc. We also implement these for maps, where they do keyword
;; lookups. By using these in other functions we can accept a large range of
;; inputs, including both Glowstone/Bukkit objects and Clojure maps.
(defprotocol PolymorphicFunctions
  ;; Keep these classnames fully qualified, Clojure doesn't like it otherwise
  ;; when called from namespaces where these are not imported.
  (-add [this that]
    "Add locations, vectors, etc. That can also be a map of `:x`, `:y`, `:z`")
  (^double x [_])
  (^double y [_])
  (^double z [_])
  (yaw [_])
  (pitch [_])
  (^org.bukkit.util.Vector direction-vec [_])
  (^org.bukkit.util.Vector as-vec [_] "Coerce to org.bukkit.util.Vector")
  (material-data [_])
  (with-xyz [_ xyz] "Return the same type, but with x/y/z updated")
  (^org.bukkit.Chunk chunk [_] "Retrieve the chunk for a location, entity, etc.")
  (entities [_] "Get the chunk's entities"))

(defprotocol HasLocation
  (^org.bukkit.Location -location [_] "Get the location of the given object"))

(defprotocol HasWorld
  (^org.bukkit.World -world [_] [_ _]))

(defprotocol HasWorlds
  (^java.util.List -worlds [_])
  (^org.bukkit.World -world-by-name [_ ^String _])
  (^org.bukkit.World -world-by-uuid [_ ^UUID _]))

(defprotocol HasItemMeta
  (^org.bukkit.inventory.meta.ItemMeta -item-meta [_])
  (-set-item-meta [_ ^org.bukkit.inventory.meta.ItemMeta _]))

(defprotocol HasMaterial
  (^org.bukkit.Material -material [_]))

(defprotocol HasDisplayName
  (^String -display-name [_])
  (-set-display-name [_ ^String _]))

(defprotocol HasLore
  (^java.util.List -lore [_])
  (-set-lore [_ _]))

(defprotocol HasTargetBlock
  (^org.bukkit.Block -get-target-block [_ transparent max-distance]))

(defprotocol HasBlockData
  (^org.bukkit.block.data.BlockData -get-block-data [_]))

(defprotocol CanParseBlockData
  (^org.bukkit.block.data.BlockData -parse-block-data [_ _]))

(declare ^Material material)
(declare ^clojure.lang.Keyword material-name)
(declare ^XMaterial xmaterial)
(declare ^Block get-block)
(declare ^clojure.lang.IPersistentMap block)

(defn add
  "Add multiple vector/location-like things together.
  Returns a value with the same type of the first argument."
  ([this that]
   (-add this that))
  ([this that & more]
   (reduce -add (-add this that) more)))

(defn location
  "Get the location of the given object"
  ^Location [o]
  (cond
    (instance? Location o)
    o

    (vector? o)
    (let [[x y z yaw pitch world] o]
      (map->Location (into {}
                           (remove (comp nil? val))
                           {:x x
                            :y y
                            :z z
                            :yaw (when (number? yaw) yaw)
                            :pitch pitch
                            :world world})))

    (map? o)
    (or (:location o)
        (map->Location o))

    :else
    (-location o)))

(defn world
  "Get the world associated with a given object, or look up a world by string or
  UUID."
  (^World [o]
   (cond
     (instance? World o)
     o

     (satisfies? HasWorld o)
     (-world o)

     (string? o)
     (-world-by-name (server) o)

     (instance? java.util.UUID o)
     (-world-by-uuid (server) o)

     :else
     (-world (location o))))
  (^World [o n]
   (cond
     (string? n)
     (-world-by-name o n)

     (instance? java.util.UUID n)
     (-world-by-uuid o n))))

(defn worlds
  "Get a list of all worlds on the server"
  ([]
   (Bukkit/getWorlds))
  ([o]
   (-worlds o)))

(def
  ^{:doc "Render hiccup-like markup into a string with color and styling codes that Minecraft understand, see the [[lambdaisland.witchcraft.markup]] namespace for details."}
  render-markup
  markup/render)

(defn item-meta
  "Get the ItemMeta for an item or compatible object, this object contains things
  like the display name of the item, if it has been renamed."
  [o]
  (-item-meta o))

(defn display-name
  "Get the display-name for an item or compatible object."
  [o]
  (if (satisfies? HasDisplayName o)
    (-display-name o)
    (display-name (item-meta o))))

(defn lore
  "Get the lore for an item or compatible object."
  [o]
  (if (satisfies? HasLore o)
    (-lore o)
    (lore (item-meta o))))

(defn set-lore
  "Set the lore on an item, itemstack, itemmeta. Takes a list (sequence) of
  strings, or of markup vectors as per [[lambdaisland.witchcraft.markup/render]]."
  [o lore]
  (if (satisfies? HasLore o)
    (-set-lore o (map #(markup/render [:reset %1]) lore))
    (let [m (item-meta o)]
      (-set-lore m (map #(markup/render [:reset %1]) lore))
      (-set-item-meta o m))))

(defn set-display-name
  "Set the display-name on an item, itemstack, itemmeta. Takes a list (sequence)
  of strings, or of markup vectors as
  per [[lambdaisland.witchcraft.markup/render]]."
  [o name]
  (if (satisfies? HasDisplayName o)
    (-set-display-name o (markup/render name))
    (let [m (item-meta o)]
      (-set-display-name m (markup/render name))
      (-set-item-meta o m))))

(defn set-item-meta
  "Set the ItemMeta for compatible objects, like an ItemStack. Either takes an
  actual ItemMeta object, or a map with `:name` and/or `:lore` keys."
  [o im]
  (cond
    (satisfies? HasItemMeta o)
    (-set-item-meta o im)

    (map? im)
    (let [my-meta (item-meta o)]
      (doseq [[k v] im]
        (case k
          :name (set-display-name my-meta v)
          :lore (set-lore my-meta v)))
      (set-item-meta o my-meta))))

(defn get-target-block
  "Get the target block in the line of sight of the entity/player, as org.bukkit.block.Block"
  (^org.bukkit.block.Block [entity]
   (get-target-block entity #{:air}))
  (^org.bukkit.block.Block [entity transparent]
   (get-target-block entity transparent 20))
  (^org.bukkit.block.Block [entity transparent max-distance]
   (cond
     (satisfies? HasTargetBlock entity)
     (-get-target-block entity (into #{} (map material) transparent) max-distance))))

(defn target-block
  "Get the target block in the line of sight of the entity/player, as a map"
  [& args]
  (block (apply get-target-block args)))

(defn get-block-data
  "Get the BlockData object of something"
  ^org.bukkit.block.data.BlockData
  [b]
  (cond
    (satisfies? HasBlockData b)
    (-get-block-data b)
    (string? b)
    (-parse-block-data (server) b)
    :else
    (-get-block-data (get-block b))))

(defn block-data
  "Get the block data for the block as a map with `:material`, and other
  material-specific key-values. This is kind of hacky in that we essentially
  parse this out of the stringified version of the `BlockData`, but given the
  immense parochialism in concrete `BlockData` subclasses we have little other
  choice, and we can still reconstitute the `BlockData` by parsing it with
  `Server/createBlockData`."
  [b]
  (let [bd (get-block-data b)
        [_ _ attrs] (re-find #"^([^\[]*)\[(.*)\]"
                             (.getAsString bd))]
    (into {:material (material-name bd)}
          (map (fn [s]
                 (let [[k v] (str/split s #"=")]
                   [(keyword k)
                    (cond
                      (= "true" v) true
                      (= "false" v) false
                      (re-find #"\d+" v) (Long/parseLong v)
                      (re-find #"\d+\.\d+" v) (Double/parseDouble v)
                      :else (keyword v))])))
          (str/split attrs #","))))

(defn material
  "Get the `org.bukkit.Material` for the given object.

  - map: looks for a `:material` key
  - vector: will look for a keyword in the fourth position
    (so you can use `[x y z material]``)
  - keyword: looks up the named material
  - Objects that implement `getMaterial()` or `getType()`, use
    the object method to get the material
  - location-like things: pass on to `get-block` to get the
    material at that location
  "
  [m]
  (cond
    (satisfies? HasMaterial m)
    (-material m)

    (keyword? m)
    (if-let [xm (xmaterial m)]
      (.parseMaterial xm)
      (throw (ex-info (str "Material not found: " m) {:material m
                                                      :known-materials
                                                      (count materials)})))

    (map? m)
    (material (:material m (location m)))

    (vector? m)
    (let [[_ _ _ m] m]
      (when (keyword? m)
        (material m)))

    :else
    (-material (get-block m))))

(defn material-name [m]
  (if (instance? XMaterial m)
    (get material-names m)
    (material-name (xmaterial m))))

(reflect/extend-signatures HasLocation
  "org.bukkit.Location getLocation()"
  (-location [this]
    (.getLocation this)))

(reflect/extend-signatures HasWorld
  "org.bukkit.World getWorld()"
  (-world [this]
    (.getWorld this)))

(reflect/extend-signatures HasWorlds
  "java.util.List getWorlds()"
  (-worlds [this]
    (.getWorlds this))
  "org.bukkit.World getWorld(java.lang.String)"
  (-world-by-name [this ^String obj]
    (.getWorld this obj))
  "org.bukkit.World getWorld(java.util.UUID)"
  (-world-by-uuid [this ^java.util.UUID obj]
    (.getWorld this obj)))

(reflect/extend-signatures HasItemMeta
  "org.bukkit.inventory.meta.ItemMeta getItemMeta()"
  (-item-meta [this]
    (.getItemMeta this))
  "setItemMeta(org.bukkit.inventory.meta.ItemMeta)"
  (-set-item-meta [this ^org.bukkit.inventory.meta.ItemMeta im]
    (.setItemMeta this im)))

(reflect/extend-signatures HasDisplayName
  "java.lang.String getDisplayName()"
  (-display-name [this]
    (.getDisplayName this))
  "setDisplayName(java.lang.String)"
  (-set-display-name [this ^String n]
    (.setDisplayName this n)))

(reflect/extend-signatures HasLore
  "java.util.List getLore()"
  (-lore [this]
    (.getLore this))
  "setLore(java.util.List)"
  (-set-lore [this lore]
    (.setLore this lore)))

(reflect/extend-signatures HasTargetBlock
  "org.bukkit.block.Block getTargetBlock(java.util.Set,int)"
  (-get-target-block [this ^java.util.Set transparent ^long max-distance]
    (.getTargetBlock this transparent max-distance)))

(reflect/extend-signatures HasBlockData
  "org.bukkit.block.data.BlockData getBlockData()"
  (-get-block-data [this]
    (.getBlockData this)))

(reflect/extend-signatures HasMaterial
  "org.bukkit.Material getMaterial()"
  (-material [this] (.getMaterial this))
  "org.bukkit.Material getType()"
  (-material [this] (.getType this)))

(reflect/extend-signatures CanParseBlockData
  "org.bukkit.block.data.BlockData createBlockData(java.lang.String)"
  (-parse-block-data [this ^String string]
    (.createBlockData this string)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn online-players
  "List all online players"
  []
  (.getOnlinePlayers (server)))

(defn player
  "Get a player by name, or simply the first player found."
  (^Player []
   (first (online-players)))
  (^Player [name]
   (some #(when (= name (:name (bean %)))
            %)
         (online-players))))

(defn default-world
  "World used for commands when no explicit world is given.
  Uses [[*default-world*]], or the first world on the server."
  []
  (if *default-world*
    (world *default-world*)
    (first (worlds (server)))))

(defn in-front-of
  "Get the location `n` blocks in front of the given location, based on the
  location's direction. Polymorphic, can work with most things that have a
  location. Returns plain data."
  ([loc]
   (in-front-of loc 1))
  ([loc n]
   (let [dir (direction-vec loc)]
     (-add (location loc) {:x (Math/ceil (* n (x dir)))
                           :y (Math/ceil (* n (y dir)))
                           :z (Math/ceil (* n (z dir)))}))))

(defn time
  "Get the current time in the world in ticks."
  ([]
   (time (default-world)))
  ([world]
   (.getTime ^World world)))

(defn ^:scheduled set-time
  "Set the time in a world, or the first world on the current server.
  Time is given in ticks, with 20 ticks per second, or 24000 ticks in a
  Minecraft day."
  ([time]
   (set-time (default-world) time))
  ([^World world time]
   (.setTime world time)))

(defn fast-forward
  "Fast forward the clock, time is given in ticks, with 20 ticks per second, or
  24000 ticks in a Minecraft day."
  ([time]
   (fast-forward (default-world) time))
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
  ^Location [{:keys [x y z yaw pitch]
              :or {x 0 y 0 z 0 yaw 0 pitch 0}
              :as opts}]
  (Location. (if-let [w (:world opts)]
               (world w)
               (default-world))
             x y z
             yaw pitch))

(defn player-chunk
  "Return the chunk the player is in"
  [^Player player]
  (let [{:keys [world location]} (bean (player))]
    (.getChunkAt ^World (.getWorld player) ^Location (location player))))

(defn get-block
  "Get the block at a given location. Idempotent."
  ^Block [loc]
  (if (instance? Block loc)
    loc
    (.getBlockAt ^World (world loc) (x loc) (y loc) (z loc))))

(defn direction
  "Get the direction of a Block, or of the block at a certain location."
  [block-or-loc]
  (let [block (get-block block-or-loc)]
    (get block-face-names (XBlock/getDirection block))))

(defn block-face
  "Coerce to BlockFace

  Use to convert keywords like :east/:west/:self to BlockFace instances. Idempotent."
  ^BlockFace [kw]
  (if (keyword? kw)
    (get block-faces kw)
    kw))

(defn block
  "Get the block at the given location, returns a Clojure map. See [[get-block]]
  if you need the actual block object."
  [block-or-loc]
  (let [block (get-block block-or-loc)
        direction (direction block)]
    (cond-> {:x (long (x block))
             :y (long (y block))
             :z (long (z block))
             :material (material-name block)}
      (pre-flattening?)
      (assoc :data (.getData (.getData (.getState block))))
      (not= :self direction)
      (assoc :direction direction))))

(defmulti -set-direction (fn [server block face] server))

(defmethod -set-direction :default [_ block face]
  (XBlock/setDirection block face))

(defn set-direction
  "Set the direction of a block, takes a keyword or BlockFace,
  see [[block-faces]]"
  [loc dir]
  (let [block (get-block loc)]
    (-set-direction server-type block (block-face dir)))
  loc)

(defn xmaterial
  "Get an XMaterial instance from a keyword, string, Material, or ItemStack, or
  anything else for which the polymorphic [[material]] method is implemented.

  This is a shim class which provides version-independent material handling."
  ^XMaterial [m]
  (when m
    (cond
      (keyword? m)
      (get materials m)

      (satisfies? HasMaterial m)
      (xmaterial (-material m))

      ;; The nonsense we pull to prevent a few reflection warnings...
      (string? m)
      (XMaterial/matchXMaterial ^String m)

      (instance? Material m)
      (XMaterial/matchXMaterial ^Material m)

      (instance? ItemStack m)
      (XMaterial/matchXMaterial ^ItemStack m)

      :else
      (XMaterial/matchXMaterial ^Material (material m)))))

(defmulti -set-block "Server-specific set-block implementation"
  (fn [server block material direction] server))

(defmethod -set-block :default [_ block material direction]
  (let [xmaterial (xmaterial material)]
    (XBlock/setType block xmaterial))
  (when direction
    (set-direction block direction)))

(defn set-block
  "Set the block at a specific location to a specific material

  `material` can be a keyword (see [[materials]]), a String like `RED_WOOL` or
  `WOOL:14`, a bukkit Material, a Bukkit ItemStack, or an xseries XMaterial."
  ([loc]
   (set-block loc (material loc)))
  ([loc material]
   (let [b (get-block loc)]
     (swap! undo-history conj {:before [(block b)]
                               :after [loc]})
     (-set-block server-type b material (when (map? loc) (:direction loc))))
   loc))

(defmulti -set-blocks (fn [server blocks] server))

(defmethod -set-blocks :default [_ blocks]
  (doseq [{:keys [direction material] :as loc} blocks
          :let [block (get-block loc)]]
    (-set-block server-type block material direction)))

(defn set-blocks
  "Set blocks in bulk

  Takes a sequence of maps with `:x`, `:y`, `:z`, `:material`, and optionally
  `:world` and `:data`, or a sequence of `[x y z material]` vectors.
  Alternatively you can pass a sequence of any object that responds to the `x`,
  `y`, `z`, and `material-name` methods.

  This adds an entry to the undo history, capturing the previous state of the
  blocks it is changing, so you can [[undo!]] and then [[redo!]] the result,
  unless `:keep-history?` is set to `false`.

  Optionally takes an `:anchor` option, which then offsets the whole structure
  by that distance (can be a map, vector, `Location`, bukkit `Vector`, etc.), so
  you can define the structure you are passing in independently of its position
  in the world.

  Currently only optimized on Glowstone, elsewhere it repeatedly
  calls [[set-block]], so changing really large amounts of blocks at once will
  incur significant lag."
  ([blocks]
   (set-blocks blocks {:keep-history? true}))
  ([blocks {:keys [keep-history? anchor]
            :or {keep-history? true}}]
   (let [blocks (as-> blocks $
                  (remove nil? $)
                  (if anchor
                    (map #(add % anchor) $)
                    $)
                  (map (fn [b]
                         (if (map? b)
                           b
                           {:x (x b)
                            :y (y b)
                            :z (z b)
                            :material (material-name b)}))
                       $))]
     (when keep-history?
       (swap! undo-history conj {:before (doall (map block blocks))
                                 :after blocks}))
     (-set-blocks server-type blocks))))

;; TODO: move this elsewhere
#_
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
                            (get entity-types entity)
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
  ([^Entity entity l]
   (.teleport entity
              (location
               (if (map? l)
                 (merge (loc entity)
                        l)
                 l)))))

(defn fly!
  "Set a player as allowing flight and flying.
  Note: doesn't seem to actually cause flying, but it does make flying
  possible."
  ([]
   (fly! (player)))
  ([^Player player]
   (.setAllowFlight player true)
   (.setFlying player true)
   (teleport player (update (loc player) :y inc))))

(defn clear-weather
  "Get clear weather"
  ([]
   (clear-weather (default-world)))
  ([^World world]
   (doto world
     (.setThundering false)
     (.setStorm false))))

(defn inventory
  "Get the player's inventory"
  ^Inventory [player]
  (.getInventory ^Player player))

(defn item-stack
  "Create an ItemStack object"
  ^ItemStack
  ([material]
   (item-stack material 1))
  ([material count]
   (let [^ItemStack is (if (instance? ItemStack material)
                         material
                         (.parseItem (xmaterial material)))]
     (.setAmount is count)
     is)))

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
  "Create a org.bukkit.util.Vector instance"
  ^Vector [^double x ^double y ^double z]
  (Vector. x y z))

(defn xyz
  "Get the x/y/z values of an object as a vector, can work with maps containing
  `:x`/`:y`/`:z` keys, or virtually any Glowstone object that encodes or has a
  location."
  [o]
  [(x o) (y o) (z o)])

(defn xyz1
  "Like [[xyz]], but add an extra `1` at the end, for affine transformations"
  [o]
  [(x o) (y o) (z o) 1])

(defn distance
  "Get the euclidian distance between two locations. Can take various bukkit
  objects (Location, Block, Vector), as well as Clojure vectors `[x y z]` or
  maps `{:x x :y y :z z}`, and any combination thereof."
  [this that]
  (let [[^double x1 ^double y1 ^double z1] (xyz this)
        [^double x2 ^double y2 ^double z2] (xyz that)]
    (Math/sqrt
     (+ (Math/pow (- x2 x1) 2)
        (Math/pow (- y2 y1) 2)
        (Math/pow (- z2 z1) 2)))))

(defn set-bed-spawn-location
  "Set the location where the player will respawn, set `force?` to true to update
  the spawn location even if there is no valid bed available."
  ([^Player player loc]
   (.setBedSpawnLocation player (location loc)))
  ([^Player player loc force?]
   (.setBedSpawnLocation player (location loc) force)))

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
  (.setFoodLevel player food-level))

(extend-protocol PolymorphicFunctions
  Entity
  (as-vec [e]
    (as-vec (location e)))
  (^double x [e] (x (location e)))
  (^double y [e] (y (location e)))
  (^double z [e] (z (location e)))
  (yaw [e] (yaw (location e)))
  (pitch [e] (pitch (location e)))
  (^org.bukkit.util.Vector direction-vec [e] (direction-vec (location e)))

  Block
  (as-vec [e] (as-vec (location e)))
  (x [e] (x (location e)))
  (y [e] (y (location e)))
  (z [e] (z (location e)))
  (yaw [e] (yaw (location e)))
  (pitch [e] (pitch (location e)))
  (^org.bukkit.util.Vector direction-vec [e] (direction-vec (location e)))
  (material-data [b] (when (pre-flattening?)
                       (.getData (.getData (.getState b)))))
  (-add [this that]
    (-add (location this) that))

  Location
  (as-vec [l] (vec3 (x l) (y l) (z l)))
  (direction-vec [l] (.getDirection l))
  (x [l] (.getX l))
  (y [l] (.getY l))
  (z [l] (.getZ l))
  (yaw [l] (.getYaw l))
  (pitch [l] (.getPitch l))
  (-add ^Location [this that]
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
  (material-data [l] (material-data (get-block l)))
  (with-xyz [this [x y z]]
    (Location. (world this)
               x
               y
               z
               (yaw this)
               (pitch this)))
  (chunk [this]
    (.getChunk this))

  Vector
  (as-vec [v] v)
  (x [v] (.getX v))
  (y [v] (.getY v))
  (z [v] (.getZ v))
  (-add ^Vector [this that]
    (Vector. (+ (x this)
                (x that))
             (+ (y this)
                (y that))
             (+ (z this)
                (z that))))
  (yaw [v] 0)
  (pitch [v] 0)
  (world [v] )
  (material-data [l] (material-data (get-block l)))
  (with-xyz [_ [x y z]] (vec3 x y z))

  java.util.Map
  (x [m] (or (.get m :x) 0))
  (y [m] (or (.get m :y) 0))
  (z [m] (or (.get m :z) 0))
  (pitch [m] (or (.get m :pitch) 0))
  (yaw [m] (or (.get m :yaw) 0))
  (world [m] (world (location m)))
  (as-vec [m]
    (vec3 (x m) (y m) (z m)))
  (material-data [m]
    (when (pre-flattening?)
      (or (.get m :data)
          (material-data (get-block m)))))
  (-add [this that]
    (-> this
        (update :x + (x that))
        (update :y + (y that))
        (update :z + (z that))))
  (with-xyz [m [x y z]]
    (assoc m :x x :y y :z z))
  (chunk [this]
    (chunk (location this)))

  clojure.lang.Keyword



  ;; Vectors can be used in two ways
  ;; [x y z yaw pitch world]
  ;; [x y z material]
  ;; Elements at the end can be omitted. If the fourth element
  ;; is a number it's considered a yaw, if it's a keyword it's considered a
  ;; material-name
  clojure.lang.PersistentVector
  (x [[x _ _]] (or x 0))
  (y [[_ y _]] (or y 0))
  (z [[_ _ z]] (or z 0))
  (yaw [[_ _ _ yaw]] (if (number? yaw) yaw 0) 0)
  (pitch [[_ _ _ _ pitch]] (or pitch 0))
  (world [[_ _ _ _ _ w]] (if w (world w) (default-world)))
  (-add [this that]
    (assoc this
           0 (+ (x this) (x that))
           1 (+ (y this) (y that))
           2 (+ (z this) (z that))))
  (as-vec [[x y z]]
    (vec3 x y z))
  (with-xyz [m [x y z]]
    (assoc m 0 x 1 y 2 z))
  (chunk [this]
    (chunk (location this)))

  Server
  (world [s]
    (first (worlds s)))

  Chunk
  (entities [c]
    (seq (.getEntities c)))
  (x [c]
    (.getX c))
  (y [c])
  (z [c]
    (.getZ c))
  (world [c]
    (.getWorld c)))

(defn undo!
  "Undo the last build. Can be repeated to undo multiple builds."
  []
  (swap! undo-history
         (fn [[{:keys [before after] :as op} & rest]]
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

(defn init-xmaterial!
  "We can only access the XMaterial class once a Bukkit server is available"
  []
  ;; Make sure the clojure compiler doesn't try to access the class either
  (eval
   `(when (server)
      (alter-var-root
       #'materials
       (constantly (util/enum->map XMaterial)))

      (alter-var-root
       #'material-names
       (constantly
        (into {} (map (juxt val key)) materials)))

      (extend-type XMaterial
        HasMaterial
        (-material [m#] (.parseMaterial m#))))))

;; TODO: Glowstone 1.12 does not recognize biomeProvider or hardcore

(defn world-creator [^String name opts]
  (reduce
   (fn [^WorldCreator wc [k v]]
     (case k
       ;; TODO: most of these are polymorphic, add type dispatch. For now only
       ;; supporting most basic/primitive version
       #_#_:biome-provider (.biomeProvider wc ^String v)
       :copy (.copy wc ^World v)
       :environment (.environment wc v)
       :seed (.seed wc v)
       :structures? (.generateStructures wc v)
       :generator (.generator wc ^String v)
       :generator-settings (.generatorSettings wc v)
       :hardcore? (.hardcore wc v)
       :type (.type wc v)))
   (WorldCreator. name)
   opts))

(defn create-world
  "Create a new world with a given name and options

  - :seed long
  - :structures? boolean
  - :hardcore? boolean"
  [name opts]
  (.createWorld (server) (world-creator name opts)))

(defn fly-speed [^Player player]
  (.getFlySpeed player))

(defn set-fly-speed [^Player player speed]
  (.setFlySpeed player speed))

(defn allow-flight
  ([player]
   (allow-flight player true))
  ([^Player player bool]
   (.setAllowFlight player bool)))

(defn active-potion-effects [^Player player]
  (.getActivePotionEffects player))

(defn active-item [^Player player]
  (.getActiveItem player))

(util/when-class-exists
 org.bukkit.GameRule
 (defn set-game-rule
   "Set a game rule like `:do-daylight-cycle` or `:do-insomnia`.
  See `(keys wc/game-rule-types)` for all options"
   [wrld kw bool]
   (if-let [rule (get game-rule-types kw)]
     (.setGameRule (world wrld) ^org.bukkit.GameRule rule bool)
     (throw (ex-info (str "No such game rule " kw ", see " `game-rule-types)))))

 (defn set-game-rules
   "Set multiple game rules like `:do-daylight-cycle` or `:do-insomnia`.
  See `(keys wc/game-rule-types)` for all options. Takes a map from keyword to
  bool."
   [world m]
   (run! #(set-game-rule world (key %) (val %)) m)))

(defn eye-height [^LivingEntity e]
  (.getEyeHeight e))

(defn eye-location [^LivingEntity e]
  (loc (.getEyeLocation e)))

(defn send-title
  "Send a title and optionally subtitle to the player

  Titles are splashed in big type over the middle of the screen, before they
  fade out.

  Takes strings or vectors (markup), e.g.

  ```
  [:red \"hello \" [:underline \"world\"]]
  ```

  See [[markup/codes]]."
  ([^Player p title]
   (send-title p title ""))
  ([^Player p title subtitle]
   (.sendTitle p
               (markup/render title)
               (markup/render subtitle))))

(defn send-message
  "Send a chat message to a specific player

  Takes strings or vectors (markup), e.g.

  ```
  [:red \"hello \" [:underline \"world\"]]
  ```

  See [[markup/codes]]."
  [^Player p message]
  (.sendMessage p (markup/render message)))

(defn normalize-text
  "When dealing with text which contains `§`-based color and markup codes,
  \"normalize\" it by setting it on an item and reading back the result. This
  will cause the server to strip out redundant markup. Useful if you later want
  to use equality checks on the name of items.

  Can also take hiccup-like markup as per [[lambdaisland.witchcraft.markup]]"
  [txt]
  (display-name
   (doto (item-stack :wooden-axe 1)
     (set-display-name (render-markup txt)))))

(load "witchcraft/printers")
