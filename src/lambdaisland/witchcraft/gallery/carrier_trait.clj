(ns lambdaisland.witchcraft.gallery.carrier-trait
  "A trait for NPCs that are able to carry stuff around for you. Right click to
  open their inventory and put stuff in it."
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.citizens :as c]
            [lambdaisland.witchcraft.events :as e]))

(defn init! []
  (c/make-trait
   :carrier
   {:init {:inventory (wc/make-inventory {:capacity 18})}})

  (e/listen!
   :citizens/npcright-click
   ::carrier
   (fn [e]
     (when (c/has-trait? (:NPC e) :carrier)
       (wc/open-inventory
        (:clicker e)
        (:inventory @(c/npc-trait (:NPC e) :carrier)))))))

(comment
  (def jonny (create-npc :player "jonny"))
  ;; or
  (def jonny (c/npc-by-id 0))

  (c/update-traits jonny {:carrier {}})

  )
