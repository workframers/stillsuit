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
    (= val-type expect-type)))

(defn- rainbow-by-id [{:keys [:stillsuit/connection]} {:keys [id]} _]
  (some-> connection d/db (d/entity [:rainbow/id num])))

(defn- echo-rainbow [context args value]
  (-> args vals first))

(def rainbow-resolver-map {:mutation/echo       echo-rainbow
                           :rainbow/identity    echo-rainbow
                           :rainbow/typecheck   rainbow-typecheck
                           :query/rainbow-by-id rainbow-by-id})

(deftest test-music-queries
  (try
    (fixtures/verify-queries!
      ;(log/spy
      (fixtures/load-setup :rainbow rainbow-resolver-map))

    (catch Exception e
      (.printStackTrace e))))

; hrm, compile troubles
;(deftest test-scalars-outbound
;  (testing "load database"
;    (let [[schema ctx] (setup resolver-map)
;          query  "{ rainbowById(id: 111) {
;                      id oneString oneLong oneFloat oneInstant oneUuid oneKeyword oneBoolean oneBigdec oneBigint
;                      oneRef { oneString } } }"
;          result (lacinia/execute schema query nil ctx)
;          data   (get-in result [:data :rainbowById])]
;      (is (map? schema))
;      (is (nil? (:errors result)))
;      (is (some? data)))))
;
;(deftest test-scalars-inbound
;  (testing "load database"
;    (let [[schema ctx] (setup resolver-map)
;          query  "mutation {
;                    echo(id: 111) {
;                      id
;                    }
;                  }"
;          result (lacinia/execute schema query nil ctx)
;          data   (get-in result [:data :rainbowById])]
;      (log/spy result)
;      (is (map? schema))
;      (is (nil? (:errors result)))
;      (is (some? data)))))
