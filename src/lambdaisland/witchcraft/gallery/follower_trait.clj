(ns lambdaisland.witchcraft.gallery.follower-trait
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.citizens :as c]))

;; An example of the traits system, here I define a new trait, :follower, for an
;; NPC which follows another entity (e.g. a player). Once they are at least
;; `:min-dist` away from the entity they `:follow`, they will navigate towards
;; that entity, until they are at most `:max-dist` blocks away.

(defn init! []
  (c/make-trait
   ;; Name of the trait
   :follower
   ;; Map of callback and config
   {;; Initial value of the trait's state, for each instance. This state is kept
    ;; and automatically persisted.
    :init {:follow nil
           :min-dist 4
           :max-dist 2
           :following? false}
    ;; Run hook where we periodically check if we should navigate or not, we get
    ;; passed the trait instance, which we can deref to get the current state.
    :run
    (fn [this]
      (let [{:keys [min-dist max-dist follow following? offset]} @this
            do-nav #(c/navigate-to this (cond-> (wc/location follow)
                                          offset (wc/add offset)))]
        (when (and (c/spawned? (c/npc this)) follow) ; Kicks in as soon as we are following someone
          (if following?
            (if (< max-dist (wc/distance (c/npc this) follow))
              (do-nav)
              (c/stop-navigating this))
            (when (< min-dist (wc/distance (c/npc this) follow))
              (do-nav))))))}))

(comment
  (def jonny (create-npc :player "jonny"))
  ;; or
  (def jonny (c/npc-by-id 0))

  (c/update-traits
   jonny
   {:follower {:follow (wc/player "sunnyplexus")
               :min-dist 4
               :max-dist 1}})

  (c/npc-trait (c/npc-by-id 0) :follower)

  (c/remove-trait jonny :follower))
