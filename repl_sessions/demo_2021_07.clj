(ns repl-sessions.demo-2021-07
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.cursor :as c]
            [lambdaisland.witchcraft.events :as e]))

(set! *warn-on-reflection* true)

;; First start the server, if you want a clean slate you can create
;; a "superflat" world, otherwise you'll get a more typically generated world.
(wc/start! {;;:server-port 4567
            ;;:config-dir "/tmp/config"
            :level-type "FLAT"})

;; Now connect with Minecraft 1.12 by adding a new server with address
;; 127.0.0.1:25565

;; It can be annoying if it gets dark or starts raining while you're working on
;; stuff.

(wc/fast-forward 5000)
(wc/clear-weather)

;; A lot of functions implicitly try to find the first player they find, but
;; better to be explicit, especially when you plan to be with multiple people on
;; the same server.

(def me (wc/player "sunnyplexus"))

;; Let's see where we are
(wc/location me)
(wc/world me)
(wc/server)
(wc/direction me)

;; let's move us around a bit
(wc/teleport me (wc/add (wc/location me) {:x 3}))
(wc/teleport me (wc/add (wc/location me) {:yaw 5 :pitch -5 :z 1 :x 1}))

;; Let's create some blocks
(wc/set-block (wc/in-front-of me) :water)

;; Let's just teleport to the center of the world so we can work with
;; predictable numbers

(wc/teleport me {:x 0 :z 0})

(doseq [x (range 0 10)
        y (range 4 7)
        z (range 0 10)]
  (wc/set-block {:x x :y y :z z} :glass))

(doseq [x (range 0 10)
        y (range 4 7)
        z (range 0 10)]
  (wc/set-block {:x x :y y :z z}
                (if (= y 6)
                  :wood
                  (if (and (< 0 x 9) (< 0 z 9))
                    :air
                    :stone))))

(wc/add-inventory me :iron-pickaxe)
(wc/add-inventory me :iron-axe)

(c/start me)

(def events (atom []))
@events
(wc/listen! :player-interact
            ::get-event
            (fn [e]
              (wc/send-message (:player e) "Good job!")))

(keys e/events)

;; Clean up

(doseq [x (range -100 200)
        y (range 3 20)
        z (range -100 200)]
  (wc/set-block {:x x :y y :z z} (if (= 3 y)
                                   :grass :air)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cursor

(def starting-point (c/start))

(-> starting-point
    (c/material :wood)
    (c/steps 3)
    c/build)

(c/undo!)

(defn where-am-i [pos]
  (-> pos
      (c/block)
      (c/material :red-glazed-terracotta)
      (c/step)
      c/build)
  (future (Thread/sleep 1500)
          (c/undo!)))

(where-am-i starting-point)

;; the foundation
(-> starting-point
    (c/move 1 :down)
    (c/material :gravel)
    (c/steps 8) (c/extrude 11 :right)
    c/build)

;; the walls
(-> starting-point
    (c/material :wood)
    (c/steps 8) (c/rotate 2)
    ;; (c/steps 11) (c/rotate 2)
    ;; (c/steps 7) (c/rotate 2)
    ;; (c/steps 5) (c/move 2) (c/steps 4)
    ;; (c/extrude 2 :up)
    c/build)

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
    c/build)

(wc/add-inventory (wc/player) :acacia-door 2)
