(ns plop.ui.base
  (:require hicada.compiler
            gustav.core
            gustav.web
            plop.ui.aot-styles
            [hiccup.core :as hiccup]))



(defmacro e [body]
  (list
   'hiccup.core/html
   (if-let [aot-styles @gustav.core/*aot-styles]
     (hicada.compiler/compile body
                              {:server-render? true
                               :transform-fn (gustav.web/make-web-aot-xform aot-styles)
                               :emit-fn (fn [a b c]
                                          (into [a b] c))}
                              &env)
     (hicada.compiler/compile body
                              {:server-render? true
                               :transform-fn #(-> %
                                                  gustav.core/raw-collector-xform
                                                  gustav.web/dev-xform)
                               :emit-fn (fn [a b c]
                                          (into [a b] c))}
                              &env))))




(comment

  (macroexpand-1 '(e [:p {:style {:color "red"}} "PLOP"]))

 @gustav.core/*style-frequencies

 (reset! gustav.core/*style-frequencies nil)


 (reset! gustav.core/*aot-styles
         (gustav.web/style-frequencies-to-aot-classes @gustav.core/*style-frequencies))

 (reset! gustav.core/*aot-styles nil)

 (macroexpand-1 '(e [:p {:style {:color "red"}} "PLOP"]))

 (hiccup.core/html (e [:p {:style {:color "blue"}} "PLOP"]))



 (hiccup/html [:style (gustav.web/css-class-declarations @gustav.core/*aot-styles)])



  )
