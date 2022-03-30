(ns repl-sessions.launcher-api
  (:require [clojure.java.io :as io]
            [progrock.core :as pr]))

(def progress (atom nil))

(.getVersionList
 (sk.tomsik68.mclauncher.backend.MinecraftLauncherBackend. (io/file "/tmp")))

(.getLatestSnapshot
 (.getLatestVersionInformation
  (sk.tomsik68.mclauncher.backend.MinecraftLauncherBackend. (io/file "/tmp"))))

(sk.tomsik68.mclauncher.impl.login.legacy.LegacySession. "user" "sessid" "uuid" "dlTicket" "1.18.2")


(def backend (sk.tomsik68.mclauncher.backend.MinecraftLauncherBackend. (io/file "/tmp/mc")))

(.updateMinecraft
 backend "1.18.2"
 (reify sk.tomsik68.mclauncher.api.ui.IProgressMonitor
   (setProgress [_ amount]
     (swap! progress assoc :progress amount)
     (pr/print @progress))
   (setMax [_ len]
     (reset! progress (pr/progress-bar len))
     (pr/print @progress))
   (incrementProgress [_ amount]
     (swap! progress pr/tick amount)
     (pr/print @progress))
   (setStatus [_ status]
     (println (str "\r" status)))))

(.command(.launchMinecraft
          backend
          (reify sk.tomsik68.mclauncher.api.login.ISession
            (getUsername [_] "plexus")
            (getSessionID [_] "123")
            (getUUID [_] (str (java.util.UUID/randomUUID)))
            (getType [_] sk.tomsik68.mclauncher.api.login.ESessionType/MOJANG)
            (getProperties [_] nil))
          "1.18.2"
          ))
