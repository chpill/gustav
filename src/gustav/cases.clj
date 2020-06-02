(ns gustav.cases
  (:require [clojure.string :as str]))


(defn kebab->camel [^String method-name]
  (str/replace
   method-name
   #"-(\w)"
   #(str/upper-case (second %1))))

(defn kebab->camel-k [k]
  (-> k name kebab->camel keyword))

(defn map-enforce-camel [styles]
  (into {}
        (map (fn [[k v]]
               [(kebab->camel-k k) v]))
        styles))

(comment (map-enforce-camel {:background-color "red"
                             :margin-top 12}))


(defn camel->kebab [^String s]
  (str/replace s
               #"[a-z][A-Z]"
               (fn [[last-letter first-letter]]
                 (str last-letter
                      "-"
                      (str/lower-case first-letter)))))

(defn camel->kebab-k [k]
  (-> k name camel->kebab keyword))

(defn map-enforce-kebab [styles]
  (into {}
        (map (fn [[k v]]
               [(camel->kebab-k k) v]))
        styles))

(comment
  (camel->kebab "plopPlouf")

  (map-enforce-kebab {:backgroundColor "papayawhip"
                      :marginTop 20}))
