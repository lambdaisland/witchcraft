(ns lambdaisland.witchcraft.nrepl.task-eval
  "Schedule all nREPL eval operations on the server thread.

  This way you don't have to wrap a lot of operations in a [[wc/run-task]]."
  (:require [lambdaisland.witchcraft :as wc]
            [nrepl.middleware :as middleware :refer [set-descriptor!]]
            [nrepl.misc :as misc :refer [response-for]]
            [nrepl.transport :as t]))

(defn scheduled-exec [session]
  (fn [id ^Runnable thunk ^Runnable ack]
    (wc/run-task
     #(if-let [whoami (and (not wc/*default-world*)
                           (:witchcraft/whoami (meta session)))]
        (let [player (wc/player whoami)
              world (wc/world player)]
          (binding [wc/*default-world* world]
            (.run thunk)
            (.run ack)))
        (do
          (.run thunk)
          (.run ack))))))

(defn wrap-eval
  [h]
  (fn [{:keys [op session] :as msg}]
    (when (and session
               (:exec (meta session))
               (not (::decorated (meta session))))
      (alter-meta! session
                   (fn [session-meta]
                     (-> session-meta
                         (assoc :exec (scheduled-exec session))
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
