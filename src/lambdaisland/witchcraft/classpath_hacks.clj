(ns lambdaisland.witchcraft.classpath-hacks
  "We do some classpath scanning stuff both
  in [[lambdaisland.witchcraft.reflect]] (which much of the interop is based
  on), and in [[lambdaisland.witchcraft.event]] (to find out which events are
  supported by the running system)"
  (:require [clojure.string :as str]))

(defn context-classloader
  "Get the context classloader for the current thread"
  ^ClassLoader
  ([]
   (context-classloader (Thread/currentThread)))
  ([^Thread thread]
   (.getContextClassLoader thread)))

(defn base-loader
  "Get the loader that Clojure uses internally to load files

  This is usually the current context classloader, but can also be
  clojure.lang.Compiler/LOADER."
  ^ClassLoader []
  (clojure.lang.RT/baseLoader))

(defn dynamic-classloader?
  "Is the given classloader a [[clojure.lang.DynamicClassLoader]]"
  [cl]
  (instance? clojure.lang.DynamicClassLoader cl))

(defn priority-classloader?
  "Is the given classloader a [[priority-classloader]]"
  [cl]
  (when-let [name (and cl (.getName ^ClassLoader cl))]
    (str/starts-with? name "lambdaisland/priority-classloader")))

(defn root-loader
  "Find the bottom-most DynamicClassLoader in the chain of parent classloaders"
  ^DynamicClassLoader
  ([]
   (root-loader (base-loader)))
  ([^ClassLoader cl]
   (when cl
     (loop [loader cl]
       (let [parent (.getParent loader)]
         (cond
           (or (dynamic-classloader? parent)
               (priority-classloader? parent))
           (recur parent)

           (or (dynamic-classloader? loader)
               (priority-classloader? loader))
           loader))))))


;;;;;;;;;;;;;;;;;;;;

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
  (.addURL (root-loader)
           (some-> klz
                   class-resource
                   (str/replace #"!/.*" "")
                   (str/replace #"^jar:" "")
                   java.net.URL.)))

(defonce inject-jars! (run! inject-jar-by-class! tracer-classes))
