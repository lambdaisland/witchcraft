(ns lambdaisland.witchcraft.cursor
  "Logo-like API for drawing blocks by using a walking metaphor.

  This is a functional API, movements simply build up a list of block positions,
  call [[build]] at the end to actually make them materialize in the game.

  A cursor contains a current location (x/y/z), a
  directions (:north, :north-east, :east, etc.), and a current building
  material, e.g :lapis-block (see [[lambdaisland.witchcraft.bukkit/materials]]).

  It also contains a drawing flag `:draw?` and a list of blocks `:blocks`. When
  drawing is on, then any step will add a block to the list. [[build]] creates
  blocks in the world based on this, and resets the list.

  Call [[start]] to get an initial cursor, this will return a cursor that is one
  step ahead of the player (so what you draw is in sight), facing away from the
  player.

  ```
  (require '[lambdaisland.witchcraft.cursor :as c])

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
      (c/build)
      )
  ```
  "
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft.bukkit :as bukkit]
            [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

#_bukkit/materials
(def default-material :lapis-block)

(defonce undo-history (atom ()))
(defonce redo-history (atom ()))

(def directions
  [:south
   :south-west
   :west
   :north-west
   :north
   :north-east
   :east
   :south-east])

(def movements
  {:south      [0 0 1],
   :south-west [-1 0 1],
   :west       [-1 0 0],
   :north-west [-1 0 -1],
   :north      [0 0 -1],
   :north-east [1 0 -1],
   :east       [1 0 0],
   :south-east [1 0 1]
   :up         [0 1 0]
   :down       [0 -1 0]})

(declare step)

(defn start
  "Creates a new cursor, starting one block ahead of the player."
  []
  (let [player-loc (bean (wc/player-location))]
    (step {:dir (nth directions (-> player-loc
                                    :yaw
                                    (/ 45)
                                    Math/round
                                    (mod (count directions))))
           :x (Math/round (:x player-loc))
           :y (Math/round (:y player-loc))
           :z (Math/round (:z player-loc))
           :material default-material
           :draw? false
           :blocks #{}})))

(defn draw
  "Enable/disable drawing. Enables by default, pass false to disable."
  ([c]
   (draw c true))
  ([c draw?]
   (assoc c :draw? draw?)))

(defn block-value [cursor]
  (assoc (select-keys cursor [:x :y :z :material])
         :data (:material-data cursor)))

(defn block
  "Add a block to the block list based on the cursor location."
  [cursor]
  (update cursor :blocks conj (block-value cursor)))

(defn ?block
  "Add a block to the block list based on the current cursor location and
  material, but only if drawing is enabled. (pronounced \"maybe block\")"
  [c]
  (if (:draw? c)
    (block c)
    c))

(defn material
  "Set the current cursor material, and optionally material-data, to be used for
  consecutive blocks."
  ([cursor m]
   (assoc cursor :material m :material-data nil))
  ([cursor m md]
   (assoc cursor :material m :material-data md)))

(defn rotate-dir
  "Given a direction keyword like :north or :south and a number, make that many
  1/8 turns clockwise.

  (rotate :north 4) ;; => :south"
  [dir n]
  (if (some #{dir} directions)
    (nth (drop-while (complement #{dir}) (cycle directions)) (mod n 8))
    dir))

(defn rotate
  "Rotate the cursor clockwise by a number of 1/8 turns clockwise."
  [{:keys [dir xy-dir] :as cursor} n]
  (let [n-even (mod (+ n (mod n 2)) 8)]
    (assoc
      cursor :dir
      (cond
        (= dir :up)
        (case n-even
          0 :up
          2 (rotate-dir xy-dir 2)
          4 :down
          6 (rotate-dir xy-dir 6))
        (= dir :down)
        (case n-even
          0 :down
          2 (rotate-dir xy-dir 6)
          4 :up
          6 (rotate-dir xy-dir 2))

        :else
        (rotate-dir dir n)))))

(defn face
  "Face the cursor in a certain direction"
  [cursor dir]
  (if (not= dir (:dir cursor))
    (assoc cursor
           :dir dir
           :xy-dir (cond
                     ;; save the direction where the feet are pointing when going
                     ;; up/down, important for rotate later on
                     (= :up dir) (:dir cursor)
                     (= :down dir) (rotate-dir (:dir cursor) 4)))
    cursor))

(defn- step* [m dir]
  (assert (keyword? dir) (str "Direction must be a keyword, got " dir))
  (assert (contains? movements dir) (str "Unknown direction " dir ", should be one-of "
                                         (keys movements)))
  (let [[x y z] (get movements dir)]
    (-> m
        (update :x + x)
        (update :y + y)
        (update :z + z))))

(defn step
  "Take one step forward in the direction given, or the direction the cursor is
  facing. If drawing is enabled this will also add a block to the block list
  corresponding with the new location."
  ([cursor]
   (step cursor (:dir cursor)))
  ([cursor dir]
   (?block (step* cursor dir))))

(defn steps
  "Take n steps forward as with [[step]]"
  ([cursor n]
   (steps cursor n (:dir cursor)))
  ([cursor n dir]
   (if (< n 0)
     (steps cursor (- n) (rotate-dir dir 4))
     (nth (iterate #(step % dir) cursor) n))))

(defn move
  "Move the cursor as with steps, but without drawing."
  ([{:keys [draw? dir] :as cursor} n]
   (move cursor n dir))
  ([{:keys [draw?] :as cursor} n dir]
   (assert (int? n) (str "Move takes a number of steps, got " n))
   (-> cursor
       (draw false)
       (steps n dir)
       (draw draw?))))

(defn move-to
  "Move the cursor to the given location, does not draw."
  [cursor loc]
  (merge cursor (select-keys (bean loc) [:x :y :z])))

(defn excursion
  "Apply a block-drawing function f, then return to the original position."
  [cursor f]
  (assoc cursor :blocks (:blocks (f cursor))))

(defn extrude
  "Take the current block list and extrude it in a given direction, by default
  up."
  ([cursor n]
   (extrude cursor n :up))
  ([{:keys [material material-data] :as cursor } n dir]
   (update cursor
           :blocks
           (fn [blocks]
             (reduce (fn [res i]
                       (into res
                             (map (fn [b]
                                    (nth (iterate #(step* % dir) (assoc b
                                                                        :material material
                                                                        :material-data material-data)) i)))
                             blocks))
                     blocks
                     (range 1 (inc n)))))))

(def material->keyword (into {} (map (juxt val key)) bukkit/materials))

(defn- lookup-block [block]
  (let [{:keys [x y z type state]} (bean (wc/get-block block))]
    {:x x
     :y y
     :z z
     :material (material->keyword type)
     :data (.getData (.getData state))}))

(defn build
  "Apply the list of blocks in the cursor to the world."
  [{:keys [blocks] :as cursor}]
  (swap! undo-history conj (doall (map lookup-block blocks)))
  (wc/set-blocks blocks)
  (assoc cursor :blocks #{}))

(defn undo!
  "Undo the last build. Can be repeated to undo multiple builds."
  []
  (swap! undo-history (fn [[blocks & rest]]
                        (when blocks
                          (wc/set-blocks blocks)
                          (swap! redo-history conj blocks))
                        rest))
  :undo)

(defn redo! []
  "Redo the last build that was undone with [[undo!]]"
  (swap! redo-history (fn [[blocks & rest]]
                        (when blocks
                          (wc/set-blocks blocks)
                          (swap! undo-history conj blocks))
                        rest))
  :redo)

(comment
  (-> {:x 1 :y 1 :z 1 :dir :east}
      (rotate 4)
      step
      step
      step
      (rotate 2)
      step))
