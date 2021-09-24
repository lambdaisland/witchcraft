(in-ns 'lambdaisland.witchcraft)
(require '[lambdaisland.witchcraft.reflect :as reflect])

(def reflections (reflect/load-reflections))

(defmacro extend-signatures
  {:style/indent [1 :form [1]]}
  [protocol & sig-impl]
  `(extend-protocol ~protocol
     ~@(for [[sig impl] (partition 2 sig-impl)
             klass (get reflections sig)
             x [(symbol klass) impl]]
         x)))

(defprotocol HasWorld
  (^org.bukkit.World -world [_]))

(defprotocol HasWorld1
  (^org.bukkit.World -world1 [_ _]))

(defprotocol HasLocation
  (^org.bukkit.Location -location [_]))

(extend-signatures HasLocation
  "org.bukkit.Location getLocation()"
  (-location [this]
    (.getLocation this)))

(extend-signatures HasWorld
  "org.bukkit.World getWorld()"
  (-world [this]
    (.getWorld this)))

(extend-signatures HasWorld
  "org.bukkit.World getWorld(java.lang.String)"
  (-world1 [this obj]
    (.getWorld this obj))
  "org.bukkit.World getWorld(java.util.UUID)"
  (-world1 [this ^java.util.UUID obj]
    (.getWorld this obj)))

(comment
  (get reflections "static getWorld(java.lang.String)")
  (get reflections "org.bukkit.World getWorld(java.util.UUID)")
  (Class/forName "org.bukkit.craftbukkit.v1_17_R1.CraftServer")
  (clojure.reflect/reflect
   org.bukkit.craftbukkit.v1_17_R1.CraftServer)
  (filter #(.contains (key %) "getWorld(") reflections)

  (.getWorld org.bukkit.Bukkit "world"))
