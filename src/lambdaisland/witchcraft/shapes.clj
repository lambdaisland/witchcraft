(ns lambdaisland.witchcraft.shapes
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.matrix :as m]))

(defn ball-fn
  "Return a predicate that checks if the location is part of the ball."
  [{:keys [radius center inner-radius]
    :or {inner-radius (- radius 1.5)
         center [0 0 0]}}]
  (let [[cx cy cz] (wc/xyz center)]
    (fn self
      ([loc]
       (self (wc/x loc) (wc/y loc) (wc/z loc)))
      ([x y z]
       (< inner-radius (wc/distance center [x y z]) radius)))))

(defn box
  "Create a simple box.
  We also use this as the base shape to carve out other shapes."
  [{:keys [east-west-length
           north-south-length
           height
           material]}]
  (for [x (range 0 east-west-length)
        y (range 0 height)
        z (range 0 north-south-length)]
    (cond-> [x y z] material (conj material))))

(defn ball
  "Create a ball shape.
  By default the inner-radius is one block less than the outer radius, so you
  get a ball with a \"wall\" that is one block thick. Set inner-radius to 0 to
  get a solid ball."
  [{:keys [radius center inner-radius material]
    :as opts}]
  (let [pred (ball-fn opts)]
    (for [loc (map #(wc/add %
                            center
                            [(- radius) (- radius) (- radius)])
                   (box {:east-west-length (inc (* radius 2))
                         :north-south-length (inc (* radius 2))
                         :height (inc (* radius 2))
                         :material material}))
          :when (pred loc)]
      material)))

(defn line [{:keys [start
                    end
                    length
                    direction
                    material]}]
  (assert (or (and end (not direction) (not length))
              (and (not end) direction length))
          "specify either end or direction+length, not both")
  (let [direction (when direction (m/vnorm direction))
        end (or end (m/v+ start (m/v* direction length)))
        direction (or direction (m/vnorm (m/v- end start)))]
    (loop [blocks #{(conj start material)}
           pos start]
      (if (< (wc/distance pos end) 1.5)
        blocks
        (recur (conj blocks (conj pos material))
               (m/v+ pos direction))))))

(defn tube [{:keys [start
                    end
                    length
                    direction
                    radius
                    inner-radius
                    material]}]
  (assert (or (and end (not direction) (not length))
              (and (not end) direction length))
          "specify either end or direction+length, not both")
  (let [direction (when direction (m/vnorm direction))
        end (or end (m/v+ start (m/v* direction length)))
        direction (or direction (m/vnorm (m/v- end start)))
        ;; https://stackoverflow.com/posts/36773942
        [dx dy dz] direction
        sigma (if (< dx 0) -1 1)
        h (+ dx sigma)
        beta (/ -1.0 (* sigma h))
        f (* beta dy)
        g (* beta dz)
        u (m/vnorm [(* f h) (+ 1.0 (* f dy)) (* f dz)])
        v (m/vnorm [(* g h) (* g dy) (+ 1.0 (* g dz))])
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
          :when (m/dot-product )])))


(wc/set-blocks
 (line {:start [732 80 -783]
        :end [730 90 -780]
        :material :air}))
(wc/set-blocks
 (tube {:start [732 80 -783]
        :end [730 90 -780]
        :material :stone}))

(m/vlength (m/vnorm [1 1 0]))

(wc/set-blocks
 (ball {:radius 10
        :center [732 125 -783]
        :material :yellow-stained-glass}))

(wc/set-time 0)

(wc/material-name [732 125 -783 :glass])

(seq (wc/entities (wc/chunk [0 0 0])))
