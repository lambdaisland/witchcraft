(ns lambdaisland.witchcraft.util
  (:require [clojure.string :as str])
  (:import (org.bukkit.util Vector)))

(set! *warn-on-reflection* true)

(defn enum->map [enum]
  (into {}
        (map (juxt #(keyword (str/replace (str/lower-case (.name ^Enum %)) "_" "-")) identity))
        (java.util.EnumSet/allOf enum)))

(defn accessible-field ^java.lang.reflect.Field [^Class klz field]
  (doto (.getDeclaredField klz field)
    (.setAccessible true)))

(defn set-field! [klz field obj val]
  (.set (accessible-field klz field) obj val))

(defn set-static! [klz field val]
  (set-field! klz field klz val))
