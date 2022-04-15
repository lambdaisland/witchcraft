(ns lambdaisland.witchcraft.reflect
  (:require [clojure.string :as str]
            [lambdaisland.witchcraft.classpath-hacks :as classpath-hacks])
  (:import (org.reflections Reflections Store)
           (org.reflections.util ConfigurationBuilder
                                 ClasspathHelper
                                 FilterBuilder)
           (javassist.bytecode MethodInfo ClassFile)
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
   "net.glowstone"
   "net.citizensnpcs"])

(defn assoc-sig [m sig klassname]
  (assoc! m sig ((fnil conj #{}) (get m sig) klassname)))

(defn classloaders []
  ;; shenanigans because paper's pluginclassloader hides what its parents
  ;; provide
  (let [plugin-loader (when-let [instance-var (resolve 'lambdaisland.witchcraft.plugin/instance)]
                        (.getClassLoader (.getClass ^Object @@instance-var)))]
    (cond-> [(classpath-hacks/context-classloader)]
      plugin-loader
      (conj plugin-loader
            (.getParent plugin-loader)))))

(defn reflect-config ^ConfigurationBuilder []
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
          (let [^ClassFile classfile (.getOrCreateClassObject adapt file)
                klassname (.getClassName adapt classfile)]
            (if-not (and (Modifier/isPublic (.getAccessFlags classfile))
                         (some #(.startsWith klassname %) packages))
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
               (.getMethods adapt classfile))))
          m))
      (transient {})
      files))))

(defonce reflections
  (delay (load-reflections)))

(defn exclude-descendants
  "We don't extend a protocol to a type if a supertype of the type already
  implements the given method, this mainly means that we extend Bukkit interface
  like Block, and not concrete implementations, like CraftBlock, unless the
  latter has methods we care about that are not provided by an interface."
  [klasses]
  (remove
   (fn [klass]
     (try
       (some (set klasses) (map (memfn ^Class getName)
                                (ancestors (Class/forName klass))))
       (catch Error _
         true)))
   klasses))

(defmacro extend-signatures
  {:style/indent [1 :form [1]]}
  [protocol & sig-impl]
  `(extend-protocol ~protocol
     ~@(as-> sig-impl $
         (for [[sig impl] (partition 2 $)
               klass (exclude-descendants (get @reflections sig))]
           {:class (symbol klass)
            :fntail impl})

         (group-by :class $)

         (for [[klass impls] $
               form (cons klass (map :fntail impls))]
           form))))

(comment
  (filter #(re-find #"openInventory\(" (key %)) @reflections)

  (exclude-descendants
   (get @reflections "getInventory()"))

  (ancestors (Class/forName "org.bukkit.entity.HumanEntity"))
  (load-reflections)

  (clojure.reflect/reflect org.bukkit.inventory.Inventory)
  )
