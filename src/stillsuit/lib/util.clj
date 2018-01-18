(ns stillsuit.lib.util
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

(defn load-edn-resource
  "Given a filename in resources, read and parse it, returning nil if it wasn't found"
  [resource-path]
  (try
    (with-open [r (io/reader (io/resource resource-path))]
      (edn/read {:readers *data-readers*} (PushbackReader. r)))
    (catch IOException e
      (log/errorf "Couldn't open file '%s': %s" resource-path (.getMessage e))
      nil)
    ;; This is the undocumented exception clojure.edn throws if it gets an error parsing an edn file
    (catch RuntimeException e
      (log/errorf "Error parsing edn file '%s': %s" resource-path (.getMessage e))
      nil)))
