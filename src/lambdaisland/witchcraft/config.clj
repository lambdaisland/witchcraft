(ns lambdaisland.witchcraft.config
  "Create and handle Glowstone ServerConfig instances"
  (:require [clojure.java.io :as io]
            [lambdaisland.witchcraft.util :as util])
  (:import [net.glowstone.util.config ServerConfig ServerConfig$Key]))

(set! *warn-on-reflection* true)

(def config-keys
  (util/enum->map ServerConfig$Key))

(defn config-params [opts]
  (into {} (comp
            (map (juxt (comp config-keys key) val))
            (remove (comp nil? first))) opts))

(def key-data
  (let [ks (into {}
                 (map (juxt identity #(util/accessible-field ServerConfig$Key (name %))))
                 [:path :def :migrate :migratePath :validator])]
    (into {}
          (for [k (java.util.EnumSet/allOf ServerConfig$Key)]
            [k
             (into {}
                   (map (juxt key #(.get ^java.lang.reflect.Field (val %) k)))
                   ks)]))))

(def default-params
  (into {} (map (juxt key (comp :def val))) key-data))

(defn data-config
  "Creates an in-memory config based on the passed in data."
  [opts]
  (let [config-map (merge default-params (config-params opts))]
    (proxy [ServerConfig] [(io/file "")
                           (io/file "" "")
                           config-map]
      (load [])
      (save []))))

(defn server-config
  "Create a ServerConfig, takes the `:config-dir` and `:config-file` (YAML), and
  optionally other config keys."
  ([{:keys [config-dir config-file]
     :or {config-dir "config"
          config-file "glowstone.yml"}
     :as config}]

   (ServerConfig. (io/file config-dir)
                  (io/file config-dir config-file)
                  (config-params config))))
