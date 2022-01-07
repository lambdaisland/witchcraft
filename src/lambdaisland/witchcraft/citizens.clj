(ns lambdaisland.witchcraft.citizens
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.events :as e]
            [lambdaisland.witchcraft.citizens.trait :as trait]
            [lambdaisland.witchcraft.reflect :as reflect]
            [lambdaisland.witchcraft.util :as util])
  (:import (net.citizensnpcs Citizens)
           (net.citizensnpcs.api CitizensAPI)
           (net.citizensnpcs.api.trait Trait TraitFactory)
           (net.citizensnpcs.api.npc NPCRegistry)
           (org.bukkit.entity EntityType)))

(set! *warn-on-reflection* true)

(defn npc-registry ^NPCRegistry []
  (CitizensAPI/getNPCRegistry))

(defn trait-factory ^TraitFactory []
  (CitizensAPI/getTraitFactory))

(defprotocol HasCreateNPC
  (-create-npc [_ e s]))

(defprotocol HasTraits
  (-get-or-add-trait [_ c]))

(reflect/extend-signatures HasCreateNPC
  "createNPC(org.bukkit.entity.EntityType,java.lang.String)"
  (-create-npc [this entity-type npc-name]
    (.createNPC this entity-type npc-name)))

(reflect/extend-signatures HasTraits
  "net.citizensnpcs.api.trait.Trait getOrAddTrait(java.lang.Class)"
  (-get-or-add-trait [this klz]
    (.getOrAddTrait this klz)))

(defn create-npc [entity-type npc-name]
  (-create-npc (npc-registry)
               (get wc/entity-types entity-type)
               npc-name))

(defn make-trait
  {:doc (:doc (meta #'trait/make-trait))}
  ^Class [name callbacks]
  (trait/make-trait name callbacks))

(defn trait-class
  [name]
  (.getTraitClass (trait-factory) name))

(defn npc-trait
  "Get the trait with the given name for the given NPC, adds it if it hasn't been
  added to the NPC already, and returns the Trait instance."
  [npc trait-name]
  (-get-or-add-trait npc (trait-class trait-name)))

(defn npc-by-id [id]
  (.getById (npc-registry) id))

(comment

  (def jonny (create-npc :player "jonny"))

  (def me (wc/player "sunnyplexus"))

  (make-trait "fishing"
              {:on-attach (fn [this] (println "Fishing attached!"))
               :on-spawn (fn [this] (println "Fisher spawned!"))
               :run (fn [this])})

  (npc-trait jonny "fishing")

  (wc/set-time 0)

  (wc/spawn [0 0 0] jonny)


  (make-trait "test-trait2" {:init {:foo 123}})

  @(npc-trait (npc-by-id 0)
              "test-trait2"))
