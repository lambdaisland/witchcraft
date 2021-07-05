(ns bukkit-tutorial
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.util :as util]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]])
  (:import (net.glowstone.entity GlowLivingEntity)))

;; https://dev.bukkit.org/projects/controllable-mobs-api/pages/tutorials/tutorial-1-the-basics

;; Spawn a zombie when right clicking on a block while holding a piece of rotten
;; flesh, and modify the behavior of the zombie

;; Some issue which may be GlowStone bugs:
;; - we're receiving two PlayerInteractionEvents on every click
;; - even though the zombies have "follow player" as their sole task, every a
;;   few moments they just start sprinting away really really fast

(wc/start! {:level-type "FLAT"})

(def me (wc/player "sunnyplexus"))

(wc/inventory-add me :rotten-flesh)
(wc/inventory-add me :iron-axe)

(wc/item-in-hand-type me)
(wc/item-in-hand-count me)
(wc/fast-forward 3000)

(wc/listen! :player-interact
            ::spawn-zombie-from-flesh
            (fn [e]
              (when (= (:right-click-block wc/block-actions) (:action e))
                (wc/inventory-remove me :rotten-flesh)
                (let [zombie (some-> e
                                     :clickedBlock
                                     wc/location
                                     (wc/offset [0 1 0])
                                     (wc/spawn :zombie))
                      task-manager(.get (util/accessible-field GlowLivingEntity "taskManager") zombie)]
                  (.cancelTasks task-manager)
                  (.addTask task-manager (proxy [net.glowstone.entity.ai.FollowPlayerTask] []
                                           (shouldStart [^GlowLivingEntity _] true)))))))

(wc/listen! :entity-add-to-world
            ::on-command
            (fn [e] (def xxx (:entity e))))


;; clean up
;; (run! #(.remove %) (wc/nearby-entities me 100 100 100))
