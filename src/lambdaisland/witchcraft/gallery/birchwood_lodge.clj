(ns lambdaisland.witchcraft.gallery.birchwood-lodge
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.palette :as palette]
            [lambdaisland.witchcraft.fill :as fill]
            [lambdaisland.witchcraft.matrix :as m]
            [lambdaisland.witchcraft.shapes :as shapes]
            [lambdaisland.witchcraft.markup :as markup]
            [lambdaisland.witchcraft.cursor :as cursor]))

(defn roof-segment [cursor length]
  (-> cursor
      (cursor/excursion
       #(-> %
            (cursor/material :birch-planks)
            (cursor/steps 2)
            (cursor/material :cobbled-deepslate-slab {:type :top})
            (cursor/steps length)
            (cursor/material :birch-planks)
            (cursor/steps 2)))

      (cursor/rotate 2)
      (cursor/move 1 :forward)
      (cursor/rotate -2)

      (cursor/excursion
       #(-> %
            (cursor/rotation 6)
            (cursor/material :birch-stairs)
            (cursor/steps 2)
            (cursor/material :cobbled-deepslate-slab {:type :bottom})
            (cursor/steps length)
            (cursor/material :birch-stairs)
            (cursor/steps 2)))

      (cursor/rotate 2)
      (cursor/move 1 :forward 1 :down)
      (cursor/rotate -2)))

(defn half-roof [cursor {:keys [length sections]}]
  (-> cursor
      (cursor/reps sections roof-segment length)
      (cursor/move 1 :up)
      (cursor/material :birch-slab {:type :bottom})
      (cursor/steps (+ length 4))))

(defn roof-triangle [cursor {:keys [sections] :as opts}]
  (-> (reduce
       (fn [c sect]
         (cursor/excursion c #(-> %
                                  (cursor/move (dec (* sect 2)) :left
                                               sect :down
                                               1 :forward)
                                  (cursor/rotate 2)
                                  (cursor/steps (dec (* sect 4))))))
       (cursor/material cursor :dark-oak-wood)
       (range 1 sections))))

(defn roof [cursor {:keys [length sections] :as opts}]
  (-> cursor
      (cursor/excursion roof-triangle opts)
      (cursor/excursion half-roof opts)
      (cursor/move (+ length 3) :forward)
      (cursor/rotate 4)
      (cursor/excursion roof-triangle opts)
      (cursor/excursion half-roof opts)))


(defn lodge-section [{:keys [height depth width direction]}]
  (concat
   (-> (cursor/start [0 (dec (+ height width)) 0] direction)
       (cursor/move 2 :backward)
       (roof {:length depth :sections width})
       :blocks)
   (let [dx -1
         dz (- (- (* width 2) 2))
         l (- (* width 4) 3)
         w (+ 2 depth)
         [dx dz l w] (if (#{:east :west} direction)
                       [dx dz l w]
                       [dz dx w l])]
     (shapes/rectube {:material :stripped-birch-wood
                      :start [dx 0 dz]
                      :length l
                      :width w
                      :height height
                      }))))

(defn lantern-bar-gen [{:keys [chain-width chain-length
                               bar-width
                               axis
                               lanterns]
                        :or {axis :x}}]
  (let [half-chain (quot chain-width 2)
        half-bar (quot bar-width 2)
        spacing (/ half-bar (quot lanterns 2))
        blk (fn [x y z & args]
              (into [(if (= axis :x) x z)
                     y
                     (if (= axis :x) z x)]
                    args))]
    (concat
     (for [y (range chain-length)
           x [(- half-chain) (cond-> half-chain (even? chain-width) dec)]]
       (blk x (- y) 0 :chain))

     (for [x (range (- half-bar) (cond-> half-bar (odd? bar-width) inc))]
       (blk x (- chain-length) 0 :dark-oak-log {:axis axis}))

     (when (odd? lanterns)
       [(blk 0 (- (inc chain-length)) 0 :lantern {:hanging true})])

     (for [lnt (range (quot lanterns 2))
           fact [1 -1]]
       (blk (* fact (Math/ceil (* spacing (+ 0.5 lnt))))
            (- (inc chain-length))
            0
            :lantern
            {:hanging true})))))


(comment
  (let [anchor (wc/target-block (wc/player))]

    (prn anchor)
    (-> (lodge-section {:height 10 :depth 16 :width 4 :direction :east})
        (wc/set-blocks {:start (wc/add anchor [5 0 6])}))
    (-> (lodge-section {:height 10 :depth 10 :width 4 :direction :east})
        (wc/set-blocks {:start (wc/add anchor [-14 0 6])}))
    (-> (lodge-section {:height 17 :depth 13 :width 3 :direction :south})
        (wc/set-blocks {:start anchor}))

    (wc/set-blocks
     (map #(assoc (dissoc (wc/block %) :block-data) :material :oak-planks)
          (concat
           (fill/fill-xz (wc/add anchor [6 0 7])
                         {:pred #(not= :stripped-birch-wood (wc/mat %))
                          })
           (fill/fill-xz (wc/add anchor [-13 0 6])
                         {:pred #(not= :stripped-birch-wood (wc/mat %))
                          })
           (fill/fill-xz (wc/add anchor [1 0 0])
                         {:pred #(not= :stripped-birch-wood (wc/mat %))
                          }))))

    (wc/set-blocks (lantern-bar-gen {:chain-width 5
                                     :bar-width 9
                                     :lanterns 3
                                     :chain-length 5
                                     :axis :x})
                   {:start
                    (wc/add [12 12 6]
                            anchor)}

                   )
    )



  (wc/undo!)

  )

(wc/xyz-round (wc/target-block (wc/player)))
(m/v-
 [-16341 163 -17240]
 [-16351 150 -17246]
 )

(def anchor  [-16351 150 -17246])
