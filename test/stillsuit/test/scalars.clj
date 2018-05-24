(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [stillsuit.test-util.fixtures :as fixtures]
            [stillsuit.core :as stillsuit]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [datomic.api :as d]
            [clojure.tools.reader.edn :as edn]))

(use-fixtures :once fixtures/once)
(use-fixtures :each fixtures/each)

(defn rainbow-typecheck
  "Little utility function that takes in a value (as a lacinia scalar) and a string which
  can be parsed by edn/read-string and returns true if the type of the value and the type
  of the parsed string are the same."
  [c {:keys [value expected]} v]
  (let [val-type    (type value)
        expect-val  (edn/read-string expected)
        expect-type (type expect-val)]
    (when (not= val-type expect-type)
      (log/warnf "Typecheck failed: expected %s, got %s" (str expect-type) (str val-type)))
    (= val-type expect-type)))

(defn- rainbow-by-id [context {:keys [id]} _]
  (some-> context stillsuit/db (d/entity [:rainbow/id id])))

(defn- echo-rainbow [context args value]
  (-> args vals first))

(def rainbow-resolver-map {:mutation/echo       echo-rainbow
                           :rainbow/identity    echo-rainbow
                           :rainbow/typecheck   rainbow-typecheck
                           :query/rainbow-by-id rainbow-by-id})

(deftest test-rainbow-queries
  (try
    (fixtures/verify-queries!
     (fixtures/load-setup :rainbow rainbow-resolver-map))

    (catch Exception e
      (.printStackTrace e))))
