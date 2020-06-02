(ns gustav.react-native
  #?(:clj (:require [clojure.string :as str]
                    [net.cgrand.macrovich :as macrovich]
                    [clojure.string :as str]
                    gustav.core
                    gustav.to-js)
     :cljs (:require [net.cgrand.macrovich :as macrovich]
                     [clojure.string :as str]
                     gustav.class-name
                     js-libs.react-components))

  #?(:cljs (:require-macros [gustav.react-native :refer [dump-rn-styles!]]))

  #?(:clj (:import (cljs.tagged_literals JSValue))))


;; TODO FIXME this is very much not self-hosted CLJS friendly :I
#?(:clj
   (do
     (defn rn-optimized-style-vec [style-coll static]
       (mapv (fn [style-kv]
               (gustav.core/abort-on-missing-style style-coll style-kv)
               (list 'js*
                     "gustav.react_native.sheet[gustav.class_name.generator(~{})]"
                     (gustav.core/stringify-style-kv style-kv)))
             static))

     (defn make-rn-aot-xform
       "Provides a function that will resolve static styles according to the input
  `style-coll` and output react-native specific style declarations.

   `style-coll` may be a set or a map, or whatever that supports `get`."
       [style-coll]
       (fn [[tag
             {:as props :keys [style
                               sst static-style
                               dst dynamic-style]}
             & others
             :as hiccup-form]]

         (let [{:keys [static dynamic]} (gustav.core/decant-styles hiccup-form)]
           (if (or static dynamic)
             (let [rn-style
                   (let [static-keys (rn-optimized-style-vec style-coll
                                                             static)
                         style-vec (cond-> static-keys
                                     dynamic (conj dynamic))]
                     (if (= 1 (count style-vec))
                       (first style-vec)
                       style-vec))
                   props' (-> props
                              (dissoc :sst :static-style :dst :dynamic-style)
                              (assoc :style rn-style))]
               (into [tag props'] others))
             hiccup-form))))



     (comment
       (def rn-aot-xform (make-rn-aot-xform #{[:margin 42]}))
       (def rn-aot-xform (make-rn-aot-xform #{[:margin 42] [:color "plop"]}))

       (rn-aot-xform [:View {:style {:margin 42}}])
       (rn-aot-xform [:View {}])
       (rn-aot-xform [:View {:style '(do {})}])
       (rn-aot-xform [:View {:style {}}])
       (rn-aot-xform [:View {:style {:color 'bim}}])
       (rn-aot-xform [:View {:style {:margin 42
                                     :background-color 'bim}}])
       (rn-aot-xform [:View {:sst {:color "plop"}
                             :dst {:margin 42}}])


       (def plop {:margin 42 :background-color "red"})

       (rn-aot-xform [:View {:style 'plop}])

       )

     (defn rn-dev-xform
       "Outputs dev-time unoptimized style declarations, that are usable after code is hot-loaded"
       [[tag
         {:as props :keys [style
                           sst static-style
                           dst dynamic-style]}
         & others
         :as hiccup-form]]
       ;; (clojure.pprint/pprint hiccup-form)
       ;; (clojure.pprint/pprint (some-> hiccup-form decant-styles))
       ;; (clojure.pprint/pprint (some-> hiccup-form decant-styles style-transform-fn))

       (let [{:keys [static dynamic]} (gustav.core/decant-styles hiccup-form)]
         (if (or static dynamic)
           (let [v (cond-> []
                     static (conj (gustav.cases/map-enforce-camel static))
                     dynamic (conj dynamic))
                 rn-style (if (= 1 (count v))
                            (first v)
                            v)
                 props' (-> props
                            (dissoc :sst :static-style :dst :dynamic-style)
                            (assoc :style rn-style))]
             (into [tag props'] others))
           hiccup-form)))



     (comment

       (rn-dev-xform [:View {:style {:margin 42}}])
       (rn-dev-xform [:View {}])
       (rn-dev-xform [:View {:style '(do {})}])
       (rn-dev-xform [:View {:style {}}])
       (rn-dev-xform [:View {:style {:color 'bim}}])
       (rn-dev-xform [:View {:style {:margin 42
                                     :background-color 'bim}}])
       (rn-dev-xform [:View {:sst {:color "plop"}
                             :dst {:margin 42}}])


       (def plop {:margin 42 :background-color "red"})

       (rn-dev-xform [:View {:style 'plop}])

       )


     (comment
       (count @gustav.core/*style-frequencies)
       (count @gustav.core/*aot-styles)
       )


     (defn raw-static-style [sst]
       (-> sst
           gustav.core/eval-static-style
           gustav.core/collect-static-style!))

     (defn static-style [sst]
       (let [evaluated-style (gustav.core/eval-static-style sst)]
         ;; We need to manually convert to JS here as this macro will be
         ;; called to handle styles out of the reach of hicada.
         (gustav.to-js/to-js
          (if-let [aot-styles @gustav.core/*aot-styles]
            (rn-optimized-style-vec aot-styles
                                    evaluated-style)
            (gustav.cases/map-enforce-camel evaluated-style)))))


     (comment
       (gustav.to-js/to-js {:plop 1})

       (static-style {:plop  1})

       )






     (defn prep-rn-styles [style-kvs]
       (->> style-kvs
            (map (fn [style-kv]
                   (list 'js*
                         "gustav.react_native.raw_styles[gustav.class_name.generator(~{})] = ~{}"
                         (gustav.core/stringify-style-kv style-kv)
                         (gustav.to-js/to-js
                          {(-> style-kv key gustav.cases/kebab->camel-k)
                           (val style-kv)}))))


            ;; (take 3)

            (list* 'do (list 'js* "gustav.react_native.raw_styles = {}"))))


     (comment
       (clojure.pprint/pprint (prep-rn-styles (keys @gustav.core/*style-frequencies)))
       )

     (defmacro dump-rn-styles! []
       (prep-rn-styles (keys @gustav.core/*aot-styles)))



     ))



;; This only works if the style collection already happened, via macro-expansion
;; in clojure (not clojurescript) compilation.
;; This var should be empty in clojure, and filled in clojurescript.

#?(:cljs
   (do (dump-rn-styles!)
       (js* "gustav.react_native.sheet = js_libs.react_components.raw_sst(gustav.react_native.raw_styles)")))

