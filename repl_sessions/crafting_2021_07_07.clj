(ns crafting-2021-07-07
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.cursor :as c]))

(wc/start! {:level-seed "apple"})

(def me (wc/player "sunnyplexus"))
(wc/location me)
[181.39333147179173 77.0 352.38498570989907 282.0 9.2999935 "world"]

;; ground
(wc/teleport [184 77 355])
(wc/fast-forward 5000)

;;overview
(wc/teleport me [190.97795265998212
                 90.78260120960923
                 371.66588346603123
                 145.20013
                 17.699974
                 "world"])

(defn draw-lamppost [cursor]
  (-> cursor
      (c/face :up)
      (c/palette {\f :iron-fence
                  \C :cobblestone
                  \# :concrete
                  \t :trap-door
                  \* :fire
                  \N :netherrack})
      (c/pattern "CfffN*")))

(defn excursion [cursor f & args]
  (assoc cursor :blocks (:blocks (apply f cursor args))))

(def lamppost (draw-lamppost (c/start [0 0 0])))

;; We can define material aliases via palette, these can be keywords but really
;; they can be anything
(-> (c/start [184 77 355])
    (c/palette {:foo :concrete})
    (c/material :foo)
    (c/step :east)
    (c/build))

;; Pattern drops multiple
(-> (c/start [184 77 355])
    (c/pattern [:concrete :cobblestone :fence])
    (c/build))



(doseq [x (range 175 185 3)
        y [77]
        z [356 352]]
  (-> lamppost
      (c/transpose [x y z]) c/build))

(doseq [x (range 175 230 7)
        y [77]
        z [355 351]]
  (-> lamppost (c/transpose (wc/highest-block-at [x y z])) c/build))

(def cursor
  (-> (c/start {})
      (c/palette {\f :iron-fence
                  \C :cobblestone
                  \# :concrete
                  \t :trap-door
                  \* :fire
                  \N :netherrack
                  \c :cyan-glazed-terracotta
                  \y :yellow-glazed-terracotta})))

(def pos (wc/in-front-of me))

(let [shape (-> (assoc cursor :x 2 :dir :east)
                (c/symmetry {:x 0}
                            {:z -3}
                            {:x 0 :z -3})
                (c/material :air #_:red-sandstone)
                (c/step :east)
                (c/material :air #_:cobblestone)
                (c/steps 2 :east
                         1 :south
                         3 :east
                         1 :north
                         1 :east
                         2 :north))]
  (c/build
   (c/transpose
    (update shape
            :blocks
            into (mapcat :blocks)
            [(-> shape (c/extrude 1 :up))
             (-> shape
                 (c/transpose [0 4 0])
                 (c/extrude 1 :down))
             (-> shape
                 (c/transpose [0 2 0])
                 (update :blocks (partial map #(assoc % :material :air #_:glass))))])
    [179 77 354])))


(wc/add-inventory me :cobblestone 20)

(defn duplicate-flip-x-z [cursor]
  (update cursor
          :blocks
          (fn [blocks]
            (into blocks
                  (map (fn [b]
                         (assoc b :x (:z b) :z (:x b))))
                  blocks))))

(def fort-walls
  (-> (assoc cursor :x 0 :dir :east)
      (c/material :cobblestone)
      (c/steps 2 :west
               2 :south
               3 :west
               1 :north
               3 :west
               2 :south
               5 :west
               2 :north
               11 :west)
      duplicate-flip-x-z))

(defn map-blocks [cursor f & args]
  (update cursor
          :blocks
          (fn [blocks]
            (into #{} (map #(apply f % args)) blocks))))

(-> fort-walls
    (c/extrude 8 :up)
    (map-blocks (fn [{:keys [x y z] :as block}]
                  (if (and (< -3 x 7)
                           (< 3 y 7)
                           (< -3 z 7)
                           )
                    (assoc block :material :glass)
                    block)))
    (c/transpose [188 77 350])
    c/build
    #_
    (c/flash! 3000))

fort-walls

(wc/add-inventory me :diamond-pickaxe)
(wc/add-inventory me :bed)

(wc/fast-forward 1000)

(wc/set-block me :bed)
(c/undo!)

(-> (c/start [0 0 0])
    (c/step)
    (c/step))

(defn highest-y [loc]
  (some #(when (not= :air (wc/material-name [(wc/x loc) % (wc/z loc)]))
           %)
        (range 100 60 -1)))

(defn lamppost-excursion [cursor]
  (excursion cursor
             #(as-> % $
                (c/steps $
                         3 :south
                         3 :east)
                (c/draw $)
                (assoc $ :y (highest-y (wc/add [(:x $) 0 (:z $)] [188 0 350])))
                (draw-lamppost $))))

(-> (c/start [0 0 0])
    (c/draw false)
    (c/steps 2 :west)
    (c/steps 2 :south)
    (c/steps 3 :west)
    lamppost-excursion
    (c/steps 1 :north)
    (c/steps 3 :west)
    (c/steps 2 :south)
    (c/steps 5 :west)
    lamppost-excursion
    (c/steps 2 :north)
    (c/steps 6 :west)
    lamppost-excursion
    (c/steps 5 :west)
    duplicate-flip-x-z
    (c/transpose [188 0 350])
    c/build)

(-> (c/start {})

    draw-lamppost    )

(c/redo!)

;; Show how nicely the direction works for terracotta and stairs
(-> (c/start [184 77 355])
    (c/face :east)
    (c/steps 4)
    (c/rotate 2)
    (c/steps 3)
    (c/rotate 2)
    (c/steps 3)
    (c/rotate 2)
    (c/steps 2)
    (c/rotate 2)
    (c/steps 2)
    (c/rotate 2)
    (c/step)
    (c/rotate 2)
    (c/step)
    )

(-> (c/start [184 77 355])
    (c/face :east)
    (c/n-times 4
               #(-> %
                    (c/material :wood)
                    (c/step)
                    (c/material :jungle-door)
                    (c/step)
                    (c/material :wood)
                    (c/step)
                    (c/rotate 2)))
    (c/build)
    )

(-> (c/start [186 94 421] #_[184 85 355])
    (assoc :cnt 1)
    (c/move 1 :north 1 :east)
    (c/material :jungle-wood-stairs)
    (c/symmetry-xz)
    (c/n-times 6 #(-> %
                      (c/steps 1 :east)
                      (c/steps (:cnt %) :north)
                      (c/move (:cnt %) :south)
                      (c/steps (:cnt %) :south)
                      (c/move (:cnt %) :north)
                      (c/move 1 :down)
                      (update :cnt inc)))
    (c/build)
    )
(-> (c/start [186 88 421] #_[184 85 355])
    (c/move 5 :east)
    (c/face :north)
    (c/material :cobblestone)
    (assoc :block-fn
           (fn [c]
             (let [v (c/block-value c)
                   v (if (#{:cobblestone :mossy-cobblestone} (:material c))
                       (assoc v :material
                              (if (< (rand-int 100) 20)
                                :mossy-cobblestone
                                :cobblestone))
                       v)]
               (update c :blocks conj v))))
    (c/symmetry-xz)
    (c/excursion #(c/steps % 2 :east 1 :north))
    (c/steps 4)
    (c/material :wood)
    (c/step)
    (c/matrices)
    (c/extrude 8 :down)

    (c/build)
    )
