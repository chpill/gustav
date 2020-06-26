(ns plop.ui.home
  (:require [plop.ui.base :as b]
            [hiccup.core :as hiccup]))



(defn home-page []
  (hiccup/html
   [:html {}
    [:head {}
     [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
     ;; Refreshes the page every second. Really annoying when using the devtools...
     #_[:meta {:http-equiv "refresh" :content "1"}]]


    ;; TODO: we need a dev-mode transform for styles!
    [:body {:style "background-color:papayawhip; text-align:center; font-family: Helvetica;"}
     [:h1 {} "PLOP"]]]))


(spit "/tmp/index.html"
      (home-page))

(comment
  )
