(ns lambdaisland.witchcraft.gallery.megachop9000
  "A magical dwarven axe which chops down a full tree trunk with one chop."
  (:require [lambdaisland.witchcraft :as wc]
            [lambdaisland.witchcraft.events :as e]
            [lambdaisland.witchcraft.markup :as markup]
            [lambdaisland.witchcraft.fill :as fill]))

(def megachop9000-name
  (delay
    (wc/normalize-text
     (markup/fAnCy "MegaChop 9000"
                   [:yellow :gold :red]))))

(defn create-megachop []
  (let [axe (wc/item-stack :diamond-axe)]
    (wc/set-display-name axe @megachop9000-name)
    (wc/set-lore axe
                 [[:italic "Even the trees shivered"]
                  [:italic
                   "when the "
                   [:bold "Themjer"] " passed."]])
    axe))

(defn give-to-player [player]
  (wc/add-inventory player (create-megachop)))

(defn register-listener! []
  (e/listen!
   :player-interact
   ::megachop-9000
   (fn [{:keys [clickedBlock player action]}]
     (when clickedBlock
       (let [material (wc/material-name clickedBlock)]
         (when (and (= @megachop9000-name (wc/display-name (wc/item-in-hand player)))
                    (re-find #"-log$" (name material)))
           (run! (memfn breakNaturally)
                 (fill/fill-xyz clickedBlock {:pred #(= material (wc/material-name %))}))))))))


(comment
  (give-to-player (wc/player "sunnyplexus"))
  (register-listener!)

  )
