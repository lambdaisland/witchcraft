(ns dependencies
  (:require [lambdaisland.classpath :as licp]))

(licp/update-classpath! {:extra {:deps '{org.reflections/reflections {:mvn/version "0.9.12"}}}})
