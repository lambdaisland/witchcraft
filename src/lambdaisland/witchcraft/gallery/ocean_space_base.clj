(ns lambdaisland.witchcraft.gallery.ocean-space-base
  "A futuristic base consisting of a big torus suspending by three pointy towers."
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.palette :as p]
            [lambdaisland.witchcraft.matrix :as m]))

(def palette
  (concat
   (repeat 13 :netherrack)
   (repeat 7 :red-terracotta)
   (repeat 7 :orange-terracotta)
   (repeat 7 :red-sandstone)
   (repeat 5 :copper-block)
   (repeat 7 :stripped-jungle-log)
   (repeat 11 :white-terracotta)
   (repeat 23 :white-concrete)))

(defn sloped-pillar [loc]
  (let [gen (p/palette-generator palette)]
    (wc/set-blocks
     (for [x (range -18 19)
           y (range (count palette))
           z (range -18 19)
           :when (< (- 16.5 (Math/sqrt y) (Math/sqrt y))
                    (Math/sqrt (+ (* x x) (* z z)))
                    (- 18.5 (Math/sqrt y) (Math/sqrt y)))]
       {:x (+ x (wc/x loc))
        :y (+ y (wc/y loc))
        :z (+ z (wc/z loc))
        :material
        (if (= (mod (inc y) 13) 0)
          :sea-lantern
          (gen y))}))))

(defn torus-shape [{:keys [r R material-fn]
                    :or {material-fn (constantly :green-stained-glass)}}]
  (let [margin 15]
    (for [x (range (Math/floor (- (- R) margin)) (Math/ceil (+ R margin)))
          y (range (Math/floor (- (- r) 3)) (Math/ceil (+ r 3)))
          z (range (Math/floor (- (- R) margin)) (Math/ceil (+ R margin)))
          :when (<= (Math/abs (- (+ (Math/pow (- (Math/sqrt (+ (* x x) (* z z)))
                                                 R) 2)
                                    (* y y))
                                 (* r r)) )
                    margin)]
      {:x x
       :y y
       :z z
       :material (material-fn {:x x :y y :z z})})))

(defn torus [loc]
  (let [gen-fn (p/palette-generator [:deepslate
                                     :deepslate
                                     :deepslate-copper-ore
                                     :deepslate-copper-ore
                                     :deepslate-copper-ore
                                     :basalt
                                     :basalt
                                     :basalt
                                     :cyan-terracotta
                                     :cyan-terracotta
                                     :cyan-terracotta
                                     ])]
    (wc/set-blocks
     (m/transform
      (torus {:R 70
              :r 11
              :material-fn
              (fn [{:keys [x y z]}]
                (if (< -3 y 3)
                  :green-stained-glass
                  (if (< (rand-int 100) 5)
                    :shroomlight
                    (gen-fn (Math/abs y)))))})
      (m/translation-matrix (wc/xyz loc))))))

(comment
  ;; built in the squid.casa world at these locations
  (torus [723 112 -764])
  (sloped-pillar [783 63 -729])
  (sloped-pillar [671 63 -716]))
