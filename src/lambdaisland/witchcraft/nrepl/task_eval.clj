(ns lambdaisland.witchcraft.nrepl.task-eval
  "Schedule all nREPL eval operations on the server thread.

  This way you don't have to wrap a lot of operations in a [[wc/run-task]]."
  (:require [lambdaisland.witchcraft :as wc]
            [nrepl.middleware :as middleware :refer [set-descriptor!]]
            [nrepl.misc :as misc :refer [response-for]]
            [nrepl.transport :as t]))

(defn run-task [thunk session]
  (wc/run-task
   #(if-let [whoami (and (not wc/*default-world*)
                         (:witchcraft/whoami (meta session)))]
      (let [player (wc/player whoami)
            world (wc/world player)]
        (binding [wc/*default-world* world]
          (.run thunk)))
      (.run thunk))))

(defn wrap-eval
  [h]
  (fn [{:keys [op session] :as msg}]
    (when (and (:exec (meta session))
               (not (::decorated (meta session))))
      (alter-meta! session
                   (fn [session-meta]
                     (-> session-meta
                         (update :exec
                                 (fn [exec]
                                   (fn [id thunk ack]
                                     (exec id
                                           (partial run-task thunk session)
                                           ack))))
                         (assoc ::decorated true)))))
    (h msg)))

(set-descriptor! #'wrap-eval
                 {:requires #{"clone"}
                  :expects  #{"eval"}
                  :handles  {}})

(comment
  (doseq [sess (vals @@#'nrepl.middleware.session/sessions)]
    (alter-meta! sess assoc :witchcraft/whoami "sunnyplexus"))
  )
