(ns cursor-improvements
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.cursor :as c]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

(def starting-point (c/start))

(-> starting-point
    (c/material :wood)
    (c/draw)
    (c/steps 3)
    :blocks
    set-blocks)

#_ starting-point
;; => {:dir :east,
;;     :x -77,
;;     :y 68,
;;     :z 85,
;;     :material :lapis-block,
;;     :draw? false,
;;     :blocks []}

;; the walls
(-> starting-point
    c/draw
    (c/material :wood)
    (c/steps 8)
    (c/rotate 2)
    (c/steps 10)
    (c/rotate 2)
    (c/steps 8)
    (c/rotate 2)
    (c/steps 4)
    (c/material :air)
    (c/steps 2)
    (c/material :wood)
    (c/steps 4)
    :blocks
    set-blocks
    )

;; the foundation
(-> starting-point
    (c/step :down)
    (c/material :cobblestone)
    (c/steps 8)
    (c/rotate 2)
    c/draw
    (c/steps 10)
    (c/extrude 12 :west)
    :blocks
    set-blocks)

;; the roof
(loop [c (-> starting-point
             (c/draw false)
             (c/steps 2 :up)
             (c/step :north-west)
             (c/material :red-glazed-terracotta)
             (c/draw))
       x-steps 10
       y-steps 12]
  (println x-steps y-steps)
  (if (< 0 y-steps)
    (-> c
        (c/steps x-steps)
        (c/rotate 2)
        (c/steps y-steps)
        (c/rotate 2)
        (c/steps x-steps)
        (c/rotate 2)
        (c/steps y-steps)
        (c/rotate 2)
        (c/draw false)
        (c/step :up)
        (c/step :south-east) ;; modify this based on where you are facing
        (c/draw true)
        (recur (- x-steps 2)
               (- y-steps 2)))
    (set-blocks (:blocks c))))

;; empty out the inside
(dotimes [i 7]
  (-> starting-point
      (c/steps (inc i))
      (c/rotate 2)
      (c/step)
      (c/material :air)
      (c/draw)
      (c/steps 8)
      (c/extrude 3)
      :blocks
      set-blocks))

(-> starting-point
    (c/rotate 2)
    (c/material :wood)
    (c/draw)
    (c/steps 3)
    (c/material :air)
    (c/steps 2)
    (c/material :wood)
    (c/steps 3)
    :blocks
    set-blocks)

(-> (c/start)
    (c/draw)
    (c/material :red-glazed-terracotta)
    (c/steps 3)
    (c/rotate 2)
    (c/material :blue-glazed-terracotta)
    (c/steps 3)
    (c/rotate 2)
    (c/material :green-glazed-terracotta)
    (c/steps 3)
    (c/rotate 2)
    (c/material :yellow-glazed-terracotta)
    (c/steps 3)
    (c/build))
