(ns lambdaisland.witchcraft.cursor
  "Logo-like API for drawing blocks by using a walking metaphor.

  This is a functional API, movements simply build up a list of block positions,
  call [[build!]] at the end to actually make them materialize in the game.

  A cursor contains a current location (x/y/z), a
  directions (:north, :north-east, :east, etc.), and a current building
  material, e.g :lapis-block (see [[lambdaisland.witchcraft/materials]]).

  It also contains a drawing flag `:draw?` and a list of blocks `:blocks`. When
  drawing is on, then any step will add a block to the list. [[build!]] creates
  blocks in the world based on this, and resets the list.

  Call [[start]] to get an initial cursor, this will return a cursor that is one
  step ahead of the player (so what you draw is in sight), facing away from the
  player.

  ```
  (require '[lambdaisland.witchcraft.cursor :as c])

  (-> (c/start)
      (c/draw)
      (c/material :red-glazed-terracotta)
      (c/steps 3)
      (c/rotate 2)
      (c/material :blue-glazed-terracotta)
      (c/steps 3)
      (c/rotate 2)
      (c/material :green-glazed-terracotta)
      (c/steps 3)
      (c/rotate 2)
      (c/material :yellow-glazed-terracotta)
      (c/steps 3)
      (c/build)
      )
  ```
  "
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

(def default-material
  "Material used if no material is specified. This one has an arrow on it, which
  will show the direction the cursor is moving into."
  :magenta-glazed-terracotta)

(def directions
  [:south
   :south-west
   :west
   :north-west
   :north
   :north-east
   :east
   :south-east])

(def movements
  {:south      [0 0 1],
   :south-west [-1 0 1],
   :west       [-1 0 0],
   :north-west [-1 0 -1],
   :north      [0 0 -1],
   :north-east [1 0 -1],
   :east       [1 0 0],
   :south-east [1 0 1]
   :up         [0 1 0]
   :down       [0 -1 0]})

(def relative-movements
  {:forward        0
   :forwards       0
   :front          0
   :forward-right  1
   :right          2
   :backward-right 3
   :back           4
   :backward       4
   :backwards      4
   :backward-left  5
   :left           6
   :forward-left   7})

(declare step move)

(defn draw
  "Enable/disable drawing. Enables by default, pass false to disable."
  ([c]
   (draw c true))
  ([c draw?]
   (assoc c :draw? draw?)))

(defn start
  "Creates a new cursor, starting at the given location, or one step in front of
  the player's location."
  ([]
   (-> (wc/player)
       start
       move))
  ([loc]
   (start loc (:dir loc (nth directions (-> loc
                                            wc/yaw
                                            (/ 45)
                                            double
                                            Math/round
                                            (mod (count directions)))))))
  ([loc dir]
   (let [l (wc/location loc)
         x (Math/round (wc/x l))
         y (Math/round (wc/y l))
         z (Math/round (wc/z l))]
     (cond->
         {;; The cursor position
          :x x :y y :z z
          ;; The direction the cursor is facing (keyword), each step will move in
          ;; this direction
          :dir dir
          ;; The material we are drawing with
          :material default-material
          ;; Material properties, post-flattening only
          :block-data nil
          ;; Wether the "pen" is down, when this to false steps will move the cursor
          ;; but not add blocks
          :draw? true
          ;; The set of blocks that have been created, as maps with
          ;; `{:x :y :z :material :data}`, pass this
          ;; to [[lambdaisland.witchcraft/set-blocks]] (or use [[build]] on the
          ;; cursor)
          :blocks wc/EMPTY_BLOCK_SET
          ;; The center point for linear transformations like reflections,
          ;; see [[matrices]] and [[apply-matrix]]
          :origin {:x x :y y :z z}
          ;; Whether or not we should adjust the direction each block is facing
          ;; based on the direction on the cursor?
          :face-direction? true
          ;; Make all blocks face a specific direction, e.g. :east
          :block-facing nil
          ;; 3x3 matrices that are applied to each block that is placed. When this
          ;; is set each step causes multiple blocks to be set. Coordinates are
          ;; shifted before/after each transformation based on the `:origin`
          :matrices nil
          ;; Drawing palette, this acts as a lookup table for materials, so you can
          ;; give aliases to materials, or use abstract/semantic names for them.
          ;; Values need to be keywords or [keyword byte] pairs (material+data).
          ;; Keys can be anything. You can use characters as keys and pass strings
          ;; to [[pattern]] for instance.
          :palette {}
          ;; By default the first "step" isn't a step at all, it just sets a block
          ;; at the cursor's starting position. This tends to be the most intuitive
          ;; thing in most cases, but if it's not working for your case you can turn
          ;; it off.
          :first-step? (:first-step? loc true)
          ;; Relatively change the rotation of blocks based on the cursor direction
          :rotate-block 0}
       (wc/pre-flattening?)
       (assoc :data 0)))))

(defn excursion
  "Apply a function which adds blocks, then return the cursor to its orginal
  position and state."
  [cursor f & args]
  (assoc cursor :blocks (:blocks (apply f cursor args))))

(defn reps
  "Perform a function n times on the cursor"
  [c n f & args]
  (nth (iterate #(apply f % args) c) n))

(declare rotate-dir)

(defn block-value
  "Get the x/y/z and material data for the current cursor, so we can use it to
  create a block. This also resolves any palette indirection for the material.
  If the material is a two-element vector (either explicitly or via the palette)
  then this is taken as [material material-data], and overrides the
  material-data in the cursor."
  [{:keys [x y z material block-data block-facing
           data palette dir face-direction? rotate-block]}]
  (let [m (get palette material material)
        [m md] (if (vector? m) m [m data])
        md (or md 0)]
    (cond->
        {:x x
         :y y
         :z z
         :material m}
      block-facing
      (assoc :direction block-facing)
      face-direction?
      (assoc :direction (rotate-dir dir rotate-block))
      data
      (assoc :data data)
      block-data
      (assoc :block-data block-data))))

(defn apply-matrix
  "Apply a single matrix to a single x/y/z map based on the origin."
  [{:keys [origin dir] :as c} matrix]
  (let [cv ((juxt :x :y :z) c)
        ov ((juxt :x :y :z) origin)
        v- #(mapv - %1 %2)
        v+ #(mapv + %1 %2)
        dot #(reduce + (map * %1 %2))
        m*v #(mapv (partial dot %2) %1)
        [x y z] (v+ ov (m*v matrix (v- cv ov)))
        new-movement (m*v matrix (get movements dir))
        new-dir (some #(when (= new-movement (val %))
                         (key %)) movements)]
    (assoc c
           :x x
           :y y
           :z z
           :dir (or new-dir dir))))

(defn block-fn [c]
  (update c :blocks conj (block-value c)))

(defn block
  "Add a block to the block list based on the cursor location.

  Will set multiple blocks if symmetries are defined, see [[reflect]]."
  [{:keys [matrices block-fn] :as c
    :or {block-fn block-fn}}]
  (reduce
   (fn [c s]
     (excursion c
                (fn [c]
                  (block-fn (apply-matrix c s)))))
   (block-fn (dissoc c :first-step?))
   matrices))

(defn ?block
  "Add a block to the block list based on the current cursor location and
  material, but only if drawing is enabled. (pronounced \"maybe block\")"
  [c]
  (if (:draw? c)
    (block c)
    c))

(defn material
  "Set the current cursor material, and optionally material-data (integer), or
  block-data (map), to be used for consecutive blocks."
  ([c m]
   (if (vector? m)
     (material c (first m) (second m))
     (material c m nil)))
  ([c m md]
   (if (map? md)
     (assoc c :material m :block-data md :data nil)
     (assoc c :material m :block-data nil :data md))))

(defn rotate-dir
  "Given a direction keyword like :north or :south and a number, make that many
  1/8 turns clockwise.

  (rotate :north 4) ;; => :south"
  [dir n]
  (if (some #{dir} directions)
    (nth (drop-while (complement #{dir}) (cycle directions)) (mod n 8))
    dir))

(defn rotate
  "Rotate the cursor clockwise by a number of 1/8 turns clockwise."
  [{:keys [dir xy-dir] :as cursor} n]
  (let [n-even (mod (+ n (mod n 2)) 8)]
    (assoc
      cursor :dir
      (cond
        (= dir :up)
        (case n-even
          0 :up
          2 (rotate-dir xy-dir 2)
          4 :down
          6 (rotate-dir xy-dir 6))
        (= dir :down)
        (case n-even
          0 :down
          2 (rotate-dir xy-dir 6)
          4 :up
          6 (rotate-dir xy-dir 2))

        :else
        (rotate-dir dir n)))))

(defn rotation
  "Specify a relative rotation in 1/8 turns clockwise. Blocks will still be
  aligned based on the cursor direction, but then additionally given this much
  rotation. Useful in case your blocks are not aligning with the draw line in
  the way that you wanted."
  [cursor n]
  (assoc cursor :rotate-block n))

(defn face
  "Face the cursor in a certain direction"
  [cursor dir]
  (if (not= dir (:dir cursor))
    (assoc cursor
           :dir dir
           :xy-dir (cond
                     ;; save the direction where the feet are pointing when going
                     ;; up/down, important for rotate later on
                     (= :up dir) (:dir cursor)
                     (= :down dir) (rotate-dir (:dir cursor) 4)))
    cursor))

(defn block-data
  "Set the `BlockData` block-data (post-flattening and material-dependent)."
  [cursor prop-map]
  (assoc cursor :block-data prop-map))

(defn step-fn
  "Default implementation of how a single step happens, i.e. determine the
  direction and update `:x` / `:y` / `:z` accordingly. Can be overruled by
  adding a `:step-fn` to the cursor."
  [c dir]
  (let [[x y z] (get movements dir)]
    (-> c
        (assoc :dir dir)
        (update :x + x)
        (update :y + y)
        (update :z + z))))

(defn resolve-dir
  "Helper for dealing with forward/left/right type of directions, instead of
  east/north/west."
  [facing asked]
  (let [rotation (get relative-movements asked)]
    (if rotation
      (rotate-dir facing rotation)
      asked)))

(def valid-movements
  (-> #{}
      (into (keys relative-movements))
      (into (keys movements))))

(defn step
  "Take one step forward in the direction given, or the direction the cursor is
  facing. If drawing is enabled this will also add a block to the block list
  corresponding with the new location."
  ([cursor]
   (step cursor (:dir cursor)))
  ([{:keys [step-fn]
     :or {step-fn step-fn}
     :as cursor} dir]
   ;; For the very first step don't move the cursor yet, just put a block where
   ;; we are. Not 100% sure about this yet, moving the cursor first before
   ;; putting a block down is generally the right thing to do, but when starting
   ;; with a new cursor it is confusing that your first block is actually one
   ;; step away from your starting position, so this tries to do the generally
   ;; most intuitive thing.
   (assert (keyword? dir) (str "Direction must be a keyword, got " (pr-str dir)))
   (assert (contains? valid-movements dir) (str "Unknown direction " dir ", should be one-of "
                                                valid-movements))
   (if (and (:draw? cursor) (:first-step? cursor))
     (?block cursor)
     (?block (step-fn cursor (resolve-dir (:dir cursor) dir)))))
  ([cursor dir & dirs]
   (apply step (step cursor dir) dirs)))

(defn steps
  "Take n steps forward as with [[step]], or in the direction specified. Can take
  multiple number/direction pairs to do a full walk. Will change the direction
  of the cursor to the direction of the final step."
  ([cursor n]
   (steps cursor n (:dir cursor)))
  ([cursor n dir]
   (let [dir (resolve-dir (:dir cursor) dir)]
     (if (< n 0)
       (steps cursor (- n) (rotate-dir dir 4))
       (nth (iterate #(step % dir) cursor) n))))
  ([cursor n dir & more]
   (apply steps
          (steps cursor n dir)
          more)))

(defn move
  "Move the cursor as with steps, but without drawing. Retains the cursor
  direction."
  ([cursor]
   (move cursor 1))
  ([{:keys [draw? dir] :as cursor} n]
   (move cursor n dir))
  ([{:keys [draw?] :as cursor} n dir]
   (assert (int? n) (str "Move takes a number of steps, got " n))
   (-> cursor
       (draw false)
       (steps n dir)
       (draw draw?)
       (assoc :dir (:dir cursor))))
  ([c n dir & more]
   (apply move (move c n dir) more)))

(defn move-to
  "Move the cursor to the given location, does not draw."
  [cursor loc]
  (merge cursor (select-keys (bean loc) [:x :y :z])))

(defn excursion
  "Apply a block-drawing function f, then return to the original position."
  [cursor f & args]
  (assoc cursor :blocks (:blocks (apply f cursor args))))

(defn block-facing
  "Make the cursor produce blocks with the specified direction."
  [cursor dir]
  (-> cursor
      (assoc :block-facing dir)
      (assoc :face-direction? false)))

(defn extrude
  "Take the current block list and extrude it in a given direction, by default up."
  ([cursor n]
   (extrude cursor n :up))
  ([cursor n dir]
   (let [dir (resolve-dir (:dir cursor) dir)]
     (assoc
       (reduce
        (fn [c b]
          (reduce
           (fn [c i]
             (excursion
              c
              (fn [c]
                (reps
                 (assoc (merge c b) :block-facing (:direction b))
                 i
                 (fn [c]
                   (step c dir))))))
           c
           (range 1 (inc n))))
        cursor
        (:blocks cursor))
       :block-facing (:block-facing cursor)))))

(def material->keyword (into {} (map (juxt val key)) wc/materials))

(defn blocks
  "Get the set of blocks placed by the cursor"
  [{:keys [blocks] :as cursor}]
  blocks)

(defn build!
  "Apply the list of blocks in the cursor to the world."
  ([cursor]
   (build! cursor nil))
  ([{:keys [blocks] :as cursor} opts]
   (wc/set-blocks blocks opts)
   (assoc cursor :blocks #{})))

(def
  ^{:deprecated true
    :doc "See [[build!]]"}
  build build!)

(defn flash!
  "Like build, but shortly after undoes the build again

  This is meant for REPL use where you can rapidly try out changes before
  committing to them."
  ([c]
   (flash! c 2000))
  ([c timeout]
   (future
     (Thread/sleep timeout)
     (wc/undo!))
   (build! c)))

(defn palette
  "Add items to the palette. Takes a single map."
  [c m]
  (update c :palette (fnil into {}) m))

(defn pattern
  "Draw a number of steps in a line, based on a sequence of materials. Note that
  in combination with palette this can also take a string, assuming you've added
  palette entries for the given characters."
  [c pattern]
  (merge (reduce (fn [c m] (-> c (material m) (step))) c pattern)
         (select-keys c [:material :data])))

(defn face-direction?
  "Should the direction blocks are facing be based on the direction of the
  cursor?"
  [cursor bool]
  (assoc cursor :face-direction? bool))

(defn translate
  "Move all blocks in the block set, as well as the cursor, by a given offset."
  [c offset]
  (let [x (wc/x offset)
        y (wc/y offset)
        z (wc/z offset)
        f #(-> %
               (update :x + x)
               (update :y + y)
               (update :z + z))]
    (update (f c) :blocks #(wc/block-set (map f) %))))

(defn origin
  "Set the origin around which matrix operations are performed"
  [c & a]
  (assoc c :origin {:x (wc/x a)
                    :y (wc/y a)
                    :z (wc/z a)}))

(defn matrices
  "Set matrices"
  [c & matrices]
  (assoc c :matrices matrices))

;; TODO: we need to come up with a better API for specifying these, but this
;; already nicely illustrates what you can do with it
(defn symmetry-xz [c]
  (matrices c
            [[0 0 1]
             [0 1 0]
             [1 0 0]]
            [[0 0 -1]
             [0 1 0]
             [1 0 0]]
            [[0 0 1]
             [0 1 0]
             [-1 0 0]]
            [[0 0 -1]
             [0 1 0]
             [-1 0 0]]
            [[-1 0 0]
             [0 1 0]
             [0 0 1]]
            [[1 0 0]
             [0 1 0]
             [0 0 -1]]
            [[-1 0 0]
             [0 1 0]
             [0 0 -1]]))

(comment
  (-> {:x 1 :y 1 :z 1 :dir :east}
      (rotate 4)
      step
      step
      step
      (rotate 2)
      step))
