(ns lambdaisland.witchcraft.cursor
  (:require [lambdaisland.witchcraft.bukkit :as bukkit]))

(def directions
  [:north-west
   ;;:north-north-west
   :north
   ;;:north-north-east
   :north-east
   ;;:east-north-east
   :east
   ;;:east-south-east
   :south-east
   ;;:south-south-east
   :south
   ;;:south-south-west
   :south-west
   ;;:west-south-west
   :west
   ;;:west-north-west
   ])

(def movements
  {:north-west [1 0 -1]
   :north      [1 0 0]
   :north-east [1 0 1]
   :east       [0 0 1]
   :south-east [-1 0 1]
   :south      [-1 0 0]
   :south-west [-1 0 -1]
   :west       [0 0 -1]
   :up         [0 1 0]
   :down       [0 -1 0]})

(defn rotate-dir [dir n]
  (if (#{:up :down} dir)
    (if (#{3 4 5} (mod n 8))
      ({:up :down :down :up} dir)
      dir)
    (nth (drop-while (complement #{dir}) (cycle directions)) n)))

(defn rotate [cursor n]
  (update cursor :dir rotate-dir n))

(defn up [cursor]
  (update cursor :y inc))

(defn down [cursor]
  (assoc cursor :y dec))

(defn direction [cursor dir]
  (assoc cursor :dir dir))

(defn step [cursor]
  (let [[x y z] (get movements (:dir cursor))]
    (-> cursor
        (update :x + x)
        (update :y + y)
        (update :z + z))))

(comment
  (-> {:x 1 :y 1 :z 1 :dir :east}
      (rotate 4)
      step
      step
      step
      (rotate 2)
      step))
