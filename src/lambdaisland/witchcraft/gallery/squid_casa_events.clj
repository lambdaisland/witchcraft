(ns lambdaisland.witchcraft.gallery.squid-casa-events
  "Event handlers that hook up two buttons to beam the player from one to the
  other."
  (:require [lambdaisland.witchcraft.events :as e]
            [lambdaisland.witchcraft :as wc]))

(e/listen! :player-interact
           ::beam-me-up
           (fn [{:keys [clickedBlock player action]}]
             (when (and clickedBlock
                        (= :stone-button (wc/material-name clickedBlock))
                        (= :right-click-block action))
               (case (wc/xyz clickedBlock)
                 ;; up
                 [689.0 66.0 -846.0]
                 (wc/teleport player {:x 696
                                      :y 104.5
                                      :z -833.3
                                      :pitch -3.15
                                      :yaw -179})
                 ;; down
                 [695.0 105.0 -834.0]
                 (wc/teleport player {:x 690.4144080426516
                                      :y 65.5
                                      :z -844.6999999880791
                                      :pitch -2.6994934
                                      :yaw 176.85172})
                 nil))))
