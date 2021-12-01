(ns lambdaisland.witchcraft.reflect
  (:require [lambdaisland.classpath :as licp])
  (:import (org.reflections Reflections Store)
           (org.reflections.util ConfigurationBuilder
                                 ClasspathHelper
                                 FilterBuilder)
           (javassist.bytecode MethodInfo)
           (java.lang.reflect Modifier)
           (org.reflections.vfs Vfs Vfs$File)
           (org.reflections.adapters MetadataAdapter)
           (org.reflections.scanners Scanner
                                     AbstractScanner
                                     TypeElementsScanner
                                     SubTypesScanner
                                     MethodParameterScanner)))

(set! *warn-on-reflection* true)

(def packages
  ["org.bukkit"
   "net.kyori"
   #_"net.minecraft"
   "co.aikar"
   "com.destroystokyo"
   "io.papermc"
   "org.spigotmc"
   "net.glowstone"])

(defn assoc-sig [m sig klassname]
  (assoc! m sig ((fnil conj #{}) (get m sig) klassname)))

(defn classloaders []
  ;; shenanigans because paper's pluginclassloader hides what its parents
  ;; provide
  (let [plugin-loader (when-let [instance-var (resolve 'lambdaisland.witchcraft.plugin/instance)]
                        (.getClassLoader (.getClass ^Object @@instance-var)))]
    (cond-> [(licp/context-classloader)]
      plugin-loader
      (conj plugin-loader
            (.getParent plugin-loader)))))

(defn reflect-config []
  (let [config (ConfigurationBuilder.)
        loaders (into-array ClassLoader (classloaders))]
    (doseq [pkg packages]
      (.addUrls config (ClasspathHelper/forPackage pkg loaders)))
    config))

(defn load-reflections []
  (let [config (reflect-config)
        files (mapcat #(.getFiles (Vfs/fromURL %)) (.getUrls config))
        adapt (.getMetadataAdapter config)]
    (persistent!
     (reduce
      (fn [m ^Vfs$File file]
        (if (.acceptsInput adapt (.getRelativePath file))
          (let [klass (.getOrCreateClassObject adapt file)
                klassname (.getClassName adapt klass)]
            (if-not (some #(.startsWith klassname %) packages)
              m
              (reduce
               (fn [m method]
                 (if (.isPublic adapt method)
                   (-> m
                       (assoc-sig
                        (str
                         (when (Modifier/isStatic (.getAccessFlags ^MethodInfo method))
                           "static ")
                         (.getMethodName adapt method)
                         "("
                         (apply str
                                (interpose ","
                                           (.getParameterNames adapt method)))
                         ")")
                        klassname)
                       (assoc-sig
                        (str
                         (when (Modifier/isStatic (.getAccessFlags ^MethodInfo method))
                           "static ")
                         (.getReturnTypeName adapt method)
                         " "
                         (.getMethodName adapt method)
                         "("
                         (apply str
                                (interpose ","
                                           (.getParameterNames adapt method)))
                         ")")
                        klassname))
                   m))
               m
               (.getMethods adapt klass))))
          m))
      (transient {})
      files))))

(defonce reflections
  (delay (load-reflections)))

(defmacro extend-signatures
  {:style/indent [1 :form [1]]}
  [protocol & sig-impl]
  `(extend-protocol ~protocol
     ~@(as-> sig-impl $
         (for [[sig impl] (partition 2 $)
               klass (get @reflections sig)]
           {:class (symbol klass)
            :fntail impl})

         (group-by :class $)

         (for [[klass impls] $
               form (cons klass (map :fntail impls))]
           form))))

(comment
  (filter #(.contains (key %) "Lore") @reflections)
  (load-reflections)
  )
