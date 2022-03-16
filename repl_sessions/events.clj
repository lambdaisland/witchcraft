(ns repl-sessions.events
  (:require [lambdaisland.witchcraft :as wc]
            [clojure.java.classpath :as cp]
            [clojure.string :as str])
  (:import (java.io File)
           (java.util.jar JarFile JarEntry)
           (java.net URL URLClassLoader)))

(wc/start-glowstone!)

(.getSeed (wc/world "world"))

(wc/listen! :player-interact
            ::get-event
            (fn [e]
              (when (= (:right-click-block wc/block-actions) (:action e))
                (def event e))))

(:clickedBlock event)

(wc/set-block (wc/add (wc/location (:clickedBlock event)) {:y -1}) :acacia-door 8)

(require '[clojure.reflect :as r])

(r/reflect (first (cp/classpath-jarfiles)))

(.getName (rand-nth (iterator-seq (.entries (first (cp/classpath-jarfiles))))))


(sequence
 (comp
  (mapcat #(iterator-seq (.entries ^JarFile %)))
  (map #(.getName ^JarEntry %))
  (filter #(re-find #"(bukkit|paper|spigot).*event.*Event\.class" %))
  (map #(-> % (str/replace #"/" ".") (str/replace #".class$" ""))))
 (cp/classpath-jarfiles))
