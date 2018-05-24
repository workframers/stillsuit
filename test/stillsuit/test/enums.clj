(ns stillsuit.test.enums
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [stillsuit.test-util.fixtures :as fixtures]
            [stillsuit.core :as stillsuit]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [datomic.api :as d]
            [clojure.tools.reader.edn :as edn]
            [stillsuit.lacinia.enums :as se]))

(use-fixtures :once fixtures/once)
(use-fixtures :each fixtures/each)

(defn- ref-by-id [context {:keys [id]} _]
  (some-> context stillsuit/db (d/entity [:animal-ref/id id])))

(defn- keyword-by-id [context {:keys [id]} _]
  (some-> context stillsuit/db (d/entity [:animal-keyword/id id])))

(defn- ref-by-type [context args _]
  (let [db     (stillsuit/db context)
        a-type (stillsuit/datomic-enum context :movement_ref (:type args))
        ents   (->> (d/q '[:find [?a ...]
                           :in $ ?t
                           :where [?a :animal-ref/movement ?t]]
                         db a-type)
                    (map (partial d/entity db)))]
    ents))

(def resolver-map {:query/kw-by-id     keyword-by-id
                   :query/ref-by-id    ref-by-id
                   :query/refs-by-type ref-by-type})

(def sample-schema {:enums
                    {:etype
                     {:values [{:enum-value              :ABC
                                :stillsuit/datomic-value :datomic/abc}
                               {:enum-value              :XYZ
                                :stillsuit/datomic-value :datomic/xyz}]}}})

(deftest test-enum-generation
  (let [e-map (se/make-enum-map {} sample-schema)]
    (is (some? e-map))
    (is (= {:etype
            #:stillsuit{:datomic-to-lacinia
                        {:datomic/abc :ABC, :datomic/xyz :XYZ},
                        :lacinia-to-datomic
                        {:ABC :datomic/abc, :XYZ :datomic/xyz}}}
           e-map))))

(deftest test-enum-resolution
  (let [e-map   (se/make-enum-map {} sample-schema)
        context {:stillsuit/enum-map e-map}]
    (is (= :datomic/abc
           (stillsuit/datomic-enum context :etype :ABC)))))

(deftest test-enum-queries
  (try
    (let [setup (fixtures/load-setup :enums resolver-map)]
      (fixtures/verify-queries! setup))

    (catch Exception e
      (.printStackTrace e))))
