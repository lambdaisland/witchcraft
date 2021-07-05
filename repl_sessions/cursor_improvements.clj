(ns cursor-improvements
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.cursor :as c]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

(def starting-point (c/start))

#_ starting-point
;; => {:dir :east,
;;     :x -77,
;;     :y 68,
;;     :z 85,
;;     :material :lapis-block,
;;     :draw? true,
;;     :blocks []}

(-> starting-point
    (c/material :wood)
    (c/steps 3)
    #_c/build)

(defn where-am-i [pos]
  (-> pos
      (c/block)
      (c/material :red-glazed-terracotta)
      (c/step)
      c/build)
  (future (Thread/sleep 1500)
          (c/undo!)))


;; the walls
(-> starting-point
    (c/material :wood)
    (c/steps 8) (c/rotate 2)
    (c/steps 11) (c/rotate 2)
    (c/steps 7) (c/rotate 2)
    (c/steps 5) (c/move 2) (c/steps 4)
    (c/extrude 2 :up)
    c/build)

;; the foundation
(-> starting-point
    (c/move 1 :down)
    (c/material :gravel)
    (c/steps 8) (c/extrude 11 :right)
    c/build)


(c/undo!)

;; left roof
(-> starting-point
    (c/move 3 :up)
    (c/move 1)
    (->> (iterate #(-> %
                       (c/material :acacia-stairs 1)
                       (c/block)

                       (c/move 1 :right)
                       (c/material :acacia-stairs 4)
                       (c/block)
                       (c/move 1 :up))))
    (nth 5)
    (c/extrude 7 :forward)
    c/build)

;; right roof
(-> starting-point
    (c/move 3 :up)
    (c/move 11 :right)
    (c/move 1)
    (->> (iterate #(-> %
                       (c/material :acacia-stairs 0)
                       (c/block)
                       (c/move 1 :left)
                       (c/material :acacia-stairs 5)
                       (c/block)
                       (c/move 1 :up))))
    (nth 5)
    (c/extrude 7 :forward)
    c/build
    )

(wc/add-inventory (wc/player) :acacia-door 8)

(c/undo!)

(-> starting-point
    (c/move 3 :up)
    (c/move 1 :right)
    (c/material :acacia-stairs 4)
    (c/steps 7)
    c/build
    #_where-am-i)
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
