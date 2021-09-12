(ns gradients
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [lambdaisland.witchcraft :as wc]
            [clojure2d.color :as c])
  (:import javax.imageio.ImageIO
           java.awt.image.BufferedImage
           java.awt.geom.AffineTransform
           java.awt.image.AffineTransformOp
           (com.cryptomorin.xseries XMaterial XBlock)
           ))

(def images
  (into {}
        (map (fn [f]
               [(-> (.getName f)
                    (str/replace #".png" "")
                    (str/replace #"_" "-")
                    (keyword))
                f]))
        (filter #(str/ends-with? (str %) ".png")
                (file-seq
                 (io/file
                  "/home/arne/tmp/mojang/assets/minecraft/textures/block/")))))

(def materials
  (into {}
        (map
         (fn [k]
           (when-let [f (some images [k
                                      (keyword (str (name k) "-block"))
                                      (keyword (str (name k) "-side"))
                                      (keyword (str (name k) "-front"))
                                      (keyword (str (name k) "-top"))])]
             [k f])))
        (keys wc/materials)))

(def material-colors
  (into (sorted-map)
        (remove
         (comp #{[0 0 0]} second)
         (for [[k f] materials]
           [k
            (try
              (let [orig (ImageIO/read f)
                    before (BufferedImage. (.getWidth orig) (.getHeight orig) BufferedImage/TYPE_INT_ARGB)
                    after (BufferedImage. 1 1 BufferedImage/TYPE_INT_ARGB)
                    scale (AffineTransform.)]
                (.scale scale (double (/ 1 (.getWidth before)))
                        (double (/ 1 (.getHeight before))))
                (.drawImage (.getGraphics before) orig 0 0 nil)
                (let [pixel
                      (aget
                       (..
                        (.filter
                         (AffineTransformOp. scale AffineTransformOp/TYPE_BICUBIC)
                         before after)
                        getRaster
                        getDataBuffer
                        getData) 0)]
                  [
                   (bit-shift-right (bit-and pixel 0xff0000) 16)
                   (bit-shift-right (bit-and pixel 0xff00) 8)
                   (bit-and pixel 0xff)]
                  ))
              (catch Throwable t
                t))]))))

(def two-colors
  (into (sorted-map)
        (for [[k f] materials]
          [k
           (try
             (let [orig (ImageIO/read f)
                   img (BufferedImage. (.getWidth orig) (.getHeight orig) BufferedImage/TYPE_INT_ARGB)]
               (.drawImage (.getGraphics img) orig 0 0 nil)
               (c/reduce-colors
                (map
                 (fn [pixel]
                   (c/color (bit-shift-right (bit-and pixel 0xff0000) 16)
                            (bit-shift-right (bit-and pixel 0xff00) 8)
                            (bit-and pixel 0xff)
                            (bit-shift-right (bit-and pixel 0xff000000) 24)))
                 (..
                  img
                  getRaster
                  getDataBuffer
                  getData))
                2)
               )
             (catch Throwable t
               t))])))



(spit "/home/arne/github/lambdaisland/witchcraft/resources/lambdaisland/witchcraft/material_colors.edn"
      (with-out-str
        (clojure.pprint/pprint
         (into (sorted-map)
               (for [[k v] two-colors]
                 [k (let [colors (vec (remove #{[0 0 0]}
                                              (map #(mapv long (cond-> %
                                                                 (= 255.0 (doto (last %) ))
                                                                 butlast)) v)))]
                      (if (= 1 (count colors))
                        (into colors colors)
                        colors))])))))

(defn block-materials []
  (->> material-colors
       (map key)
       (filter #(let [m (wc/material %)]
                  (and (.isSolid m)
                       (not (.hasGravity m)))))
       (remove #{:anvil :large-amethyst-bud :daylight-detector :conduit :lectern})
       (remove #(str/includes? (name %) "-coral"))
       (remove #(str/ends-with? (name %) "-stem"))
       (remove #(str/ends-with? (name %) "-glass-pane"))
       (remove #(str/ends-with? (name %) "-door"))
       (remove #(str/ends-with? (name %) "-trapdoor"))
       (remove #(str/starts-with? (name %) "potted-"))))

(defn draw-gradient! [start end n]
  (wc/set-blocks
   (map-indexed (fn [idx m]
                  {:x (+ 258 idx)
                   :y 73
                   :z 21
                   :material m})
                (doto (material-gradient2 start
                                          end
                                          n)
                  prn))))
(.hasGravity (wc/material :anvil))

(count (block-materials))
(count material-colors)
(wc/set-time 0)
(c/delta-e-cie
 (c/color 143 128 55)
 (c/color 245 223 178))

(nearest-material (c/color 255 244 209))

(defn nearest-material [color]
  (key
   (apply min-key
          (fn [[m c]]
            (c/delta-e-cie (apply c/color c) color))
          (filter (comp (set (block-materials)) key) material-colors))))

(defn material-gradient [start end steps]
  (assert (get material-colors start))
  (assert (get material-colors end))
  (println start end)
  (map
   nearest-material
   (c/palette
    (c/gradient [(apply c/color (get material-colors start))
                 (apply c/color (get material-colors end))]
                {:colorspace :LAB})
    steps)))

(defn nearest-material2 [color1 color2]
  (key
   (apply min-key
          (fn [[m [c1 c2]]]
            (min
             (+ (c/delta-e-cie c1 color1)
                (c/delta-e-cie c2 color2))
             (+ (c/delta-e-cie c1 color2)
                (c/delta-e-cie c2 color1))))
          (filter (comp (set (block-materials)) key) two-colors))))
(map (fn [[k v]] [k (count v)])
     (filter (comp (set (block-materials)) key) two-colors))

(defn material-gradient2 [start end steps]
  (assert (get two-colors start))
  (assert (get two-colors end))
  (println start end)
  (let [[s1 s2] (get two-colors start)
        [e1 e2] (get two-colors end)
        d1 (+ (c/delta-e-cie s1 e1)
              (c/delta-e-cie s2 e2))
        d2 (+ (c/delta-e-cie s1 e2)
              (c/delta-e-cie s2 e1))
        [e1 e2] (if (< d2 d1) [e2 e1] [e1 e2])]
    (map
     nearest-material2
     (c/palette
      (c/gradient [s1 e1]
                  {:colorspace :LAB})
      steps)
     (c/palette
      (c/gradient [s2 e2]
                  {:colorspace :LAB})
      steps))))

(wc/set-time 0)

(material-gradient2 :birch-log :lapis-ore 10)
(get two-colors :birch-log)
(wc/set-block [261 72 21] :lapis-ore)
(wc/undo!)

(:redstone-ore two-colors)

(draw-gradient! :red-nether-bricks :chiseled-sandstone 10)

(draw-gradient! (rand-nth (block-materials))
                (rand-nth (block-materials)))
(draw-gradient! :netherrack
                :red-sandstone
                10)
(draw-gradient! :red-sandstone
                :white-terracotta
                10)

(wc/clear-weather)
(wc/set-time 0)
(wc/add-inventory (wc/player "sunnyplexus") :diamond-pickaxe)
(wc/fly! (wc/player "sunnyplexus"))

(concat
 (material-gradient2 :netherrack
                     :red-sandstone
                     4)
 (material-gradient2 :red-sandstone
                     :white-terracotta
                     4))


(wc/undo!)
(Math/sqrt 200)
[364 76 12]

(defn palette-generator [palette]
  (fn [layer]
    (let [probs (map-indexed (fn [i m]
                               [(/ 1 (Math/pow
                                      (inc (Math/abs (- i layer)))
                                      3)) m])
                             palette)]
      (reduce
       (fn [rem [i m]]
         (if (< rem i)
           (reduced m)
           (- rem i)))
       (rand (apply + (map first probs)))
       probs))))

(let [g (palette-generator [:a :b :c :d :e :f])]
  (frequencies (take 100 (repeatedly #(g 2)))))
(frequencies palette)

(def palette
  (concat
   (repeat 13 :netherrack)
   (repeat 7 :red-terracotta)
   (repeat 7 :orange-terracotta)
   (repeat 7 :red-sandstone)
   (repeat 5 :copper-block)
   (repeat 7 :stripped-jungle-log)
   (repeat 11 :white-terracotta)
   (repeat 23 :white-concrete)
   ))


(let [gen (palette-generator palette)]
  (wc/set-blocks
   (for [x (range -18 19)
         y (range (count palette))
         z (range -18 19)
         :when (< (Math/sqrt (+ (* x x) (* z z)))
                  (- 18.5 (Math/sqrt y) (Math/sqrt y)))]
     {:x (+ x 197 #_373) :y (+ y 63) :z (+ z 15 #_12)
      :material
      (if (= (mod (inc y) 13) 0)
        :sea-lantern
        (gen y))})))

(wc/teleport [200 100 22])

(wc/set-time 20000)
(wc/undo!)
