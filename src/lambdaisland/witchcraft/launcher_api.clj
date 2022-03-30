(ns lambdaisland.witchcraft.launcher-api
  (:require [clojure.java.io :as io]
            [progrock.core :as pr])
  (:import (sk.tomsik68.mclauncher.backend MinecraftLauncherBackend)))

(defn launcher-backend ^MinecraftLauncherBackend [file]
  (MinecraftLauncherBackend. (io/file file)))

(defn version-list
  ([]
   (version-list (launcher-backend "/tmp")))
  ([backend]
   (.getVersionList backend)))

(defn latest-snapshot [backend]
  (.getLatestSnapshot
   (.getLatestVersionInformation
    backend)))

(defn debounce [f ms]
  (let [latest (atom (System/currentTimeMillis))]
    (fn []
      (let [now (System/currentTimeMillis)]
        (when (< (+ @latest ms) now)
          (reset! latest now)
          (f))))))

(defn progress-monitor []
  (let [progress (atom (pr/progress-bar 1))
        message (atom nil)
        pr-bar (debounce (fn []
                           (pr/print @progress {:length 20
                                                :format (format "%17s :percent%% [:bar] %s"
                                                                (str (:progress @progress)
                                                                     "/"
                                                                     (:total @progress))
                                                                @message)}))
                         ;; progress bar does not need more than 20fps
                         50)]
    (reify sk.tomsik68.mclauncher.api.ui.IProgressMonitor
      (setProgress [_ amount]
        (swap! progress assoc :progress amount)
        (pr-bar))
      (setMax [_ len]
        (reset! progress (pr/progress-bar len))
        (pr-bar))
      (incrementProgress [_ amount]
        (swap! progress pr/tick amount)
        (pr-bar))
      (setStatus [_ status]
        (reset! message (subs status 0 (min (count status) 75)))
        (print "\r" (apply str (repeat 120 " ")))
        (pr-bar)))))

(defn update-minecraft [backend version]
  (.setLevel sk.tomsik68.mclauncher.api.common.MCLauncherAPI/log java.util.logging.Level/OFF)
  (.updateMinecraft backend version (progress-monitor)))

(defn session [{:keys [username session-id uuid type properties]
                :or {type sk.tomsik68.mclauncher.api.login.ESessionType/MOJANG}}]
  (reify sk.tomsik68.mclauncher.api.login.ISession
    (getUsername [_] username)
    (getSessionID [_] session-id)
    (getUUID [_] uuid)
    (getType [_] type)
    (getProperties [_] properties)))

(defn launch-cmd [backend session version]
  (.command (.launchMinecraft backend session version)))
