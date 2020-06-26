(ns gustav.web
  (:require [clojure.string :as str]
            gustav.core))

(defn dec-to-baseB [n B]
  (loop [output ()
         n      n]
    (let [q (quot n B)
          r (rem n B)]
      ;; (println output)
      (if (< n B)
        (conj output r)
        (recur (conj output r)
               q)))))

(comment (dec-to-baseB 1234 10))


(def alphabet-low "abcdefghijklmnopqrstuvwxyz")
(def alphabet-up (str/upper-case alphabet-low))
(def class-name-chars (concat alphabet-low alphabet-up))

(defn make-class-name [n]
  (->> (dec-to-baseB n (count class-name-chars))
       (map #(nth class-name-chars %))
       (apply str)))


(defn make-style-name [[style-rule style-value]]
  (str (name style-rule) "__" style-value))

(comment

  (make-class-name 0)
  (make-class-name 1)
  (make-class-name (* 51 52))
  (make-class-name 10e7)
  )


#?(:cljs
   (do

     (comment

       (goog-define aot_goog_define false)
       (def ^:const aot? aot_goog_define)

       (js/console.log
               (if aot_goog_define
                 "aot_goog_define is true"
                 "aot_goog_define is false"))

              (js/console.log
               (if aot?
                 "aot? is true"
                 "aot? is false")))


     (def *registered-styles (atom {}))

     (defn use-style! [style]
       (or (get @*registered-styles style)
           ;; TODO FIXME
           ;; use `make-style-name` here, to have the same system that in release-mode
           (let [class-name (-> (count @*registered-styles) make-class-name)
                 sheet (js-libs.react-components/raw-sst (js-obj class-name
                                                                 (clj->js style)))
                 style-id (goog.object/get sheet class-name)]
             (swap! *registered-styles assoc style style-id)
             style-id)))

     (defn just-in-time-dev-styles [styles non-static-styles]
       (let [js-styles (reduce (fn [acc style]
                                 (.push acc (use-style! style))
                                 acc)
                               #js []
                               styles)]
         (when non-static-styles
           (.push js-styles non-static-styles))
         js-styles))))



;; [_a-zA-Z]+[_a-zA-Z0-9-]*
(def first-chars "ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz")
(def first-chars-count 53)

(def next-chars "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz")
(def next-chars-count 64)


(defn s
  ([n] (s "" n))
  ([acc n]
   (->> (dec-to-baseB n first-chars-count)
        (map #(nth first-chars %))
        str/join)))

(s 52) "z"
(s 53) "BA" ;; arrrg...

(def b first-chars-count)
(def progression [1 b (* 2 b) (* b b) (* b (inc b))])
(def test-seq (into []
                    (comp (map (fn [x] [(dec x) x]))
                          cat)
                    progression))

(map s test-seq)
'(0   1   b-1  b   b+b-1  b+b  bb-1 bb    bb+b-1 bb+b)
'("A" "B" "z" "BA" "Bz"   "CA" "zz" "BAA" "BAz"  "BBA")
'("A" "B" "z" "AA" "Az"   "BA" "yz" "zA"  "zz"   "AAA")

(* (inc b)
   (* b b))

'()
'("zzz" "AAAA")



(defn s2 [n]
  (let [base first-chars-count
        q (quot n base)
        r (rem n base)
        acc (list r)]
    (->> (if (< n base)
           acc
           ;; (dec-to-baseB q (inc first-chars-count))
           (into acc
                 (map dec)
                 (dec-to-baseB q (inc first-chars-count))))
         (map #(nth first-chars %))
         str/join)))

(map s2 test-seq)
(map s2 [0 1 52 53 105 106 2808 2809 2861 ;;2862
         ])

(= 00000 0)

(s2 0)
(s2 52)
(s2 53)
(s2 105)
(s2 106)
(comment (s2 (* first-chars-count (inc first-chars-count))))
(comment (s2 2862)) ;; crash


(defn pow [x power]
  (reduce * 1 (repeat power x)))

(comment (pow 3 2))

(defn sum-of-powers [b i]
  (reduce + 0 (map #(pow b %)
                   (range i))))

(comment (sum-of-powers 3 3))

(defn left-pad-with-0 [l expected-length]
  (into (vec (repeat (- expected-length
                        (count l))
                     0))
        l))

(comment (left-pad-with-0 '(1) 4))

(defn s3 [n base]
  (loop [step 0]
    (if (< n (dec (sum-of-powers base (inc step))))
      (-> (- n (dec (sum-of-powers base step)))
          (dec-to-baseB base)
          (left-pad-with-0 step))
      (recur (inc step)))))

;; this works for [_a-za-z]+
(defn s3-as-chars [n chars]
  (->> (s3 n (count chars))
       (map #(nth chars %))
       str/join))




(comment (s3 106 53)
         (s3-as-chars 300000 first-chars))

(nth first-chars 4)
(nth next-chars 4)

(map #(s3-as-chars % first-chars) (range 1000))


(comment (doseq [[n expected-class] {0 "A" 1 "B" 52 "z" ;; 53

                                     53 "AA" 105 "Az" ;; 53
                                     106 "BA" }]
   (assert (= expected-class (s3-as-chars n first-chars))
           (str "FAILED with n:" n " expected:" expected-class " actual:" (s3-as-chars n first-chars)))))

;; This works for [_a-zA-Z][_-0-9a-zA-Z]*
(defn s4 [n b B]
  (if (< n b)
    [n]
    (loop [step 0]
      (if (< n (* b (sum-of-powers B (inc step))))
        (do
          (let [B-pow-step (pow B step)
                x (- n (* b (sum-of-powers B step)))]
            (into [(quot x B-pow-step)]
                  (-> x
                      (rem B-pow-step)
                      (dec-to-baseB B)
                      (left-pad-with-0 step)))))
        (recur (inc step))))))


(comment (s4 100
             (count first-chars)
             (count next-chars))
         ;; "zz"
         (s4 (dec (+ first-chars-count (* first-chars-count next-chars-count)))
             (count first-chars)
             (count next-chars))

         ;; "A--"
         (s4 (+ first-chars-count (* first-chars-count next-chars-count))
             (count first-chars)
             (count next-chars))

         )

(defn s4-as-chars [n first-chars next-chars]
  (let [[first-char-index & next-char-indices] (s4 n (count first-chars) (count next-chars))]
    (str/join
     (into [(nth first-chars first-char-index)]
           (map #(nth next-chars %))
           next-char-indices))))

(comment (s4-as-chars 100 first-chars next-chars))

(def int-to-css-class #(s4-as-chars % first-chars next-chars))


(comment
  (clojure.pprint/pprint (map int-to-css-class (range 4000)))
  (doseq [[n expected-class] {0 "A" 1 "B" 52 "z"
                              53 "A-" 54 "A0"
                              116 "Az"
                              117 "B-" 118 "B0" 180 "Bz"
                              181 "C-" 244 "Cz"
                              245 "D-" 308 "Dz"

                              (dec (+ first-chars-count
                                      (* first-chars-count next-chars-count)))
                              "zz"

                              (+ first-chars-count
                                 (* first-chars-count next-chars-count))
                              "A--"

                              (dec (+ first-chars-count
                                      (* first-chars-count next-chars-count)
                                      (* first-chars-count
                                         next-chars-count
                                         next-chars-count)))
                              "zzz"

                              (+ first-chars-count
                                 (* first-chars-count next-chars-count)
                                 (* first-chars-count
                                    next-chars-count
                                    next-chars-count))
                              "A---"}]
    (assert (= expected-class (int-to-css-class n))
            (str "FAILED with n:" n " expected:" expected-class " actual:" (int-to-css-class n)))))



(defn class-names [style-to-css-classes styles]
  (->> styles
       (map #(or (get style-to-css-classes %)
                 (throw (ex-info (str "unknow static style: " %)
                                 {:missing-style %}))))
       (str/join " ")))

(comment
  (class-names {[:a 1] "a"
                [:b 2] "b"}
               {:a 1 :b 2})
  (class-names {[:a 1] "a"
                [:b 2] "b"}
               {:a 1 :b 2 :c 3})
  )


(defn make-web-aot-xform
  "Provides a function that will resolve static styles according to the input
  `style-to-css-classes` and output css class names.

   `style-to-css-classes` may be a set or a map, or whatever that supports `get`."
  [style-to-css-classes]
  (fn [[tag
        {:as props :keys [style
                          class
                          sst static-style
                          dst dynamic-style]}
        & others
        :as hiccup-form]]

    (let [{:keys [static dynamic]} (gustav.core/decant-styles hiccup-form)]
      (if (or static dynamic)
        (let [static-style-as-css-classes (class-names style-to-css-classes
                                                       static)
              props' (cond-> (dissoc props :style :sst :static-style :dst :dynamic-style)
                       dynamic (assoc :style dynamic)
                       static (assoc :class
                                     (cond->> static-style-as-css-classes
                                       (not (str/blank? class)) (str class " "))))]
          (into [tag props'] others))
        hiccup-form))))



(comment

  (def xf (make-web-aot-xform {[:flex 1] "a" [:margin 42] "b" [:color "red"] "c"}))

  (xf [:div {:style {:margin 42}}])

  )

(defn css-class-declarations [aot-styles]
  (->> aot-styles
       (map (fn [[[property-kw value] class-name]]
              (str class-name " {"
                   (name property-kw) ": " value
                   "}")))
       (str/join "\n")))


(comment
  (let [aot-styles {[:color "red"] "A"
                    [:color "blue"] "B"}]

    (->> aot-styles
         (map (fn [[[property-kw value] class-name]]
                (str class-name " {"
                     (name property-kw) ": " value
                     "}")))
         (str/join "\n"))

    ))


(defn style-frequencies-to-aot-classes [style-freq]
  (->> style-freq
       (sort-by val >)
       (map first)
       (reduce (fn [acc style]
                 (assoc acc style (int-to-css-class (count acc))))
               {})))

(comment
  (let [style-freq {[:color "red"] 2
                    [:flex 1] 10
                    [:padding 10] 5}]
    (style-frequencies-to-aot-classes style-freq)))


;; TODO
(def dev-xform identity)
