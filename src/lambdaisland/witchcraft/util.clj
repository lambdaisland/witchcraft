(ns lambdaisland.witchcraft.util
  (:require [clojure.string :as str]))

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

(defn dasherize
  "Converts an underscored or camelized string
  into an dasherized one."
  [s]
  (when s
    (-> s
        (str/replace #"([A-Z][a-z]+)" (fn [[match c]]
                                        (if c (str "-" (str/lower-case c)) "")))
        (str/replace #"([A-Z]+)" "-$1")
        (str/replace #"[-_\s]+" "-")
        (str/replace #"^-" "")
        (str/replace #"-$" ""))))

(defmacro when-class-exists [classname & body]
  (try
    (Class/forName (str classname))
    `(do ~@body)
    (catch ClassNotFoundException e)))

(defmacro if-class-exists [classname then else]
  (try
    (Class/forName (str classname))
    then
    (catch ClassNotFoundException e
      else)))
