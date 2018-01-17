(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [stillsuit.test-util.fixtures :as fixtures]
            [stillsuit.core :as stillsuit]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [datomic.api :as d]))

(use-fixtures :once fixtures/once)

(defn- rainbow-by-id [{:keys [:stillsuit/db]} {:keys [id]} _]
  (let [num (-> id read-string long)]
    (d/entity db [:rainbow/id num])))

(defn- echo-rainbow [{:keys [:stillsuit/db]} args value]
  (log/spy args)
  (log/spy value)
  nil)

(def resolver-map {:mutation/echo       echo-rainbow
                   :query/rainbow-by-id rainbow-by-id})

(defn setup [resolvers]
  (let [schema   (fixtures/get-schema :rainbow)
        context  (fixtures/get-context :rainbow)
        _ (log/spy resolvers)
        compiled (-> schema
                     (util/attach-resolvers resolvers)
                     (stillsuit/decorate {:stillsuit/scalars  :all
                                          :stillsuit/entity   :true
                                          :stillsuit/compile? true}))]
    [compiled context]))

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
