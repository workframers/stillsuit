(ns stillsuit.lib.util
  "A collection of utility functions."
  (:require [clojure.walk :as walk]
            [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io])
  (:import (clojure.lang IPersistentMap)
           (java.io IOException PushbackReader)))

;; Cheerfully copied from the lacinia tutorial
(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes for easier constants in the tests, and
   eliminates ordering problems."
  [m]
  (walk/postwalk
   (fn [node]
     (cond
       (instance? IPersistentMap node)
       (into {} node)

       (seq? node)
       (vec node)

       :else
       node))
   m))

(defn load-edn-file
  "Given a filename in resources, read and parse it, returning nil if it wasn't found"
  [filename]
  (try
    (with-open [r (io/reader filename)]
      (edn/read {:readers *data-readers*} (PushbackReader. r)))
    (catch IOException e
      (log/errorf "Couldn't open file '%s': %s" filename (.getMessage e))
      nil)
    ;; This is the undocumented exception clojure.edn throws if it gets an error parsing an edn file
    (catch RuntimeException e
      (log/errorf "Error parsing edn file '%s': %s" filename (.getMessage e))
      nil)))

(defn load-edn-resource
  "Given a filename in resources, read and parse it, returning nil if it wasn't found"
  [resource-path]
  (-> resource-path io/resource load-edn-file))

;; Adapted from deep-merge-with to handle nil values:
;; https://github.com/clojure/clojure-contrib/commit/19613025d233b5f445b1dd3460c4128f39218741
(defn deep-merge-with
  "Like merge-with, but merges maps recursively, appling the given fn
  only when there's a non-map at a particular level.
  ```clojure
    (deep-merge-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                       {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
    ; => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}
  ```"
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? #(or (map? %) (nil? %)) maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn deep-map-merge
  "Recursively merge one or more maps, using the values of later maps to replace the
  values of earlier ones."
  [& maps]
  (apply deep-merge-with
         (fn [& values]
           (last values))
         maps))
