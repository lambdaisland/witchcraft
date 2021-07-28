(ns bridge
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.cursor :as c]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]))

(comment
  (wc/start!)
  (.setTime (wc/world) 0)
  (wc/clear-weather)
  (fly!)
  (sort (keys materials))
  )

(defonce c (c/start))

#_(wc/teleport
   (highest-block-at
    (some #(when  (not= (.name (.getType %)) "AIR") %)
          (repeatedly #(get-block {:x (rand-int 500)
                                   :y 90
                                   :z (rand-int 500)}
                                  )))))

(comment
  (lambdaisland.witchcraft.cursor/undo!)
  (lambdaisland.witchcraft.cursor/redo!)
  )


(defn surface [c]
  (-> c
      (c/draw)
      (c/rotate -1)
      (c/material :cobblestone)
      (c/steps 69)
      (c/extrude 6 :north)))


(defn rsteps [c r n]
  (-> c
      (c/rotate r)
      (c/steps n)))

(defn reps [c n f]
  (nth (iterate f c) n))

(defn circle
  ([c]
   (circle c 2))
  ([c size]
   (let [dir-adjust (fn [c n]
                      (if (or (#{:east :west :north :south} (:dir c))
                              (even? n))
                        n
                        (inc n)))]
     (reps c 8 #(let [size (dir-adjust % size)]
                     (rsteps % 1 size))))))

(defn circle-ish [c]
  (reps c 4 #(-> %
                    (c/rotate 1)
                    c/step
                    (c/rotate 1)
                    (c/steps 2))))

(defn four-by-four [c]
  (reps c 4 #(rsteps % 2 1)))

(defn pillars-base [c]
  (-> c
      (c/rotate -1)
      (c/steps 10)
      (c/steps 6 :down)
      (c/draw)
      (c/material :smooth-brick)
      (circle 1)
      (c/move 10)
      circle
      (c/move 10)
      (c/rotate 1) (c/move 2) (circle 1) (c/move -2) (c/rotate -1)
      (c/move 10)
      circle
      (c/move 10)
      (c/rotate 1) (c/move 2) (circle 1) (c/move -2) (c/rotate -1)
      (c/extrude 10 :down)))

(defn pillars-top [c]
  (-> c
      (c/rotate -1)
      (c/steps 10)
      (c/steps 5 :down)
      (c/rotate 2)
      (c/steps 1)
      (c/rotate -2)

      (c/draw)
      (c/material :smooth-brick)

      (c/rotate 1) (four-by-four) (c/rotate -1)
      (c/move 10)
      (c/rotate 1) (c/move 1) circle-ish (c/move -1) (c/rotate -1)
      (c/move 10)
      (c/rotate 1) (c/move 1) (four-by-four) (c/move -1) (c/rotate -1)
      (c/move 10)
      (c/rotate 1) (c/move 1) circle-ish (c/move -1) (c/rotate -1)
      (c/move 10)
      (c/rotate 1) (four-by-four) (c/rotate -1)

      (c/extrude 4 :up)
      c/build
      ))

(def c (c/start (wc/location (wc/player))))

(comment
  (c/build (pillars-base c))
  (c/build (pillars-top c))
  (c/build (surface c)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn grow
  ([{:keys [blocks] :as cursor}]
   (assoc cursor :blocks (grow cursor blocks)))
  ([cursor blocks]
   (let [coords #(select-keys % [:x :y :z])
         existing (set (map coords blocks))]
     (:blocks
      (reduce
       (fn [c b]
         (reduce
          (fn [c d]
            (let [new-c (-> c (merge (coords b)) (c/face d) (c/move 1))]
              (if (contains? existing (coords new-c))
                c
                (c/block new-c))))
          c
          [:north :east :south :west]))
       cursor
       (:blocks cursor))))))


(defn pillar
  ([c height size]
   (pillar c height size :up))
  ([c height size dir]
   (let [layer (fn [c size]
                 (update c
                         :blocks
                         into
                         (-> (assoc c :blocks #{})
                             four-by-four
                             (reps (dec size) grow)
                             :blocks)))
         factor (/ height size)]
     (reduce (fn [c s]
               (c/excursion c
                            (fn [c]
                              (let [start (Math/round (double (* factor s)))
                                    end (Math/round (double (* factor (inc s))))]
                                (-> c
                                    (c/move start dir)
                                    (reps (inc (- end start))
                                             #(-> %
                                                  (c/move 1 dir)
                                                  (layer (- size s)))))))))
             c (range size)))))

(defn to-bottom [c]
  (c/move-to c (wc/highest-block-at c)))

(def pos (c/start))

(teleport pos)

(defn pillar-row [c {:keys [distance
                            pillar-height pillar-width
                            platform-height platform-width
                            y-base
                            pillars]
                     :or   {distance        12
                            pillar-height   20
                            platform-height 4
                            pillar-width    3
                            platform-width  5
                            y-base          60
                            pillars         4 }}]
  (let [next-pillar (fn [c] (-> c
                                (assoc :y y-base)
                                (pillar pillar-height pillar-width)
                                (assoc :y (+ y-base pillar-height platform-height))
                                (pillar platform-height platform-width :down)
                                (c/move distance)))]
    (reps c pillars next-pillar)))

(defn build-bridge [pos params]
  ;; bridge pillars
  (-> pos
      (c/move 5)
      (c/draw)
      (c/material :smooth-brick)
      (pillar-row params)
      c/build)

  (-> pos
      (assoc :y (+ (:y-base params) (:pillar-height params)
                   (:platform-height params)))
      (c/move 3 :left)
      (c/draw)
      (c/material :wood-step)
      (c/steps (+  (* (:distance params)
                      (:pillars params)) 2))
      (c/extrude 7 :right)
      (c/extrude 1 :forward-left)
      #_(update :blocks
                (fn [blocks]
                  (map
                   (fn [b]
                     (if (< (rand-int 100) 20)
                       (assoc b :material :glass)
                       b))
                   blocks)))
      c/build))

(build-bridge (c/start)
              {:pillars 3
               :distance 18
               :pillar-height 20
               :pillar-width 5
               :platform-height 5
               :y-base 60})

(c/rotate-dir  (:dir pos) 1)

;; bridge surface

(-> pos
    (c/move 3 :south)
    (c/draw)
    (c/material :fence)
    (c/steps 50)
    (c/extrude 1 :north)
    c/build)

(-> pos
    (c/move 5 :north)
    (c/move 3 :south-east)
    (c/draw)
    (c/material :fence)
    (c/steps 50)
    (c/extrude 1 :south)
    c/build)

(-> pos
    (c/face :north-west)
    (c/draw)
    (c/steps 4)
    c/build)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn catenary [x length]
  (let [y #(- (Math/cosh (/ % (/ length 2))) 1)
        x-factor (y (/ length 2))]
    (-  (/ (y x) x-factor) 1)))

(for [x [-10 -5 0 5 10]]
  (catenary x 20))

(let [start  (c/start)
      end    (c/move start 60)
      dip    25
      delta  (fn [a b]
               (Math/sqrt (* (Math/abs (- (:x a) (:x b)))
                             (Math/abs (- (:z a) (:z b))))))
      length (delta start end)
      y-fn   #(* (catenary (-  % (/ length 2)) length) dip)
      step-fn (fn [curs dir]
                (let [[x _ z] (get c/movements dir)
                      curs (-> curs (update :x + x) (update :z + z))
                      dist (delta start curs)]
                  (assoc curs :y (Math/round (+ (:y start) (y-fn dist))))))
      curs (assoc start :step-fn step-fn)]
  (-> curs
      (c/material :wood-double-step)
      (c/steps 60)
      (c/step :left)
      (c/extrude 1 :forward-right)
      (c/extrude 3 :right)
      c/build))

(->  (c/start)
     (c/material :cobblestone)
     (c/steps 3)
     (c/extrude 5 :forward-right)
     (c/extrude 10 :up)

     c/build)
