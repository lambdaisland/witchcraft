(ns dependencies
  (:require [lambdaisland.classpath :as licp]))

(licp/update-classpath! {:extra {:deps '{org.reflections/reflections {:mvn/version "0.9.12"}}}})
(licp/git-pull-lib "/home/arne/github/lambdaisland/witchcraft/deps.edn" 'com.lambdaisland/classpath)
(licp/update-classpath! {})

(require 'lambdaisland.classpath :reload)

(licp/classpath-chain)
