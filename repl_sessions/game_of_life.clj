(ns game-of-life
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.bukkit :as bukkit :refer [entities materials]]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

#_ (wc/start!)
(fast-forward 5000)
(def size 50)

(def grid (atom (vec (for [x (range size)]
                       (vec (for [y (range size)]
                              (rand-nth [1 0])))))))

(defn neigbour-score [grid x y]
  (apply +
         (for [x' (range (dec x) (+ x 2))
               y' (range (dec y) (+ y 2))
               :when (not (and (= x x') (= y y')))
               :when (<= 0 x' (dec size))
               :when (<= 0 y' (dec size))]
           (get-in grid [x' y']))))


(defn next-generation [grid]
  (vec
   (for [x (range size)]
     (vec
      (for [y (range size)]
        (let [alive? (= 1 (get-in grid [x y]))
              score (neigbour-score grid x y)]
          (if (if alive?
                (#{2 3} score)
                (= 3 score))
            1 0)))))))

(def origin (offset (player-location 10)
                    [0 5 0]))

(swap! grid next-generation)

(defn draw [grid {:keys [blockX blockY blockZ] :as origin}]
  (set-blocks
   (for [x (range size)
         y (range size)]
     {:x (+ blockX x)
      :y (+ blockY y)
      :z blockZ
      :material (if (= 1 (get-in grid [x y]))
                  :stone
                  :air)})))

(defonce stop-gol! (atom false))
(future
  (while (not@stop-gol!)
    (draw (swap! grid next-generation) origin)))
(reset! stop-gol! true)

(draw @grid origin)
(fast-forward 4000)
