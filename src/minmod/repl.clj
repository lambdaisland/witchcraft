(ns minmod.repl
  (:import (net.glowstone GlowServer)
           (org.bukkit Location))
  (:require [clojure.string :as str]
            [minmod.glowserver :as server]
            [minmod.bukkit :as bukkit :refer [entities materials]]))

(server/start!)

(def me (server/player))

(def my-loc (.getLocation me))
(def world (.getWorld my-loc))

(defprotocol Datafy
  (datafy [_]))

(defprotocol Loc
  (location [_])
  (world [_]))

(extend-protocol Datafy
  Location
  (datafy [l]
    {:x (.getBlockX l)
     :y (.getBlockY l)
     :z (.getBlockZ l)
     :yaw (.getYaw l)
     :pitch (.getPitch l)
     :world (.getWorld l)}))

(extend-protocol Loc
  net.glowstone.entity.GlowPlayer
  (location [player]
    (.getLocation player))
  (world [player]
    (world (.getLocation player)))

  org.bukkit.Location
  (world [loc]
    (.getWorld loc))

  java.util.Map
  (location [{:keys [x y z yaw pitch world]}]
    (Location. world x y z yaw pitch))
  )

[(location (datafy (location me)))
 (location me)]


(defn set-block-type [world {:keys [x y z]} type]
  (.setType (.getBlockAt world x y z) type))

(let [{:keys [x y z]} (datafy (location (server/player)))]
  (doseq [x (range x (+ x 5))
          y (range y (+ y 5))
          z (range z (+ z 5))]
    (set-block-type (world (server/player)) {:x x :y y :z z} (:diamond-block materials))))

(let [radius 100
      {:keys [x y z]} (datafy (location (server/player)))]
  (doseq [x (range (- x 100) (+ x 100))
          y (range y (+ y 30))
          z (range (- z 100) (+ z 100))]
    (set-block-type (world (server/player)) {:x x :y y :z z} (:air materials))))

(let [{:keys [x y z]} (datafy (location (server/player)))]
  (doseq [x (range (- x 100) (+ x 100))
          y (range y (+ y 30))
          z (range (- z 100) (+ z 100))]
    (set-block-type (world (server/player)) {:x x :y y :z z} (:air materials))))

(defn kubus [{:keys [x y z]}
             {:keys [hoogte breedte lengte]}
             materiaal]
  (doseq [x (range x (+ x breedte))
          y (range y (+ y hoogte))
          z (range z (+ z lengte))]
    (set-block-type (world (server/player))
                    {:x x :y y :z z}
                    (get materials
                         materiaal))))

(doseq [[m] (shuffle (seq materials))]
  (bukkit/inventory-add (server/player) m))

(bukkit/empty-inventory (server/player))

(defn fly! []
  (.setAllowFlight (server/player) true)
  (.setFlying (server/player) true))

(fly!)

(datafy(location (server/player)))

(.isFlying (server/player))

(datafy (location (server/player)))

(defn update-location [entity f & args]
  (let [l (location (apply f (datafy (location entity)) args))]
    (.teleport entity
               l
               org.bukkit.event.player.PlayerTeleportEvent$TeleportCause/PLUGIN)))

(future
  (dotimes [i 1000]
    (update-location (server/player) update :yaw inc)
    (Thread/sleep 50)))

(fly!)
(update-location (server/player) update :y + 1)

(datafy (location (server/player)))
{:x -470, :y 75, :z 131, :yaw 98.16312, :pitch 4.5000563, :world #object[net.glowstone.GlowWorld 0x171c38da "GlowWorld(name=world)"]}
{:x -470, :y 75, :z 131, :yaw 98.16312, :pitch 4.5000563, :world #object[net.glowstone.GlowWorld 0x171c38da "GlowWorld(name=world)"]}
(defn verwijder-huis [{:keys [x y z] :as locatie}
                      {:keys [hoogte breedte lengte]}]
  (kubus locatie
         {:hoogte hoogte
          :breedte breedte
          :lengte lengte}
         :air))

(defn maak-huis [{:keys [x y z] :as locatie}
                 {:keys [hoogte breedte lengte]}]
  (kubus locatie {:hoogte hoogte :breedte breedte :lengte lengte} :acacia-stairs)
  (kubus {:x (inc x)
          :y (inc y)
          :z (inc z)}
         {:hoogte (- hoogte 2) :breedte (- breedte 2) :lengte (- lengte 2)} :air)
  #_(kubus {:x (inc x)
            :y (inc y)
            :z (inc z)}
           {:hoogte 10 :breedte (- breedte 2) :lengte 5} :sand)
  #_(kubus {:x (inc x)
            :y (inc y)
            :z (+ (inc z) 5)}
           {:hoogte 10 :breedte (- breedte 2) :lengte (- lengte 2 5)} :water)

  )

(.spawnEntity (world (server/player))
              (location (merge
                         (datafy (location (server/player)))
                         {:x (+ -471 5)
                          :y (+ 63 15)
                          :z (+ 129 13)}))
              (:squid entities))
materials
;; (maak-huis {:x -471, :y 63, :z 129}
;;            {:hoogte 20
;;             :breedte 15
;;             :lengte 20 })

;; (verwijder-huis {:x -471, :y 63, :z 129}
;;                 {:hoogte 20
;;                  :breedte 18
;;                  :lengte 22 })

(maak-huis {:x -766
            :y 63
            :z -59}
           {:hoogte 10
            :breedte 10
            :lengte 10})


(.setTime (world (server/player)) (+ (.getTime (world (server/player))) 3 3000))

(defn rand-loc []
  (let [loc (datafy (location (server/player)))]
    (location (-> loc
                  (update :x + (- (rand-int 10) 5))
                  (update :y + 10)
                  (update :z + (- (rand-int 10) 5))))))

(rand-loc)

(defn spawn-random-chicken []
  (.spawnEntity (world (server/player)) (rand-loc) (:chicken entities)))

(doseq [_ (range 100)]
  (spawn-random-chicken)
  (Thread/sleep 500))

(.spawnEntity (world me) (-> me
                             location
                             datafy
                             (update :x - 5)
                             (update :z - 2)
                             location) #_(rand-loc) org.bukkit.entity.EntityType/CHICKEN)

(= server (org.bukkit.Bukkit/getServer))

(.getGameMode me)
