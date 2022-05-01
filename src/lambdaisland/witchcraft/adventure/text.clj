(ns lambdaisland.witchcraft.adventure.text
  "Support for parts of the Text sublibary of the Adventure UI library. Adding
  this out of necessity since Paper has now integrated Adventure directly.

  https://docs.adventure.kyori.net/text.html"
  (:require [lambdaisland.witchcraft.markup :as markup]
            [lambdaisland.witchcraft.util :as util])
  (:import (net.kyori.adventure.text Component)
           (net.kyori.adventure.text.format NamedTextColor TextColor TextDecoration)))

(def colors
  {:gray NamedTextColor/GRAY
   :dark-purple NamedTextColor/DARK_PURPLE
   :white NamedTextColor/WHITE
   :yellow NamedTextColor/YELLOW
   :green NamedTextColor/GREEN
   :dark-red NamedTextColor/DARK_RED
   :dark-gray NamedTextColor/DARK_GRAY
   :light-purple NamedTextColor/LIGHT_PURPLE
   :gold NamedTextColor/GOLD
   :aqua NamedTextColor/AQUA
   :dark-aqua NamedTextColor/DARK_AQUA
   :red NamedTextColor/RED
   :blue NamedTextColor/BLUE
   :dark-green NamedTextColor/DARK_GREEN
   :dark-blue NamedTextColor/DARK_BLUE
   :black NamedTextColor/BLACK})

(def decorations
  {:italic TextDecoration/ITALIC
   :underline TextDecoration/UNDERLINED
   :underlined TextDecoration/UNDERLINED
   :strikethrough TextDecoration/STRIKETHROUGH
   :obfuscated TextDecoration/OBFUSCATED
   :bold TextDecoration/BOLD})

(defn component
  "Similar to [[lambdaisland.witchcraft.markup/render]], but instead of returning
  a string with special markup, it returns an Adventure Component."
  [markup]
  (let [c (Component/text)]
    (cond
      (string? markup)
      (.content c markup)
      (vector? markup)
      (let [[tag & rest] markup]
        (cond
          (contains? colors tag)
          (do
            (.color c (get colors tag))
            (run! #(.append c (component %)) rest))
          (contains? decorations tag)
          (do
            (.decorate c (get decorations tag))
            (run! #(.append c (component %)) rest)))))
    (.build c)))
