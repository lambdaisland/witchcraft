(ns lambdaisland.witchcraft.citizens.trait
  "Make it easier to make Trait implementation

  To implement a Trait in Citizens you subclass the Trait class, and then
  register the subclass with the TraitFactory. This means you need an actual
  concrete class, so [[proxy]] doens't cut it.

  You can bypass the TraitFactory and `addTrait` to an NPC directly, but then
  your traits won't survive a server restart. Citizens persists which traits a
  given NPC has and restores them on boot, for this to work we have to go
  through the Factory.

  Note that for this to work the code that defines your traits needs to be
  loaded before Citizens restores its state. Adding them in `init` namespaces
  that you declare in your `witchcraft.edn` plugin config should do the trick.

  To make this work we `gen-class` a concrete subclass, but instead of writing
  it out to we just compile and load it in memory (DynamicClassLoader for the
  win!). The class itself is just a dummy stub that delegates method calls to
  callbacks from a registry (a simple atom), so they are easy to redefine. You
  can even use a var directly as a callback, so re-evaluating the `defn` is
  immediately reflected."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [lambdaisland.witchcraft.util :as util]
            [lambdaisland.classpath :as licp])
  (:import (clojure.lang IAtom2 IDeref)
           (net.citizensnpcs Citizens)
           (net.citizensnpcs.api CitizensAPI)
           (net.citizensnpcs.api.trait Trait
                                       TraitFactory
                                       TraitInfo)
           (net.citizensnpcs.api.util DataKey)
           (org.bukkit.entity EntityType)))

(defn trait-factory
  ^TraitFactory
  []
  (CitizensAPI/getTraitFactory))

(defn gen-and-load-class
  "Generates and immediately loads the bytecode for the specified
  class. Note that a class generated this way can be loaded only once
  - the JVM supports only one class with a given name per
  classloader. Subsequent to generation you can import it into any
  desired namespaces just like any other class. See gen-class for a
  description of the options.

  Taken from the rich comment section of `clojure/genclass.clj`.
  "
  [options-map]
  (let [[cname bytecode] (#'clojure.core/generate-class options-map)]
    (.defineClass (licp/root-loader)
                  (str/replace cname
                               #"/"
                               ".")
                  bytecode
                  "")))

(defonce
  ^{:doc "Registry from trait name to mape of callbacks"}
  registry
  (atom {}))

(defn- get-cb [this cb-name]
  (get-in @registry [(.getName this) cb-name]))

(defn- call-cb
  "Invoke a callback for a given trait and callback name. Callback receives
  `this` (the trait) and any additional args."
  [this cb-name & args]
  (when-let [cb (get-cb this cb-name)]
    (apply cb this args)))

(defn munge-key
  "Convert keyword to string, replace `.` with `__`, since `.` is a separator
  character in Bukkit ConfigurationSections."
  [k] (str/replace (str (symbol k)) #"\." "__"))

(defn demunge-key
  "Convert string to keyword, replacing `__` with `.`, see also [[munge-key]]."
  [s] (keyword (str/replace s #"__" ".")))

(defmulti pre-save
  "Prepare a value before persistence, should return either the value unchanged,
  or a map with {:witchcraft/tag ... :value ...}"
  type)

(defmethod pre-save :default [v] v)

(defmulti post-load
  "Coerce a value to the right type after loading based on the `:witchcraft/tag`."
  (fn [v] (and (map? v) (:witchcraft/tag v))))

(defmethod post-load :default [v] v)

(defn flatten-keys
  "Given a (potentially nested) map with keyword keys, return a flat list of
  key/value pairs, with composite string keys that use `.` separators to
  indicate nesting."
  [m]
  (mapcat
   (fn [[k v]]
     (if (map? v)
       (map (fn [[kk vv]]
              [(str (munge-key k) "." kk) vv])
            (flatten-keys v))
       [[(munge-key k) v]]))
   m))

(defn update-datakey
  "Given a DataKey and a (potentially nested) map, store all values in the map on
  the datakey, excluding map values that have the `:no-persist` metadata."
  [^DataKey key m]
  (doseq [[k v] (flatten-keys (reduce dissoc
                                      (dissoc m :no-persist)
                                      (:no-persist m)))
          :when (not (:no-persist (meta v)))]
    (.setRaw key k v)))

(defn datakey->map
  "Decode all hierarchically stored keys/values in the datakey into a nested map
  with keyword keys."
  [^DataKey key]
  (reduce
   (fn [acc s]
     (let [v (.getRaw key s)]
       (if (instance? org.bukkit.configuration.ConfigurationSection v)
         acc
         (assoc-in acc (map demunge-key (str/split s #"\.")) v))))
   {}
   (keys
    (.getValuesDeep key))))

;; Trait method implementations, see the Trait interface docstrings. These
;; generally just delegate to a registered callback function. Some have a
;; default implementation that is used when no matching callback is present.

(defn trait-onAttach [this] (call-cb this :on-attach))
(defn trait-onCopy [this] (call-cb this :on-copy))
(defn trait-onDespawn [this] (call-cb this :on-despawn))
(defn trait-onPreSpawn [this] (call-cb this :on-pre-spawn))
(defn trait-onSpawn [this] (call-cb this :on-spawn))
(defn trait-run [this] (call-cb this :run))
(defn trait-post-init [this] (call-cb this :post-init))

(defn trait-save [this ^DataKey key]
  (if (get-cb this :save)
    (call-cb this :save key)
    (update-datakey key (walk/prewalk pre-save @this))))

(defn trait-load [this ^DataKey key]
  (if (get-cb this :load)
    (call-cb this :load key)
    (swap! this merge (walk/postwalk post-load (datakey->map key)))))

(defn trait-isRunImplemented [this]
  (boolean (get-in @registry [(.getName this) :run])))

;; Atom/IDeref implementation for Trait. Delegate operations to the state atom.

(defn trait-deref [^IDeref this]
  (.deref (.state this)))

(defn trait-swap
  ([this f] (.swap (.state this) f))
  ([this f arg] (.swap (.state this) f arg))
  ([this f arg1 arg2] (.swap (.state this) f arg1 arg2))
  ([this f x y args] (.swap (.state this) f x y args)))

(defn trait-compareAndSet [this oldv newv]
  (.compareAndSet (.state this) oldv newv))

(defn trait-reset [this newval]
  (.reset (.state this) newval))

(defn trait-swapVals
  ([this f] (.swapVals (.state this) f))
  ([this f arg] (.swapVals (.state this) f arg))
  ([this f arg1 arg2] (.swapVals (.state this) f arg1 arg2))
  ([this f x y args] (.swapVals (.state this) f x y args)))

(defn trait-resetVals [this newval]
  (.resetVals (.state this) newval))

(defn make-trait
  "Create a subclass of npcs.citizensapi.trait.Trait, `name` is a simple lowercase
  identifier of the trait like `\"sneak\"` or `\"aggressive\"`. `callbacks` is a
  map from keyword to function (or var, IFn). Callbacks receive `this` (the
  trait) as first arg. `:load` and `:save` also receive a `DataKey`.

  Callbacks: `:load`, `:save`, `:on-attach`, `:on-copy`, `:on-despawn`,
  `:on-pre-spawn`, `:run`, `:save`.

  Can be called multiple times, subsequent calls will only replace the
  callbacks. Returns the trait subclass `java.lang.Class`.

  Note that adding a `:run` callback for a Trait that is already attached to an
  NPC will not cause it to be called. Remove the Trait from the NPC and add it
  back.

  To give your trait state, provide an `:init` key (map or function that returns
  a map), this will provide your trait's initial state. You can use the trait
  instance itself as an atom, e.g. call `@this` or `(swap! this ...)` to get/set
  values. Any state set this way will automatically be persisted by the default
  `:load`/`:save` implementations, unless the value has a `:no-persist`
  metadata.

  To initialize the Trait instance after instantiation, use `:post-init`.
  "
  [trait-name callbacks]
  (let [name (str/lower-case (name trait-name)) ; Trait does the same, stay consistent
        init-fn-name (str "init-" name)
        classname (str *ns* "." name)]
    (swap! registry assoc name callbacks)
    ;; Make a unique `init` function for this class, so we have a way to inject
    ;; the correct name into the superclass constructor
    (intern (the-ns (symbol (namespace `_)))
            (symbol (str "trait-" init-fn-name))
            (fn [] [[name] (atom (if-let [init (:init callbacks)]
                                   (if (map? init)
                                     init
                                     (init))
                                   {}))]))
    (try
      (Class/forName classname)
      (catch java.lang.ClassNotFoundException _
        (gen-and-load-class
         {:name classname
          :extends Trait
          :implements [clojure.lang.IDeref clojure.lang.IAtom2]
          :state "state"
          :init init-fn-name
          :constructors {[] [String]}
          :post-init "post-init"
          :prefix "trait-"
          :impl-ns (namespace `_)
          :load-impl-ns false})))
    (let [klz (Class/forName classname)]
      (when-not (.getTraitClass (trait-factory) name)
        (.registerTrait (trait-factory)
                        (.withName (TraitInfo/create klz) name)))
      klz)))
