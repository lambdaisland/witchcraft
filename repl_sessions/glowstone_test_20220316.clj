(ns repl-sessions.glowstone-test-20220316
  (:require [lambdaisland.witchcraft :as wc]
            [clojure.java.classpath :as cp]
            [clojure.string :as str])
  (:import (java.io File)
           (java.util.jar JarFile JarEntry)
           (java.net URL URLClassLoader)))

#_
(wc/start-glowstone!)

(.getSeed (wc/world "world"))

(wc/set-game-rules {:do-daylight-cycle false} )

(wc/set-time 0)

(wc/add-inventory (wc/player) :firework-rocket 64)

(wc/item-stack :firework 64)

(wc/xmaterial :firework-rocket)

(wc/entity-types )
(wc/materials )
