(ns stillsuit.lib.edn
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import (java.io IOException PushbackReader)))

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
