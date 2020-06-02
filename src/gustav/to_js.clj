(ns gustav.to-js
  (:import (cljs.tagged_literals JSValue)))

;; From Roman01la https://gist.github.com/roman01la/98d23f56468e266d86314aadabb56f12

;; Sablono's stuff
;; Converting Clojure data into ClojureScript (JS)
;; ====================================================
(defprotocol IJSValue
  (to-js [x]))

(defn- to-js-map [m]
  (JSValue. (into {} (map (fn [[k v]] [k (to-js v)])) m)))

(extend-protocol IJSValue
  clojure.lang.Keyword
  (to-js [x]
    (if (qualified-keyword? x)
      (str (namespace x) "/" (name x))
      (name x)))
  clojure.lang.PersistentArrayMap
  (to-js [x]
    (to-js-map x))
  clojure.lang.PersistentHashMap
  (to-js [x]
    (to-js-map x))
  clojure.lang.PersistentVector
  (to-js [x]
    (JSValue. (mapv to-js x)))
  Object
  (to-js [x]
    x)
  nil
  (to-js [_]
    nil))

;; =========================================

(defn style-value-to-js [style-value]
  (to-js style-value))
