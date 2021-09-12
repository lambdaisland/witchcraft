(ns auto-schedule)

;; Experiment to send certain functions always through `run-task`. Turns out
;; this leads to deadlock, leaving here for possible future reference.

(defn auto-schedule-tasks! []
  (doseq [var (filter (comp :scheduled meta) (vals (ns-publics (the-ns 'lambdaisland.witchcraft))))]
    (when-not (:undecorated (meta var))
      (prn "decorating" var)
      (alter-meta! var assoc :undecorated @var)
      (alter-var-root var (fn [f]
                            (fn [& args]
                              (let [p (promise)]
                                (run-task #(deliver
                                            p
                                            (try
                                              (apply f args)
                                              (catch Throwable t
                                                t))))
                                (let [res @p]
                                  (if (instance? Throwable res)
                                    (throw res)
                                    res)))))))))
