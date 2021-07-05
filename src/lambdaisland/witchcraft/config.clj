(ns lambdaisland.witchcraft.config
  "Create and handle Glowstone ServerConfig instances

  Generally to configure Glowstone you edit YAML files, but that's not very
  Clojure-y. This config munging below allows you to define config values as
  inline EDN. The values in the config map that you provide to [[data-config]]
  or [[server-config]] are keywords derived from the `ServerConfig$Key` enum,
  see `(keys config-keys)`"
  (:require [clojure.java.io :as io]
            [lambdaisland.witchcraft.util :as util])
  (:import [net.glowstone.util.config ServerConfig ServerConfig$Key]))

(set! *warn-on-reflection* true)

(def config-keys
  "Mapping from Clojure keyword to `ServerConfig$Key` enum entry."
  (util/enum->map ServerConfig$Key))

(defn config-params
  "Convert a map using Clojure keywords to use `ServerConfig$Key` keys, the way
  `ServerConfig` expects."
  [opts]
  (into {} (comp
            (map (juxt (comp config-keys key) val))
            (remove (comp nil? first))) opts))

(def key-data
  "All data encoded in the `ServerConfig$Key` enum as plain data."
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
  (assoc (into {} (map (juxt key (comp :def val))) key-data)
         ServerConfig$Key/SHUTDOWN_MESSAGE "Server shutting down."))

(defn data-config
  "Creates an in-memory config based on the passed in data.

  Makes sure all defaults are explicitly present in the map passed to
  ServerConfig, and overrides load/save to be no-ops."
  [opts]
  (let [config-map (merge default-params (config-params opts))]
    (proxy [ServerConfig] [(io/file (:config-dir opts "config"))
                           (io/file (:config-dir opts "config")
                                    (:config-file opts "glowstone.yml"))
                           config-map]
      (load [])
      (save []))))

(defn server-config
  "Create a ServerConfig backed by a YAML file, takes the `:config-dir` and
  `:config-file` (YAML), and optionally other config keys. Defaults to
  `config/glowstone.yml`"
  ([{:keys [config-dir config-file]
     :or {config-dir "config"
          config-file "glowstone.yml"}
     :as config}]

   (ServerConfig. (io/file config-dir)
                  (io/file config-dir config-file)
                  (config-params config))))
