(ns terraforming-2021-07-11
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.cursor :as c]))
(load "terraform")

(wc/start! {:level-seed "witchcraft"})
(def me (wc/player "sunnyplexus"))

(defn whereami []
  [(wc/x me) (wc/y me) (wc/z me)])

(defn find-land
  "Pick a random location on the map, and if it seems like there is land there,
  then teleport over."
  []
  (let [loc [(rand-int 2000) 70 (rand-int 2000)]]
    (when (not= :air (:material (wc/block loc)))
      (prn loc)
      (loop [loc loc]
        (if (and (= :air (:material (wc/block loc)))
                 (< (second loc) 108))
          (do
            (prn loc)
            (wc/teleport (assoc loc 1 105)))
          (recur (update loc 1 inc)))))))

;; Initial starting location. Pretty cool island but very island, lots of ocean
(def island-loc [27.058644734325476 86.0 82.51919827689997])

;; Found a mountain that I'm gonna turn into a mesa
(def mesa-loc [2255.651103806431 110.0 164.30000001192093])
(def mesa-overview-loc [2181.9006158178167 120.15193543982032 180.17158041016833 270.29968 21.000025])

(wc/teleport mesa-loc)

;; Interaction heplers, click on a block to inspect it with @B
(def B (atom nil))
(wc/listen! :player-interact ::capture-block
            (fn [e]
              (when (:clickedBlock e)
                (reset! B (:clickedBlock e)))
              #_(wc/set-blocks
                 (for [b (fill (:clickedBlock e))]
                   {:x (wc/x b)
                    :y (wc/y b)
                    :z (wc/z b)
                    :material :air}))))

;; We used the above to flatten the mesa, now pave it, looking somewhat
;; naturally with some variation.

(wc/set-blocks (for [b (fill @B)]
                 (let [n (rand-int 100)
                       [m d]
                       (cond
                         (<= 0 n 50) [:stone 0]
                         (<= 51 n 77) [:cobblestone 0]
                         (<= 78 n 80) [:stone 4]
                         (<= 81 n 93) [:stone 6]
                         (<= 93 n 95) [:stone 3]
                         (<= 96 n 100) [:gravel])]
                   {:x (wc/x b)
                    :y (wc/y b)
                    :z (wc/z b)
                    :material m
                    :data d})))

;; A path up. Use a custom step-fn to follow the shape of the land, and to clear up any snow
(-> (c/start [2273 104 161] :west)
    (assoc :step-fn (fn [c dir]
                      (let [c (c/step-fn c dir)]
                        (cond-> c
                          (#{:air :snow} (wc/material-name (wc/get-block c)))
                          (-> (update :blocks conj (c/block-value (assoc c :material :air)))
                              (update :y dec))
                          (not= :air (wc/material-name (wc/get-block (update c :y inc))))
                          (update :blocks conj (c/block-value (assoc (update c :y inc) :material :air)))))))
    (c/material :grass-path)
    (c/step)
    (c/step)
    (c/reps 28
            (fn [c] (-> c
                        (c/rotate -2)
                        (c/step)
                        (c/rotate -2)
                        (c/step)
                        (c/rotate 2)
                        (c/step)
                        (c/rotate 2)
                        (c/step)
                        (update :x #(let [n (rand-int 10)]
                                      (cond
                                        (< n 1) (inc %)
                                        (< n 4) (dec %)
                                        :else %))))))
    (#(do (def path-blocks (:blocks %)) %))
    (c/build!))
(wc/add-inventory me :diamond-axe)
[{:x 2272, :y 104, :z 161, :material :grass-path, :data 0}
 {:x 2273, :y 104, :z 161, :material :grass-path, :data 0}
 {:x 2272, :y 103, :z 162, :material :grass-path, :data 0}
 {:x 2273, :y 103, :z 162, :material :grass-path, :data 0}
 {:x 2272, :y 103, :z 163, :material :grass-path, :data 0}
 {:x 2273, :y 103, :z 163, :material :grass-path, :data 0}
 {:x 2272, :y 102, :z 164, :material :grass-path, :data 0}
 {:x 2273, :y 102, :z 164, :material :grass-path, :data 0}
 {:x 2272, :y 102, :z 165, :material :grass-path, :data 0}
 {:x 2273, :y 102, :z 165, :material :grass-path, :data 0}
 {:x 2271, :y 102, :z 166, :material :grass-path, :data 0}
 {:x 2272, :y 101, :z 166, :material :grass-path, :data 0}
 {:x 2271, :y 101, :z 167, :material :grass-path, :data 0}
 {:x 2272, :y 101, :z 167, :material :grass-path, :data 0}
 {:x 2271, :y 101, :z 168, :material :grass-path, :data 0}
 {:x 2272, :y 100, :z 168, :material :grass-path, :data 0}
 {:x 2271, :y 100, :z 169, :material :grass-path, :data 0}
 {:x 2272, :y 100, :z 169, :material :grass-path, :data 0}
 {:x 2271, :y 99, :z 170, :material :grass-path, :data 0}
 {:x 2272, :y 99, :z 170, :material :grass-path, :data 0}
 {:x 2271, :y 98, :z 171, :material :grass-path, :data 0}
 {:x 2272, :y 98, :z 171, :material :grass-path, :data 0}
 {:x 2270, :y 98, :z 172, :material :grass-path, :data 0}
 {:x 2271, :y 98, :z 172, :material :grass-path, :data 0}
 {:x 2270, :y 97, :z 173, :material :grass-path, :data 0}
 {:x 2271, :y 97, :z 173, :material :grass-path, :data 0}
 {:x 2269, :y 96, :z 174, :material :grass-path, :data 0}
 {:x 2270, :y 96, :z 174, :material :grass-path, :data 0}
 {:x 2269, :y 96, :z 175, :material :grass-path, :data 0}
 {:x 2270, :y 96, :z 175, :material :grass-path, :data 0}
 {:x 2268, :y 95, :z 176, :material :grass-path, :data 0}
 {:x 2269, :y 95, :z 176, :material :grass-path, :data 0}
 {:x 2268, :y 94, :z 177, :material :grass-path, :data 0}
 {:x 2269, :y 95, :z 177, :material :grass-path, :data 0}
 {:x 2267, :y 94, :z 178, :material :grass-path, :data 0}
 {:x 2268, :y 94, :z 178, :material :grass-path, :data 0}
 {:x 2267, :y 94, :z 179, :material :grass-path, :data 0}
 {:x 2268, :y 94, :z 179, :material :grass-path, :data 0}
 {:x 2266, :y 93, :z 180, :material :grass-path, :data 0}
 {:x 2267, :y 93, :z 180, :material :grass-path, :data 0}
 {:x 2266, :y 93, :z 181, :material :grass-path, :data 0}
 {:x 2267, :y 93, :z 181, :material :grass-path, :data 0}
 {:x 2266, :y 92, :z 182, :material :grass-path, :data 0}
 {:x 2267, :y 92, :z 182, :material :grass-path, :data 0}
 {:x 2266, :y 91, :z 183, :material :grass-path, :data 0}
 {:x 2267, :y 92, :z 183, :material :grass-path, :data 0}
 {:x 2265, :y 91, :z 184, :material :grass-path, :data 0}
 {:x 2266, :y 91, :z 184, :material :grass-path, :data 0}
 {:x 2265, :y 90, :z 185, :material :grass-path, :data 0}
 {:x 2266, :y 90, :z 185, :material :grass-path, :data 0}
 {:x 2265, :y 89, :z 186, :material :grass-path, :data 0}
 {:x 2266, :y 89, :z 186, :material :grass-path, :data 0}
 {:x 2265, :y 89, :z 187, :material :grass-path, :data 0}
 {:x 2266, :y 89, :z 187, :material :grass-path, :data 0}
 {:x 2266, :y 88, :z 188, :material :grass-path, :data 0}
 {:x 2267, :y 88, :z 188, :material :grass-path, :data 0}
 {:x 2266, :y 88, :z 189, :material :grass-path, :data 0}
 {:x 2267, :y 88, :z 189, :material :grass-path, :data 0}
 {:x 2267, :y 87, :z 190, :material :grass-path, :data 0}
 {:x 2268, :y 87, :z 190, :material :grass-path, :data 0}
 {:x 2267, :y 86, :z 191, :material :grass-path, :data 0}
 {:x 2268, :y 86, :z 191, :material :grass-path, :data 0}
 {:x 2267, :y 85, :z 192, :material :grass-path, :data 0}
 {:x 2268, :y 85, :z 192, :material :grass-path, :data 0}
 {:x 2267, :y 85, :z 193, :material :grass-path, :data 0}
 {:x 2268, :y 85, :z 193, :material :grass-path, :data 0}
 {:x 2267, :y 85, :z 194, :material :grass-path, :data 0}
 {:x 2268, :y 85, :z 194, :material :grass-path, :data 0}
 {:x 2267, :y 85, :z 195, :material :grass-path, :data 0}
 {:x 2268, :y 85, :z 195, :material :grass-path, :data 0}
 {:x 2266, :y 85, :z 196, :material :grass-path, :data 0}
 {:x 2267, :y 85, :z 196, :material :grass-path, :data 0}
 {:x 2266, :y 83, :z 197, :material :grass-path, :data 0}
 {:x 2267, :y 84, :z 197, :material :grass-path, :data 0}
 {:x 2266, :y 82, :z 198, :material :grass-path, :data 0}
 {:x 2267, :y 81, :z 198, :material :grass-path, :data 0}
 {:x 2266, :y 79, :z 199, :material :grass-path, :data 0}
 {:x 2267, :y 80, :z 199, :material :grass-path, :data 0}
 {:x 2266, :y 78, :z 200, :material :grass-path, :data 0}
 {:x 2267, :y 78, :z 200, :material :grass-path, :data 0}
 {:x 2266, :y 78, :z 201, :material :grass-path, :data 0}
 {:x 2267, :y 78, :z 201, :material :grass-path, :data 0}
 {:x 2265, :y 78, :z 202, :material :grass-path, :data 0}
 {:x 2266, :y 78, :z 202, :material :grass-path, :data 0}
 {:x 2265, :y 78, :z 203, :material :grass-path, :data 0}
 {:x 2266, :y 78, :z 203, :material :grass-path, :data 0}
 {:x 2264, :y 77, :z 204, :material :grass-path, :data 0}
 {:x 2265, :y 77, :z 204, :material :grass-path, :data 0}
 {:x 2264, :y 77, :z 205, :material :grass-path, :data 0}
 {:x 2265, :y 77, :z 205, :material :grass-path, :data 0}
 {:x 2263, :y 77, :z 206, :material :grass-path, :data 0}
 {:x 2264, :y 77, :z 206, :material :grass-path, :data 0}
 {:x 2263, :y 77, :z 207, :material :grass-path, :data 0}
 {:x 2264, :y 77, :z 207, :material :grass-path, :data 0}
 {:x 2263, :y 77, :z 208, :material :grass-path, :data 0}
 {:x 2264, :y 77, :z 208, :material :grass-path, :data 0}
 {:x 2263, :y 76, :z 209, :material :grass-path, :data 0}
 {:x 2264, :y 76, :z 209, :material :grass-path, :data 0}
 {:x 2263, :y 76, :z 210, :material :grass-path, :data 0}
 {:x 2264, :y 76, :z 210, :material :grass-path, :data 0}
 {:x 2263, :y 76, :z 211, :material :grass-path, :data 0}
 {:x 2264, :y 76, :z 211, :material :grass-path, :data 0}
 {:x 2264, :y 76, :z 212, :material :grass-path, :data 0}
 {:x 2265, :y 76, :z 212, :material :grass-path, :data 0}
 {:x 2264, :y 75, :z 213, :material :grass-path, :data 0}
 {:x 2265, :y 76, :z 213, :material :grass-path, :data 0}
 {:x 2264, :y 75, :z 214, :material :grass-path, :data 0}
 {:x 2265, :y 75, :z 214, :material :grass-path, :data 0}
 {:x 2264, :y 75, :z 215, :material :grass-path, :data 0}
 {:x 2265, :y 75, :z 215, :material :grass-path, :data 0}
 {:x 2263, :y 75, :z 216, :material :grass-path, :data 0}
 {:x 2264, :y 75, :z 216, :material :grass-path, :data 0}
 {:x 2263, :y 75, :z 217, :material :grass-path, :data 0}
 {:x 2264, :y 75, :z 217, :material :grass-path, :data 0}]

(wc/set-time 0)
