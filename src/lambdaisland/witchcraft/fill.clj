(ns lambdaisland.witchcraft.fill
  "Fill algorithms, find areas of touching blocks based on a predicate."
  (:require [lambdaisland.witchcraft :as wc]))

(defn neighbours
  "Find all neighbours of a single block along the given axes, defaults to :x/:z,
  i.e. neighbours in a horizontal plane.

  `:dx` / `:dy` / `:dz` each determine how a given axis is traversed, e.g.
  `:dx [-1 0 1]` will traverse both towards positive and negative X (east/west),
  whereas `:dx [0 1]` will only traverse towards positive X. `:dy [0]` (the
  default for Y) means the Y value will not change, i.e. only the current Y
  level (height) is considered.

  `:pred` is a function which receives a block, and returns true if the block
  should be added to the result, or false otherwise. The default function
  considers all blocks that are not `:air` blocks.

  Alternative pass in a set of `:materials` (keywords), and it will only find
  neighbours of those types.
  "
  ([loc]
   (neighbours loc nil))
  ([loc {:keys [dx dy dz pred materials]
         :or {dx [-1 0 1]
              dy [0]
              dz [-1 0 1]
              }}]
   (let [pred (or pred
                  (if materials
                    (comp (set materials) wc/material-name)
                    #(not= (wc/material-name %) :air)))]
     (for [dx dx dy dy dz dz
           nloc [(wc/add (wc/location loc) [dx dy dz])]
           :when (not= (wc/location loc) nloc)
           block [(wc/get-block nloc)]
           :when (pred block)]
       block))))

(defn fill
  "Recursively find neighbours, as per [[neighbours]].
  It's quite easy to accidentally trigger an infinite loop, which will crash
  your Minecraft server. To somewhat guard against that the result set will only
  grow for `:limit` iterations. If the algorithm hasn't terminated yet at that
  point an exception will be thrown. Increase `:limit` or set it to `false` if
  you really know what you're doing. Defaults to `15`."
  ([start]
   (fill start nil))
  ([start {:keys [limit throw?] :or {limit 15 throw? true} :as opts}]
   (let [start (wc/get-block start)]
     (loop [search #{start}
            result #{start}
            iterations 0]
       (if (and limit (<= limit iterations))
         (if throw?
           (throw (ex-info (str `fill " did not terminate within " limit " iterations, aborting.")
                           {:start start :opts opts}))
           result)
         (let [new-blocks (reduce
                           (fn [res loc]
                             (into res (remove result) (neighbours loc opts)))
                           #{}
                           search)]
           (if (seq new-blocks)
             (recur new-blocks (into result new-blocks) (inc iterations))
             result)))))))

(defn fill-xyz
  "Perform a [[fill]] along the x, y, and z axes. Convenience function."
  ([start] (fill-xyz start nil))
  ([start opts] (fill start (assoc opts :dx [-1 0 1] :dy [-1 0 1] :dz [-1 0 1]))))

(defn fill-xy
  "Perform a [[fill]] along the x, and y axes. Convenience function."
  ([start] (fill-xy start nil))
  ([start opts] (fill start (assoc opts :dx [-1 0 1] :dy [-1 0 1] :dz [0]))))

(defn fill-xz
  "Perform a [[fill]] along the x, and z axes. Convenience function."
  ([start] (fill-xz start nil))
  ([start opts] (fill start (assoc opts :dx [-1 0 1] :dy [0] :dz [-1 0 1]))))

(defn fill-x
  "Perform a [[fill]] along the x axis. Convenience function."
  ([start] (fill-x start nil))
  ([start opts] (fill start (assoc opts :dx [-1 0 1] :dy [0] :dz [0]))))

(defn fill-yz
  "Perform a [[fill]] along the y, and z axes. Convenience function."
  ([start] (fill-yz start nil))
  ([start opts] (fill start (assoc opts :dx [0] :dy [-1 0 1] :dz [-1 0 1]))))

(defn fill-y
  "Perform a [[fill]] along the y axis. Convenience function."
  ([start] (fill-y start nil))
  ([start opts] (fill start (assoc opts :dx [0] :dy [-1 0 1] :dz [0]))))

(defn fill-z
  "Perform a [[fill]] along the z axis. Convenience function."
  ([start] (fill-z start nil))
  ([start opts] (fill start (assoc opts :dx [0] :dy [0] :dz [-1 0 1]))))

#_
(for [x [true false]
      y [true false]
      z [true false]
      :when (or x y z)]
  (let [sym (symbol (str "fill-" (when x "x") (when y "y") (when z "z")))]
    `(~'defn ~sym
      ~(let [axes (cond-> []
                    x (conj "x")
                    y (conj "y")
                    z (conj "z"))]
         (str "Perform a [[fill]] along the "
              (case (count axes)
                1 (str (first axes) " axis")
                2 (str (first axes) ", and " (second axes) " axes")
                3 (str (first axes) ", " (second axes) ", and " (last axes) " axes"))
              ". Convenience function." ))
      ([~'start] (~sym ~'start nil))
      ([~'start ~'opts]
       (~'fill ~'start (~'assoc ~'opts
                        :dx ~(if x [-1 0 1] [0])
                        :dy ~(if y [-1 0 1] [0])
                        :dz ~(if z [-1 0 1] [0]))))
      )))
