(ns stillsuit.test-util.fixtures
  (:require [datomic.api :as d]
            [clojure.test :refer [testing is]]
            [stillsuit.lib.util :as util]
            [clojure.tools.logging :as log]
            [clojure.test :as test]
            [stillsuit.core :as stillsuit]
            [yaml.core :as yaml]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.tools.reader.edn :as edn]
            [clojure.walk :as walk]))

(def test-db-uri "datomic:mem://stillsuit-test-")

(def all-db-names [:rainbow :music :enums])

(def ^:private db-store (atom {}))

;; Datomic fixtures

(defn- provision-db
  [db-name]
  (let [db-str (name db-name)
        uri    (str test-db-uri db-str)
        path   (format "resources/test-schemas/%s/datomic.edn" db-str)
        txes   (util/load-edn-resource path)]
    (if-not (d/create-database uri)
      (log/errorf "Couldn't create database %s!" uri)
      (let [conn (d/connect uri)]
        (doseq [tx txes]
          @(d/transact conn tx))
        (log/debugf "Loaded %d transactions from %s" (count txes) db-str)
        conn))))

(defn- setup-datomic []
  (doseq [db-name all-db-names
          :let [conn (provision-db db-name)]]
    (swap! db-store assoc db-name conn)))

(defn- teardown-datomic []
  (doseq [db-name all-db-names]
    (d/delete-database (str test-db-uri (name db-name)))
    (log/debugf "Deleted database %s" (name db-name))
    (swap! db-store dissoc db-name)))

(defn datomic-fixture [test-fn]
  (setup-datomic)
  (test-fn)
  (teardown-datomic))

(defn catch-fixture [test-fn]
  (try
    (test-fn)
    (catch Exception e
      (log/warn "whoops!")
      (when-let [exdata (ex-data e)]
        (log/spy :warn exdata))
      (throw e))))

(defn- get-connection [db-name]
  (get @db-store db-name))

(defn- get-db [db-name]
  (d/db (get-connection db-name)))

;; Test harness stuff

(defn- get-schema
  [db-name]
  (->> db-name
       name
       (format "resources/test-schemas/%s/lacinia.edn")
       util/load-edn-resource))

(defn- get-config
  [db-name]
  (->> db-name
       name
       (format "resources/test-schemas/%s/stillsuit.edn")
       util/load-edn-resource))

(defn- get-query-doc
  [db-name]
  (->> db-name
       name
       (format "resources/test-schemas/%s/queries.yaml")
       io/resource
       io/reader
       slurp
       yaml/parse-string))

(defn load-setup
  "Given a db-name which maps to a directory under test/resources/test-schemas, load up a
  bunch of sample edn data and queries and compile a schema. Return a map of the data which
  can be passed to (execute-query) for further testing."
  ([db-name resolver-map]
   (load-setup db-name resolver-map nil))
  ([db-name resolver-map filters]
   (load-setup db-name resolver-map filters nil))
  ([db-name resolver-map filters overrides]
   (let [config    (get-config db-name)
         schema    (get-schema db-name)
         queries   (get-query-doc db-name)
         decorated (stillsuit/decorate #:stillsuit{:schema         schema
                                                   :config         config
                                                   :entity-filters filters
                                                   :connection     (get-connection db-name)
                                                   :resolvers      resolver-map})]
     (util/deep-map-merge {::context   (:stillsuit/app-context decorated)
                           ::config    config
                           ::schema    (:stillsuit/schema decorated)
                           ::decorated decorated
                           ::query-doc queries}
                          overrides))))

(defn approx-floats
  "This function is here to aid testing floating-point numbers. It walks the data structure provided
  and converts any floating-point numbers provided into BigDecimals with the given scale (default 3)."
  ([data]
   (approx-floats data 3))
  ([data ^Integer scale]
   (walk/postwalk (fn [item]
                    (if (float? item)
                      (-> item bigdec (.setScale scale java.math.RoundingMode/HALF_UP))
                      item))
                  data)))

(defn execute-named-query
  "Given a setup map as returned by (load-setup), execute the query defined in the associated YAML"
  ([setup query-name]
   (execute-named-query setup query-name nil))
  ([setup query-name variables]
   (let [query (get-in setup [::query-doc query-name :query])]
     (is (some? query))
     (stillsuit/execute (::decorated setup) query variables))))

(defn verify-queries!
  "Given a setup map returned by load-setup, run through every query in the queries.yaml file,
  executing each one and asserting that its output is identical to the expected output, if any."
  [setup]
  (testing "Verifying query response"
    (doseq [qname (-> setup ::query-doc keys sort)
            :let [response-str (get-in setup [::query-doc qname :response])]
            :when [response-str]
            :let [expected (edn/read-string response-str)]]
      (testing (str qname)
        (let [result     (execute-named-query setup qname)
              simplified (util/simplify result)]
          (is (= (approx-floats expected)
                 (approx-floats simplified))))))))

(def once (test/join-fixtures [datomic-fixture]))
(def each (test/join-fixtures [catch-fixture]))
