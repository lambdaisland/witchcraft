(ns lambdaisland.witchcraft.classpath-hacks
  "We do some classpath scanning stuff both
  in [[lambdaisland.witchcraft.reflect]] (which much of the interop is based
  on), and in [[lambdaisland.witchcraft.event]] (to find out which events are
  supported by the running system)"
  (:require [clojure.string :as str]
            [lambdaisland.classpath :as licp]))

;; We'll make this configurable eventually, in particular from the
;; witchcraft-plugin config, but for now this will do.
(def tracer-classes
  "Classes that are in jars that we care about"
  ["org.bukkit.Bukkit"
   "net.citizensnpcs.api.CitizensAPI"])

(defn class-resource [classname]
  (try
    (-> classname
        Class/forName
        .getClassLoader
        (.getResource (str (str/replace classname #"\." "/") ".class")))
    (catch ClassNotFoundException _)))

(defn inject-jar-by-class!
  "Given a classname (string), find the `.class` file that defines it, then find
  the JAR that contains it, then add that JAR to the root classloader (i.e. the
  bottom-most clojure.lang.DynamicClassloader, which sits above the
  PluginClassloader). This ensures that any code that deals with the classpath
  can see the classes in these jars."
  [klz]
  (.addURL (licp/root-loader)
           (some-> klz
                   class-resource
                   (str/replace #"!/.*" "")
                   (str/replace #"^jar:" "")
                   java.net.URL.)))

(defonce inject-jars! (run! inject-jar-by-class! tracer-classes))
