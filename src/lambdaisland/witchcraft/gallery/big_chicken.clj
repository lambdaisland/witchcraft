(ns lambdaisland.witchcraft.gallery.big-chicken
  "A 9 block high chicken shape"
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.shapes :as shapes]
            [lambdaisland.witchcraft.events :as e]
            [lambdaisland.witchcraft.palette :as p]
            [lambdaisland.witchcraft.matrix :as m]))

(def chicken
  (concat
   ;; body
   (shapes/box {:east-west-length 5
                :north-south-length 7
                :height 5
                :material :white-wool
                :start [0 0 0]})

   ;; wings
   (shapes/box {:east-west-length 7
                :north-south-length 4
                :height 3
                :material :white-wool
                :start [-1 2 1]})

   ;; head
   (shapes/box {:east-west-length 3
                :north-south-length 3
                :height 5
                :material :white-wool
                :start [1 4 -2]})

   ;; beak
   (shapes/box {:east-west-length 3
                :north-south-length 2
                :height 2
                :material :orange-wool
                :start [1 5 -4]})

   ;; eyes & red thingy
   [[1 7 -2 :black-wool]
    [3 7 -2 :black-wool]
    [2 4 -3 :red-wool]]

   ;; legs
   (shapes/line {:start [1 -4 2]
                 :end [1 1 2]
                 :material :orange-wool})
   (shapes/line {:start [3 -4 2]
                 :end [3 1 2]
                 :material :orange-wool})

   ;; feet
   [[1 -4 1 :orange-wool]
    [3 -4 1 :orange-wool]]))

(defn chicken-shape [pos]
  (m/with-origin pos chicken))

(comment
  (wc/set-blocks
   (chicken-shape
    (wc/target-block (wc/player))
    ))

  (wc/undo!))
