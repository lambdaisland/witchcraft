(ns lambdaisland.witchcraft.util
  (:require [clojure.string :as str]))

(defn enum->map [enum]
  (into {}
        (map (juxt #(keyword (str/replace (str/lower-case (.name %)) "_" "-")) identity))
        (java.util.EnumSet/allOf enum)))
