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
            [lambdaisland.witchcraft.util :as util]
            [lambdaisland.classpath :as licp])
  (:import (net.citizensnpcs Citizens)
           (net.citizensnpcs.api CitizensAPI)
           (net.citizensnpcs.api.trait Trait
                                       TraitFactory
                                       TraitInfo)
           (org.bukkit.entity EntityType)))

(defn trait-factory ^TraitFactory
  "Citizens' TraitFactory"
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

(defn- call-cb
  "Invoke a callback for a given trait and callback name. Callback receives
  `this` (the trait) and any additional args."
  [this cb-name & args]
  (when-let [cb (get-in @registry [(.getName this) cb-name])]
    (apply cb this args)))

;; The callbacks we understand
(defn trait-load [this key] (call-cb this :load key))
(defn trait-onAttach [this] (call-cb this :on-attach))
(defn trait-onCopy [this] (call-cb this :on-copy))
(defn trait-onDespawn [this] (call-cb this :on-despawn))
(defn trait-onPreSpawn [this] (call-cb this :on-pre-spawn))
(defn trait-onSpawn [this] (call-cb this :on-spawn))
(defn trait-run [this] (call-cb this :run))
(defn trait-save [this key] (call-cb this :save key))

(defn trait-isRunImplemented [this]
  (boolean (get-in @registry [(.getName this) :run])))

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
  "
  [name callbacks]
  (let [name (str/lower-case name) ; Trait does the same, stay consistent
        init-fn-name (str "init-" name)
        classname (str *ns* "." name)]
    (swap! registry assoc name callbacks)
    ;; Make a unique `init` function for this class, so we have a way to inject
    ;; the correct name into the superclass constructor
    (intern *ns* (symbol (str "trait-" init-fn-name)) (fn [] [[name]]))
    (try
      (Class/forName classname)
      (catch java.lang.ClassNotFoundException _
        (gen-and-load-class
         {:name classname
          :extends Trait
          ;; :state "state"
          :init init-fn-name
          :constructors {[] [String]}
          ;;:post-init `my-post-init
          :prefix "trait-"
          ;;:impl-ns "my.trait.impl"
          :load-impl-ns false})))
    (let [klz (Class/forName classname)]
      (when-not (.getTraitClass (trait-factory) name)
        (.registerTrait (trait-factory)
                        (.withName (TraitInfo/create klz) name)))
      klz)))
