(defn neighbours
  "Find all neighbours of a block along the given axes, defaults to :x/:z, i.e.
  neighbours in a horizontal plane."
  ([loc]
   (neighbours loc #{:x :z}))
  ([loc axis]
   (for [dx (if (:x axis) [-1 0 1] [0])
         dy (if (:y axis) [-1 0 1] [0])
         dz (if (:z axis) [-1 0 1] [0])
         nloc [(wc/add loc [dx dy dz])]
         :when (not= (wc/location loc) nloc)
         block [(wc/get-block nloc)]
         :when (not= (wc/material-name block) :air)]
     block)))

(defn fill
  "Recursively find neighbours"
  [start]
  (loop [search #{start}
         result #{start}]
    (let [new-blocks (reduce
                      (fn [res loc]
                        (into res (remove result) (neighbours loc)))
                      #{}
                      search)]
      (if (seq new-blocks)
        (recur new-blocks (into result new-blocks))
        result))))
