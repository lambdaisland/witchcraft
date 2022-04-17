(ns lambdaisland.witchcraft.citizens
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.events :as e]
            [lambdaisland.witchcraft.citizens.trait :as trait]
            [lambdaisland.witchcraft.reflect :as reflect]
            [lambdaisland.witchcraft.util :as util])
  (:import (net.citizensnpcs.api CitizensAPI)
           (net.citizensnpcs.api.ai Navigator)
           (net.citizensnpcs.api.trait Trait TraitFactory)
           (net.citizensnpcs.api.npc NPC NPCRegistry)
           (org.bukkit.entity EntityType)))

(set! *warn-on-reflection* true)

(defn npc-registry ^NPCRegistry []
  (CitizensAPI/getNPCRegistry))

(defn trait-factory ^TraitFactory []
  (CitizensAPI/getTraitFactory))

(defprotocol HasCreateNPC
  (-create-npc [_ e s]))

(defprotocol HasTraits
  (-get-or-add-trait [_ c])
  (-remove-trait [_ c])
  (-get-traits [_]))

(defprotocol HasNavigator
  (-navigator [_]))

(defprotocol HasNPC
  (-npc [_]))

(reflect/extend-signatures HasCreateNPC
  "createNPC(org.bukkit.entity.EntityType,java.lang.String)"
  (-create-npc [this entity-type npc-name]
    (.createNPC this entity-type npc-name)))

(reflect/extend-signatures HasTraits
  "net.citizensnpcs.api.trait.Trait getOrAddTrait(java.lang.Class)"
  (-get-or-add-trait [this klz]
    (.getOrAddTrait this klz))
  "void removeTrait(java.lang.Class)"
  (-remove-trait [this klz]
    (.removeTrait this klz))
  "java.lang.Iterable getTraits()"
  (-get-traits [this]
    (.getTraits this)))

(reflect/extend-signatures HasNavigator
  "net.citizensnpcs.api.ai.Navigator getNavigator()"
  (-navigator [this]
    (.getNavigator this)))

(reflect/extend-signatures HasNPC
  "net.citizensnpcs.api.npc.NPC getNPC()"
  (-npc [this]
    (.getNPC this)))

(defmethod trait/pre-save java.util.UUID [^java.util.UUID uuid]
  {:witchcraft/tag "uuid"
   :value (.toString uuid)})

(defmethod trait/post-load "uuid" [{:keys [value]}]
  (java.util.UUID/fromString value))

(defmethod trait/pre-save org.bukkit.Location [loc]
  {:witchcraft/tag "bukkit/location"
   :value (wc/loc loc)})

(defmethod trait/post-load "bukkit/location" [{:keys [value]}]
  (wc/location value))

(defmethod trait/pre-save org.bukkit.entity.Entity [entity]
  {:witchcraft/tag "bukkit/entity"
   :value (str (wc/-uuid entity))})

(defmethod trait/post-load "bukkit/entity" [{:keys [value]}]
  (wc/entity (java.util.UUID/fromString value)))

(defmethod trait/pre-save org.bukkit.inventory.Inventory [inv]
  {:witchcraft/tag "bukkit/inventory"
   :value (wc/inventory inv)})

(defmethod trait/post-load "bukkit/inventory" [{:keys [value]}]
  (wc/make-inventory value))

(defmethod trait/pre-save clojure.lang.Keyword [kw]
  {:witchcraft/tag "clojure/keyword"
   :value (str (symbol kw))})

(defmethod trait/post-load "clojure/keyword" [{:keys [value]}]
  (keyword value))

(defn create-npc [entity-type npc-name]
  (-create-npc (npc-registry)
               (get wc/entity-types entity-type)
               npc-name))

(defn npc ^NPC [o]
  (cond
    (instance? NPC o)
    o

    (satisfies? HasNPC o)
    (-npc o)))

(defn make-trait
  {:doc (:doc (meta #'trait/make-trait))}
  ^Class [name callbacks]
  (trait/make-trait name callbacks))

(defn trait-class
  [trait-name]
  (.getTraitClass (trait-factory) (name trait-name)))

(defn npc-trait
  "Get the trait with the given name for the given NPC, adds it if it hasn't been
  added to the NPC already, and returns the Trait instance."
  [npc trait-name]
  (-get-or-add-trait npc (trait-class trait-name)))

(defn npc-by-id [id]
  (.getById (npc-registry) id))

(defn trait-names [npc]
  (map #(keyword (.getName ^Trait %)) (-get-traits npc)))

(defn traits [npc]
  (-get-traits npc))

(defn has-trait? [npc trait]
  (some #{trait} (trait-names npc)))

(defn navigator ^Navigator [o]
  (cond
    (instance? Navigator o)
    o

    (satisfies? HasNavigator o)
    (-navigator o)

    (satisfies? HasNPC o)
    (-navigator (-npc o))))

(defn navigate-to [npc loc]
  (.setTarget
   (navigator npc)
   (wc/location loc)))

(defn stop-navigating [npc]
  (.cancelNavigation (navigator npc)))

(defn update-traits [npc traits]
  (doseq [[name state] traits]
    (let [t (npc-trait npc name)]
      (when (instance? clojure.lang.IAtom t)
        (swap! t merge state)))))

(defn remove-trait [npc trait]
  (-remove-trait npc (trait-class trait)))

(defn spawned? [the-npc]
  (if (instance? NPC npc)
    (.isSpawned ^NPC npc)
    (.isSpawned (npc the-npc))))

(defn despawn [the-npc]
  (if (instance? NPC npc)
    (.despawn ^NPC npc)
    (.despawn (npc the-npc))))

(defn destroy [the-npc]
  (if (instance? NPC npc)
    (.destroy ^NPC npc)
    (.destroy (npc the-npc))))

(comment
  (trait-class :inventory)
  (def jonny (npc-by-id 0) #_(create-npc :player "jonny"))

  (def me (wc/player "sunnyplexus"))

  (make-trait "fishing"
              {:on-attach (fn [this] (println "Fishing attached!"))
               :on-spawn (fn [this] (println "Fisher spawned!"))
               :run (fn [this])})

  (npc-trait jonny "fishing")

  (wc/inventory
   (.getEntity jonny))

  (wc/inventory me)
  (clojure.reflect/reflect me)
  (wc/add-inventory me :diamond-axe)
  (dotimes [i 1000]
    (wc/spawn (wc/add (wc/target-block me) [(/ (rand-int 300) 100)
                                            (/ (rand-int 300) 100)
                                            (/ (rand-int 300) 100)]) :experience-orb)
    )

  (.setLevel me 30)
  (.giveExpLevels me 1)
  (wc/en)

  (wc/set-time 0)

  (wc/set-game-rule (wc/world "world")
                    :do-daylight-cycle
                    false)

  (wc/spawn [0 0 0] jonny)

  (.setTarget
   (navigator (npc-by-id 0))
   (wc/add (wc/location me) [-5 0 -5]))

  (make-trait :test-trait2 {:init {:foo 123}})

  (.openInventory me
                  (org.bukkit.Bukkit/createInventory nil 9 "Testing inventory"))

  (npc-trait (npc-by-id 0)
             :equipment)

  (.saveToStore (npc-registry)))
