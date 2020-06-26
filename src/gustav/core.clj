(ns gustav.core
  (:require gustav.cases))


(defonce *style-frequencies (atom {}))
(defonce *aot-styles (atom nil))

(comment
  (reset! *aot-styles @*style-frequencies)

  @*style-frequencies

  )

(defn collect-static-style! [style-value]
  (do (swap! *style-frequencies
             (fn [style-frequencies style]
               (reduce (fn [acc style-statement]
                         (update acc style-statement (fnil inc 0)))
                       style-frequencies
                       style))
             style-value)
      style-value))

(defn stringify-style-kv [[style-rule style-value]]
  (str (name style-rule) "__" style-value))


(defn eval-static-style [style-form]
  (let [styles-value
        (-> style-form
            clojure.core/eval
            (try
              ;; TODO FIXME be more specific about the kind of compilation error
              ;; we want to catch (for example, do not catch syntax errors).
              (catch Exception e
                (throw (Exception. (str "style declaration is not statically defined and cannot be resolved\n"
                                        (pr-str style-form))))))

            gustav.cases/map-enforce-kebab)]

    (assert (map? styles-value))

    (not-empty styles-value)))


(defn try-eval-style-map [style]
  (try (eval-static-style style)
       ;; TODO FIXME only catch the same exceptions as above
       (catch Exception e)))


(defn try-eval-style-value [style]
  (try (eval style)
       (catch Exception e)))


(defn decant-styles
  "Given a hiccup form, separates the styles that can be resolved at compile time and the rest.
   Puts every attribute it can see in (canonical) kebab case.
   Outputs a map that may contain a `:static` map of styles, and/or `:dynamic` form"
  [[tag
    {:as props :keys [style
                      sst static-style
                      dst dynamic-style]}
    & others]]

    (assert (or (not style)
                (not (or sst static-style dst dynamic-style)))
            (str "Cannot mix classic `:style` declaration with specific static and/or dynamic ones "
                 (pr-str [tag props])))
    (assert (not (and sst static-style))
            (str "Duplicate static style declaration (using `:sst` and `:static-style` at the same time) "
                 (pr-str [tag props])))
    (assert (not (and dst dynamic-style))
            (str "Duplicate dynamic style declaration (using `:dst` and `:dynamic-style` at the same time) "
                 (pr-str [tag props])))

  (let [static-specific-style-value (eval-static-style (or sst static-style))
        dynamic-specific-style (or dst dynamic-style)]
    (if (or static-specific-style-value
            dynamic-specific-style)
      (cond-> {}
        static-specific-style-value (assoc :static static-specific-style-value)
        dynamic-specific-style (assoc :dynamic dynamic-specific-style))
      (if-let [static-generic-style-value (try-eval-style-map style)]
       {:static static-generic-style-value}
       (when (and (map? style)
                  (not-empty style))
         (-> (fn [acc k v]
               (let [successfully-evaluated-style-v (try-eval-style-value v)]
                 (if successfully-evaluated-style-v
                   (assoc-in acc
                             [:static (gustav.cases/camel->kebab-k k)]
                             successfully-evaluated-style-v)
                   (assoc-in acc [:dynamic k] v))))
             (reduce-kv {} style)))))))


;; TODO FIXME automated tests
(comment (decant-styles [:View {:style {:margin 42}}])
         (decant-styles [:View {}])
         (decant-styles [:View {:style '(do {})}])
         (decant-styles [:View {:style {}}])
         (decant-styles [:View {:style {:color 'bim}}])
         (decant-styles [:View {:style {:margin 42
                                        :background-color 'bim}}])
         (decant-styles [:View {:sst {:backgrounDcolor "plop"}
                                :dst {:marginTop 42}}])


(def plop {:margin 42 :background-color "red"})
(def plop-red "red")

(decant-styles [:View {:style 'plop}])
(decant-styles [:View {:style {:background-color 'plop-red
                               :color 'not-defined}}])

)

(defn raw-collector-xform
  "Takes a hiccup form as input, and outputs the same hiccup form but with
  separared dynamic and static styles.

  This does not output relevant markup for the browser of react-native, but it
  collect all styles it encounters."
  [[tag
    {:as props :keys [style
                      sst static-style
                      dst dynamic-style]}
    & others
    :as hiccup-form]]
  ;; (clojure.pprint/pprint hiccup-form)
  ;; (clojure.pprint/pprint (some-> hiccup-form decant-styles))

  (if-let [decanted-style (decant-styles hiccup-form)]
    (let [props' (-> props
                     (dissoc :sst :static-style :dst :dynamic-style)
                     (assoc :style decanted-style))]
      ;; (def plouf decanted-style)
      (some-> decanted-style
              :static
              collect-static-style!)
      (into [tag props'] others))
    hiccup-form))

(defn collect-and-pass-through-xform
  "Takes a hiccup form as input and outputs untouched, while collecting static
  styles as a side effect."
  [[tag
    {:as props :keys [style
                      sst static-style
                      dst dynamic-style]}
    :as hiccup-form]]
  ;; (clojure.pprint/pprint hiccup-form)
  ;; (clojure.pprint/pprint (some-> hiccup-form decant-styles))

  (when-let [decanted-style (decant-styles hiccup-form)]
    (some-> decanted-style
            :static
            collect-static-style!))
  hiccup-form)

(comment (reset! *style-frequencies {}))

;; TODO FIXME automated tests
(comment
  (deref *style-frequencies)
  (raw-collector-xform [:View {:style {:margin 42}}])
  (raw-collector-xform [:View {}])
  (raw-collector-xform [:View {:style '(do {})}])
  (raw-collector-xform [:View {:style {}}])
  (raw-collector-xform [:View {:style {:color 'bim}}])
  (raw-collector-xform [:View {:style {:margin 42
                                                  :background-color 'bim}}])
  (raw-collector-xform [:View {:sst {:color "plop"}
                               :dst {:margin 42}}])


  (def plop {:margin 42 :background-color "red"})

  (raw-collector-xform [:View {:style 'plop}])

  )





(defn abort-on-missing-style [style-coll style-kv]
  (assert (get style-coll style-kv)
          (str "Error rendering static style:
This style is not part of the provided style-coll: "
               style-kv
               "\n")))














(defn prepare-aot-styles! [ns-symbol]
  (require ns-symbol)
  (reset! *aot-styles @*style-frequencies)
  nil)


(comment
  (prepare-aot-styles! 'motus.views.root)
  )

