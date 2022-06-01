(ns lambdaisland.witchcraft.palette
  "Utilities for constructing block palettes and working with texture colors."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure2d.color :as c]
            [lambdaisland.witchcraft :as wc]))

(def material-colors
  "The two most prominent colors in each material's texture."
  (delay
    (into (sorted-map)
          (map (fn [[k v]]
                 [k (mapv #(apply c/color %) v)]))
          (read-string
           (slurp
            (io/resource "lambdaisland/witchcraft/material_colors.edn"))))))

(defn block-materials
  "Materials that can be used as blocks.
  Still needs more filtering, so may yield some weird results."
  []
  (->> @material-colors
       (map key)
       (filter #(let [m (wc/material %)]
                  (and (.isSolid m)
                       (not (.hasGravity m)))))
       (remove #{:anvil
                 :large-amethyst-bud
                 :daylight-detector
                 :conduit
                 :lectern
                 :brewing-stand
                 :hopper})
       (remove #(str/includes? (name %) "-coral"))
       (remove #(str/ends-with? (name %) "-stem"))
       (remove #(str/ends-with? (name %) "-glass-pane"))
       (remove #(str/ends-with? (name %) "-door"))
       (remove #(str/ends-with? (name %) "-trapdoor"))
       (remove #(str/starts-with? (name %) "potted-"))))

(defn nearest-material
  "Find the block material that most closely matches the color or colors."
  ([color]
   (nearest-material color color))
  ([color1 color2]
   (key
    (apply min-key
           (fn [[m [c1 c2]]]
             (min
              (+ (c/delta-e-cie c1 color1)
                 (c/delta-e-cie c2 color2))
              (+ (c/delta-e-cie c1 color2)
                 (c/delta-e-cie c2 color1))))
           (select-keys @material-colors (block-materials))))))

(defn distance [m1 m2]
  (let [[s1 s2] (get @material-colors m1)
        [e1 e2] (get @material-colors m2)
        d1 (+ (c/delta-e-cie s1 e1)
              (c/delta-e-cie s2 e2))
        d2 (+ (c/delta-e-cie s1 e2)
              (c/delta-e-cie s2 e1))]
    (if (< d2 d1) d2 d1)))

(comment
  (distance :cobblestone :stone) ;; 15.108876181814015
  (distance :red-glazed-terracotta :emerald-block) ;; 214.6447281120871
  )

(defn material-gradient
  "Given a start material, an end material, and a number of steps, generate a
  sequence of block materials that form a gradient."
  [start end steps]
  (assert (get @material-colors start))
  (assert (get @material-colors end))
  (let [[s1 s2] (get @material-colors start)
        [e1 e2] (get @material-colors end)
        d1 (+ (c/delta-e-cie s1 e1)
              (c/delta-e-cie s2 e2))
        d2 (+ (c/delta-e-cie s1 e2)
              (c/delta-e-cie s2 e1))
        [e1 e2] (if (< d2 d1) [e2 e1] [e1 e2])]
    (map
     nearest-material
     (c/palette
      (c/gradient [s1 e1]
                  {:colorspace :LAB})
      steps)
     (c/palette
      (c/gradient [s2 e2]
                  {:colorspace :LAB})
      steps))))

(defn rand-palette
  "Takes a palette probability map (keyword to number), and return a random
  material, honoring the probabilities."
  [probs]
  (let [probs (remove (fn [[m i]] (<= i 0)) probs)]
    (reduce
     (fn [rem [m i]]
       (if (< rem i)
         (reduced m)
         (- rem i)))
     (rand (apply + (map second probs)))
     probs)))

(comment
  (rand-palette
   {:stone 10
    :blue-terracotta 2
    :black-stained-glass 1}))

(defn gradient-gen
  "Given a palette (sequence of keywords), return a function which takes an
  index (number), and returns a material that is either the material at that
  entry in the palette, or a neighboring material, with further off materials
  increasingly unlikely to be picked. This allows using a palette with somewhat
  gradual transitions.

  `:spread` is the amount of blocks distance that each material in the palette
  will take up.

  `:bleed-distance` is how many blocks distance a neighboring material is allowed to
  \"bleed\" into the next.

  `:bleed` is the probability that a neighboring block bleeds into the next
  \"zone\", with `1` meaning it has the same probability as the main material
  for the given zone. This probability halves for every block further into the
  next zone you are.
  "
  [{:keys [palette spread bleed bleed-distance]
    :or {palette [:bedrock
                  :deepslate-bricks
                  :deepslate
                  :stone-bricks
                  :cracked-stone-bricks
                  :stone]
         spread 3
         bleed 0.2
         bleed-distance 2}}]
  (fn [x]
    (let [pos (quot x spread)
          midx (min (dec (count palette))
                    (quot x spread))]
      (rand-palette
       (into {}
             (map-indexed (fn [idx m]
                            [m
                             (let [dist (long (Math/abs (- (* spread idx) x)))]
                               (cond
                                 (and (= idx 0) (< x 0))
                                 1
                                 (and (= (dec (count palette)) idx)
                                      (<= (- (* spread (count palette))
                                             (/ spread 2))
                                          (Math/ceil x)))
                                 1
                                 (<= dist (/ spread 2))
                                 1
                                 (<= dist (+ (/ spread 2) bleed-distance))
                                 (nth (iterate #(/ % 2) bleed) (Math/round (double (/ (dec dist)
                                                                                      (/ spread 2)))))
                                 :else
                                 0
                                 ))])
                          palette))))))

(defn neighbors
  "Get materials that are close in color to the given material.
  Returns a seq of all materials with their score, sorted from best match to
  worst."
  [material]
  (sort-by last
           (map (juxt identity #(distance material %))
                (remove #{material}
                        (block-materials)))))

(comment
  (neighbors :deepslate))
