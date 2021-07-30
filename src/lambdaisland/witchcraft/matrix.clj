(ns lambdaisland.witchcraft.matrix
  "Vector/Matrix math

  A vector in this context can be anything that
  implements [[lambdaisland.witchcraft/with-xyz]]: a Clojure vector (`[x y z]`),
  a Clojure map (`{:x .. :y .. :z ..}`), or a Glowstone `Location` or `Vector`.
  You get the type back that you put in.

  A matrix is a vector of vectors (regular Clojure vectors) and can be
  3x3 (linear) or 4x4 (affine/homogenous).

  This code is not optimized for speed, it is fine for generating and
  manipulating minecraft structures, not for heavy number crunching.

  "
  (:require [lambdaisland.witchcraft :as wc]))

(defn v-
  "Vector subtraction

  Arguments can be Clojure maps (:x/:y/:z), vectors, or Glowstone Location or
  Vector instances. The return type is the type of `a`.
  "
  [a b]
  (wc/with-xyz a (mapv - (wc/xyz a) (wc/xyz b))))

(defn v+
  "Vector addition

  Arguments can be Clojure maps (:x/:y/:z), vectors, or Glowstone Location or
  Vector instances. The return type is the type of `a`.
  "
  [a b]
  (wc/with-xyz a (mapv + (wc/xyz a) (wc/xyz b))))

(defn v*
  "Multiply a vector with a scalar

  `v` can be a
  Clojure map (`:x/:y/:z`), vector (`[x y z]`), or Glowstone Location or Vector
  instance. Returns the same type as `v`."
  [v s]
  (wc/with-xyz v (map (partial * s) v)))

(defn m*
  "Multiply a matrix with a scalar"
  [m s]
  (mapv (partial mapv (partial * s)) m))

(defn dot-product
  "Vector dot product

  Arguments can be Clojure maps (:x/:y/:z), vectors, or Glowstone Location or
  Vector instances. Returns a number.
  "
  [a b]
  (let [a (if (vector? a) a (wc/xyz a))
        b (if (vector? b) b (wc/xyz b))]
    (reduce + (map * a b))))

(defn m*v
  "Multiply a matrix (vector of vectors) with a vector

  `m` is a Clojure vector of vectors, 3x3 (linear) or 4x4 (affine). `v` can be a
  Clojure map (`:x/:y/:z`), vector (`[x y z]`), or Glowstone Location or Vector
  instance. Returns the same type as `v`.
  "
  [m v] (wc/with-xyz v (mapv (partial dot-product (wc/xyz1 v)) m)))

(defn transpose
  "Transpose a matrix"
  [m]
  (apply mapv vector m))

(defn m*m
  "Multiply matrices"
  ([m1 m2 & rest]
   (apply m*m (m*m m1 m2) rest))
  ([m1 m2]
   (let [m2 (transpose m2)]
     (mapv (fn [row]
             (mapv (fn [bs]
                     (dot-product row bs)) m2))
           m1))))

(defn identity-matrix
  "Return a `degree x degree` matrix with all elements on the diagonal `1` and all
  others `0`"
  [degree]
  (mapv (fn [y]
          (mapv (fn [x]
                  (if (= x y) 1 0))
                (range degree)))
        (range degree)))

(defn translation-matrix
  "Returns an affine transformation matrix that moves a location by a fixed amount
  in each dimension."
  [v]
  (let [[x y z] (wc/xyz v)]
    [[1 0 0 x]
     [0 1 0 y]
     [0 0 1 z]
     [0 0 0 1]]))

(defn rotation-matrix
  "Matrix which rotates around the origin, takes the rotation in radians, and the
  dimensions that form the plane in which the rotation is performed,
  e.g. `(rotation-matrix Math/PI :x :z)`"
  [rad dim1 dim2]
  (let [row1 (mapv (fn [dim]
                     (cond (= dim dim1) (Math/cos rad)
                           (= dim dim2) (- (Math/sin rad))
                           :else 0))
                   [:x :y :z 0])
        row2 (mapv (fn [dim]
                     (cond (= dim dim1) (Math/sin rad)
                           (= dim dim2) (Math/cos rad)
                           :else 0))
                   [:x :y :z 0])
        row0 [0 0 0 0]]
    [(cond (= :x dim1) row1 (= :x dim2) row2 :else row0)
     (cond (= :y dim1) row1 (= :y dim2) row2 :else row0)
     (cond (= :z dim1) row1 (= :z dim2) row2 :else row0)
     [0 0 0 1]]))

(defn mirror-matrix
  "Matrix which mirrors points, mappings is a map of one or more of `:x/:y/:z` to
  `:x/:-x/:y/:-y/:z/:-z`. E.g. a mapping of `{:x :-x}` means that the x value
  gets flipped, in other words it's a mirroring around the `z=0` plane.
  `{:x :z, :z :x}` means that the `x` and `z` values get swapped, i.e. a
  mirroring around the `x=z` plane."
  [mappings]
  (mapv
   (fn [dim]
     (if (= 0 dim)
       [0 0 0 1]
       (case (get mappings dim dim)
         :x [1 0 0 0]
         :-x [-1 0 0 0]
         :y [0 1 0 0]
         :-y [0 -1 0 0]
         :z [0 0 1 0]
         :-z [0 0 -1 0])))
   [:x :y :z 0]))

(defn with-origin
  "Takes an affine transformation matrix, and an origin coordinate, and returns a
  matrix which performs the same trasnformation, but around the new origin. Use
  this to change the \"anchor\" around which e.g. a rotation happens, which by
  default is otherwise the `[0 0 0]` origin coordinate."
  [matrix origin]
  (m*m
   (translation-matrix (v* origin -1))
   matrix
   (translation-matrix origin)))

#_
(with-origin (rotation-matrix Math/PI :x :z) [100 100 100])
