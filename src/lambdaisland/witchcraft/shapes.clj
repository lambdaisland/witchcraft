(ns lambdaisland.witchcraft.shapes
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.matrix :as m]))

(defn range*
  "Like range, but deal with negative or non-ascending numbers in a way
   we like better.

  (range -3) ;;=> (-2 -1 0)
  (range 5 2) ;;=> (2 3 4)"
  ([a]
   (if (< 0 a)
     (range a)
     (range (inc a) 1)))
  ([a b]
   (if (< a b)
     (range a b)
     (range b a))))

(defn ball-fn
  "Return a predicate that checks if the location is part of the ball."
  [{:keys [radius center inner-radius]
    :or {inner-radius (- radius 1)
         center [0 0 0]}}]
  (let [[cx cy cz] (wc/xyz center)]
    (fn self
      ([loc]
       (self (wc/x loc) (wc/y loc) (wc/z loc)))
      ([x y z]
       (< inner-radius (wc/distance center [x y z]) radius)))))

(defn- handle-start+material [loc start material]
  (as-> loc $
    (if start (m/v+ $ start) $)
    (if material
      (let [m (if (fn? material) (material $) material)]
        (if (vector? m)
          (into $ m)
          (conj $ m)))
      $)))

(defn box
  "Create a simple box.
  We also use this as the base shape to carve out other shapes.

  If the start position is omitted you get a box which starts at [0 0 0] and you
  need to position the result yourself.

  `:material` is either a keyword, or a function which receives the `[x y z]`
  position and returns a keyword. `:material` is optional, you can add materials
  on the result yourself instead.

  Returns a sequence of `[x y z]` or `[x y z material]`, to be passed
  to [[wc/set-blocks]].

  To specify the size use `:height`, and either `:width`/`:length`, or
  `:east-west-length`/`:north-south-length`, as per your preference.
  "
  [{:keys [east-west-length width
           north-south-length length
           height
           material
           start]}]
  (let [width (or east-west-length width)
        length (or north-south-length length)]
    (for [x (range* width)
          y (range* height)
          z (range* length)]
      (handle-start+material [x y z] start material))))

(defn ball
  "Create a ball shape.
  By default the inner-radius is one block less than the outer radius, so you
  get a ball with a \"wall\" that is one block thick. Set inner-radius to 0 to
  get a solid ball.

  `:material` is either a keyword, or a function which receives the `[x y z]`
  position and returns a keyword.

  Returns a sequence of `[x y z]` or `[x y z material]`, to be passed
  to [[wc/set-blocks]]."
  [{:keys [radius center inner-radius material fill]
    :as opts}]
  (let [pred (ball-fn opts)]
    (concat
     (for [loc (map #(wc/add %
                             center
                             [(- radius) (- radius) (- radius)])
                    (box {:east-west-length (inc (* radius 2))
                          :north-south-length (inc (* radius 2))
                          :height (inc (* radius 2))
                          :material material}))
           :when (pred loc)]
       loc)
     (when fill
       (ball (assoc opts
                    :radius (or inner-radius (- radius 1))
                    :inner-radius 0
                    :material fill
                    :fill nil))) )))

(defn line
  "Draw a straight line between two points.
  Specify either end or direction+length, not both.

  `:material` is either a keyword, or a function which receives the `[x y z]`
  position and returns a keyword.

  Returns a sequence of `[x y z]` or `[x y z material]`, to be passed
  to [[wc/set-blocks]]."
  [{:keys [start
           end
           length
           direction
           material]}]
  (assert (or (and end (not direction) (not length))
              (and (not end) direction length))
          "specify either end or direction+length, not both")
  (if (= start end)
    [(conj (wc/xyz start) (if (fn? material)
                            (material (wc/xyz start))
                            material))]
    (let [start (wc/xyz start)
          end (wc/xyz end)
          direction (when direction (m/vnorm direction))
          end (or end (m/v+ start (m/v* direction length)))
          direction (or direction (m/v* (m/vnorm (m/v- end start)) 0.51))
          new-pos #(conj (mapv (fn [x] (Math/round x)) %)
                         (if (fn? material)
                           (material (wc/xyz %))
                           material))]
      (loop [blocks #{(new-pos start)}
             pos start]
        (if (< (wc/distance pos end) 1)
          (conj blocks (new-pos end))
          (recur (conj blocks (new-pos pos))
                 (m/v+ pos direction)))))))

(defn tube
  "Draw a tube, pipe, cylinder, or tunnel.

  This can be arbitrarily rotated, it does not have to be axis-aligned.

  `:start` and `:end` are the two ends of the central axis of the cylinder.
  `:radius` is the radius of the cylinder, `:inner-radius` is the space inside
  the \"tube\" that is hollow (or filled in if you specify a `:fill` material).

  Alternatively to specifying `:end` you can specify a `:direction` + `:length`.

  `:material` and `:fill` are either keywords, or functions which receive the
  `[x y z]` position and returns a keyword.

  For best result experiment with fractional numbers, e.g. a `:radius
  6.1 :inner-radius 4.9` may look better than `:radius 6 :inner-radius 5`.

  To compute this we determine the bounding box, then iterate over all blocks,
  project them onto the central axis, and then see if the distance between the
  block and this projected point equals the radius. You can specify the
  `:distance-fn` to use something other than euclidian distance here, e.g.
  Manhatten ([[m/manhatten]]) distance or Chebyshev ([[m/chebyshev]]) distance,
  which will give you more of a square pipe.

  Returns a sequence of `[x y z]` or `[x y z material]`, to be passed
  to [[wc/set-blocks]]."
  [{:keys [start
           end
           length
           direction
           radius
           inner-radius
           material
           fill
           distance-fn]
    :or {inner-radius -0.1
         distance-fn wc/distance}}]
  (assert (or (and end (not direction) (not length))
              (and (not end) direction length))
          "specify either end or direction+length, not both")
  (let [direction (when direction (m/vnorm direction))
        end (or end (m/v+ start (m/v* direction length)))
        direction (or direction (m/vnorm (m/v- end start)))
        length (or length (wc/distance start end))
        ;; https://stackoverflow.com/posts/36773942
        [dx dy dz] direction
        sigma (if (< dx 0) -1 1)
        h (+ dx sigma)
        beta (/ -1.0 (* sigma h))
        f (* beta dy)
        g (* beta dz)
        u (m/v* (m/vnorm [(* f h) (+ 1.0 (* f dy)) (* f dz)]) radius)
        v (m/v* (m/vnorm [(* g h) (* g dy) (+ 1.0 (* g dz))]) radius)
        bounds (mapcat #(map (partial m/v+ %) [u v (m/v- u) (m/v- v)]) [start end])
        [x1 y1 z1] [(apply min (map wc/x bounds))
                    (apply min (map wc/y bounds))
                    (apply min (map wc/z bounds))]
        [x2 y2 z2] [(apply max (map wc/x bounds))
                    (apply max (map wc/y bounds))
                    (apply max (map wc/z bounds))]]
    (for [x (range x1 (inc x2))
          y (range y1 (inc y2))
          z (range z1 (inc z2))
          :let [;; The component of the [x y z] vector projected onto the axis
                axis-component (m/dot-product (m/v- [x y z] start) direction)
                ;; the location on the axis of the projection of [x y z]
                axis-loc (m/v+ start (m/v* direction axis-component))
                block? (and
                        (<= 0 axis-component length)
                        (<=
                         inner-radius
                         (distance-fn [x y z] axis-loc)
                         (+ radius 0.1)))
                inside? (<=
                         (distance-fn [x y z] axis-loc)
                         inner-radius)]
          :when (or (and fill inside?) block?)]
      (let [material (cond
                       (and block? (fn? material)) (material [x y z])
                       block?     material
                       (fn? fill) (fill [x y z])
                       :else      fill)]
        (cond-> [x y z]
          material
          (conj material))))))

(defn torus
  "Draw a torus shape.
  `:radius` is the radius of the \"ring\", `:tube-radius` is the radius of the
  tube/pipe. `:material` is optional and can be a keyword or a function from `[x
  y z]` to keyword. `:margin` determines the \"thickness\", experiment with
  different values depending on the size of your torus.

  Returns a sequence of `[x y z]` or `[x y z material]`, to be passed
  to [[wc/set-blocks]]."
  [{:keys [radius tube-radius material margin start]
    :or {margin 15}}]
  (for [x (range (Math/floor (- (- radius) margin))
                 (Math/ceil (+ radius margin)))
        y (range (Math/floor (- (- tube-radius) 3))
                 (Math/ceil (+ tube-radius 3)))
        z (range (Math/floor (- (- radius) margin))
                 (Math/ceil (+ radius margin)))
        :when (<= (Math/abs (- (+ (Math/pow (- (Math/sqrt (+ (* x x) (* z z)))
                                               radius) 2)
                                  (* y y))
                               (* tube-radius tube-radius)))
                  margin)]
    (handle-start+material [x y z] start material)))

(defn rectube
  "Create a rectangular tube, i.e. a box but with the inside hollow and two
  opposing sides open.

  Arguments are like [[box]], but with the added `:direction` which can be
  `:east-west`, `:north-south`, or `:top-bottom`. `:top-bottom` is the default."
  [{:keys [east-west-length width
           north-south-length length
           height
           material
           start
           direction]
    :or {direction :top-bottom}}]
  (let [width (or east-west-length width)
        length (or north-south-length length)
        xs (range* width)
        ys (range* height)
        zs (range* length)]
    (for [x xs, y ys, z zs
          :when (or (and (or (= (first xs) x) (= (last xs) x))
                         (not (= direction :east-west)))
                    (and (or (= (first ys) y) (= (last ys) y))
                         (not (= direction :top-bottom)))
                    (and (or (= (first zs) z) (= (last zs) z))
                         (not (= direction :north-south))))]
      (handle-start+material [x y z] start material))))


(comment
  (wc/set-blocks
   (tube {:start [732 70 -783]
          :end [732 80 -770]
          :material :air
          :radius 6.1
          :inner-radius 3.9
          :distance-fn m/chebyshev
          }))

  (wc/set-blocks
   (box {:start [750 63 -755]
         :east-west-length -30
         :north-south-length -30
         :height -1
         :material :water}))

  (wc/set-blocks
   (tube {:start [732 40 -770]
          :end [732 256 -770]
          :material :air
          :radius 3.1
          :inner-radius 1.9
          :fill :air}))


  (wc/set-blocks
   (tube {:start [732 80 -783]
          :end [730 90 -780]
          :material :stone})))
