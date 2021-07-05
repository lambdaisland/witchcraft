(ns events
  (:require [lambdaisland.witchcraft :as wc]))

(wc/listen! :player-interact
            ::get-event
            (fn [e]
              (when (= (:right-click-block wc/block-actions) (:action e))
                (def event e))))

(:clickedBlock event)

(wc/set-block (wc/add (wc/location (:clickedBlock event)) {:y -1}) :acacia-door 8)
