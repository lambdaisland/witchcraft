(ns lambdaisland.witchcraft.markup
  "Adding color and text effects to chat messages and titles via hiccup-like
  Markup")

(def codes
  {:aqua "§b"
   :black "§0"
   :blue "§9"
   :dark-aqua "§3"
   :dark-blue "§1"
   :dark-gray "§8"
   :dark-green "§2"
   :dark-purple "§5"
   :dark-red "§4"
   :gold "§6"
   :gray "§7"
   :green "§a"
   :light-purple "§d"
   :red "§c"
   :white "§f"
   :yellow "§e"
   :obfuscated "§k"
   :bold "§l"
   :strikethrough "§l"
   :underline "§n"
   :italic "§o"
   :reset "§r"})

(defn format-seq
  "Turn a sequence of strings and keywords into a string ready for consumption by
  minecraft."
  [s]
  (apply str (map #(get codes % %) s)))

(defn expand-markup
  "Turn hiccup-like markup into the format understood by format-seq"
  [markup]
  (cond
    (string? markup)
    [markup]

    (vector? markup)
    (let [[tag & children] markup]
      (cond
        ;; Fragment, use this or lists for generic grouping
        (= :<> tag)
        (mapcat expand-markup children)

        ;; Any color tag unsets these effects, so we brute-force re-enable them
        ;; after every tag within our scope
        (#{:obfuscated :bold :strikethrough :underline :italic} tag)
        (cons tag (mapcat (fn [e]
                            (if (keyword? e)
                              [e tag]
                              [e]))
                          (mapcat expand-markup children)))

        :else
        (mapcat #(concat [tag] (expand-markup %) [:reset]) children)))

    (sequential? markup)
    (mapcat expand-markup markup)))

(defn render ^String [markup]
  (format-seq
   (expand-markup markup)))

(comment
  (format
   [:blue
    "Hello"
    [:underline " under"
     [:bold " bold"
      [:green " green"]]]


    ])
  ;; => "§9Hello§9§n under§l§n bold§a§n§l§n green"
  )
