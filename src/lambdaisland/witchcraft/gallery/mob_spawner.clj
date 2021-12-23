(ns lambdaisland.witchcraft.gallery.mob-spawner
  "'Classic' mob-spawner, plus a varient with multiple levels. For best results
  put it really high up in the sky, or above the open ocean. There should be
  little to no other places nearby (like caves beneath you) where mobs could
  spawn."
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.shapes :as s]
            [lambdaisland.witchcraft.cursor :as c]))

(def palette
  {::fall-base    :stone-slab
   ::wall         :tinted-glass
   ::chute-wall :tinted-glass
   ::floor        :tinted-glass
   ::planks       :acacia-planks
   ::slab         :acacia-slab
   ::trapdoor     :acacia-trapdoor
   ::ceiling      :tinted-glass
   ::gutter       :tinted-glass})

(defn platform
  "An optional platform, for if you're building this high up in the sky"
  []
  (for [x (range -9 9)
        y [-1]
        z (range -9 9)]
    [x y z ::planks]))

(defn chests-and-hoppers
  "Two big chests with hoppers above them, and slabs on top. This is where the
  mobs fall and you then kill them, the hoppers collect the loot."
  []
  (for [x [1 2]
        z [1 2]
        [y mat data] [[0 :chest {:facing :south :type ({1 :right 2 :left} x)}]
                      [1 :hopper {:facing :down}]
                      [2 ::fall-base {}]]]
    [x y z mat data]))

(defn chute-and-trapdoors
  "The chute that mobs will fall down, just a pipe basically with a 2x2 opening.
  At the bottom of it we put trapdoors so the mobs can't see us when we're
  standing underneath."
  [height]
  (concat
   (s/rectube {:width 4 :length 4 :height height :start [0 3 0]
               :material ::chute-wall})
   (s/rectube {:width 4 :length 4 :height 1 :start [0 2 0]
               :material [::trapdoor {:half :top}]})))

(defn gutter
  "The spawner itself consists of four quadrants of flooring, with in between four
  of these gutters. Inside flows water to the center to guide the mobs to the
  chute, and the sides are lined with open trapdoors, which causes them to fall
  in."
  [loc dir length]
  (let [cross-dir (c/rotate-dir dir 2)]
    (concat
     (-> (c/start loc cross-dir)
         (c/material [::trapdoor {:half :top :open true}])
         (c/excursion (fn [c]
                        (-> c
                            (c/move 1 :forward)
                            (c/steps 1 :forward)
                            (c/move 2 :forward)
                            (c/steps 1 :backward))))
         (c/material ::gutter)
         (c/steps 3 :down
                  3 cross-dir
                  2 :up)
         (c/extrude (dec length) dir)
         (c/blocks))
     (-> (c/start loc dir)
         (c/material ::gutter)
         (c/move length :forward 1 :right)
         (c/steps 2 :right)
         (c/extrude 1 :down)
         (c/blocks))
     (-> (c/start loc dir)
         (c/material :water)
         (c/move (dec length) :forward 1 :down 1 :right)
         (c/steps 2 :right)
         (c/blocks)))))

(defn half-gutter
  "Variant of the gutter, basically just the top row of blocks and trapdoors. Used
  on the upper levels of a multi-level spawner. Mobs fall into the lowest gutter
  and flow from there."
  [loc dir length]
  (let [cross-dir (c/rotate-dir dir 2)]
    (concat
     (-> (c/start loc cross-dir)
         (c/material [::trapdoor {:half :top :open true}])
         (c/excursion (fn [c]
                        (-> c
                            (c/move 1 :forward)
                            (c/steps 1 :forward)
                            (c/move 2 :forward)
                            (c/steps 1 :backward))))
         (c/material ::planks)
         (c/excursion c/steps 1 :down)
         (c/move 3 cross-dir)
         (c/excursion c/steps 1 :down)
         (c/extrude length dir)
         (c/blocks)))))

(defn floor
  "A single quadrant of flooring. We put a grid of trapdoors on top to prevent
  spiders from spawning (they need a 2x2 open area)."
  [loc dir]
  (concat
   (-> (c/start loc dir)
       (c/material ::floor)
       (c/steps 7 :forward)
       (c/extrude 6 :right)
       (c/blocks))
   (c/blocks
    (reduce
     (fn [c [x z]]
       (c/excursion
        c
        #(-> %
             (c/move (* x 2) :forward (* z 2) :right)
             (c/step))))
     (-> (c/start loc dir)
         (c/move 1 :up)
         (c/material ::trapdoor))
     (for [x (range 4) z (range 4)] [x z])))))

(defn gutters
  "All four gutters, at the given height"
  [height]
  (concat
   (gutter [0 height 0] :north 8)
   (gutter [3 height 0] :east 8)
   (gutter [3 height 3] :south 8)
   (gutter [0 height 3] :west 8)))

(defn half-gutters
  "All four half-gutters, at the given height"
  [height]
  (concat
   (half-gutter [0 height 0] :north 8)
   (half-gutter [3 height 0] :east 8)
   (half-gutter [3 height 3] :south 8)
   (half-gutter [0 height 3] :west 8)))

(defn floors
  "All four floors, at the given height"
  [height]
  (concat
   (floor [4 height -1] :north)
   (floor [4 height 4] :east)
   (floor [-1 height 4] :south)
   (floor [-1 height -1] :west)))

(defn walls
  "The walls around the spawning chamber"
  [level height]
  (s/rectube {:height height :width 20 :length 20
              :material ::wall :start [-8 level -8]}))

(defn ceiling
  "The ceiling on top of the spawning chamber, we also put some torches so no mobs
  spawn on the top at night."
  [height]
  (concat
   (for [x (range -7 11)
         y [height]
         z (range -7 11)]
     [x y z ::ceiling {} #_{:type :bottom}])
   (for [x [-8 11]
         y [height]
         z [-8 11]]
     [x y z :torch])))

(defn single-level-spawner
  "Single level spawner, uses a height of 21 so mobs take significant fall damage
  without dying, that way you can finish them off for the XP. The height also is
  important for the spawn dynamics. Too close and mobs don't spawn, too far off
  and then won't walk around, preventing them from falling down the chute."
  []
  (concat
   (chests-and-hoppers)
   (chute-and-trapdoors 21)
   (gutters 25)
   (floors 25)
   (walls 26 2)
   (ceiling 28)))

(defn three-level-spawner
  "Multi-level spawner for extra efficiency."
  [& [{:keys [fall-height] :or {fall-height 21}}]]
  (concat
   (chests-and-hoppers)
   (chute-and-trapdoors fall-height)
   (gutters (+ fall-height 4))
   (floors (+ fall-height 4))
   (half-gutters (+ fall-height 7))
   (floors (+ fall-height 7))
   (half-gutters (+ fall-height 10))
   (floors (+ fall-height 10))
   (walls (+ fall-height 5) 8)
   (ceiling (+ fall-height 13))))

(comment
  (def anchor [548 180 -72])

  (wc/build!
   (concat (platform)
           (three-level-spawner {:fall-height 19}))
   {:anchor anchor
    :palette palette})

  )
