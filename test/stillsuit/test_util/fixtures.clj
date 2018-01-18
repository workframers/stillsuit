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
            [clojure.tools.reader.edn :as edn]))

(def test-db-uri "datomic:mem://stillsuit-test-")

(def all-db-names [:rainbow :music])

(def ^:private db-store (atom {}))

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

(defn- get-context [db-name]
  (stillsuit/app-context nil (get-connection db-name)))

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
  "Return a tuple [app-context resolver-map compiled-schema]"
  [db-name resolver-map]
  (let [config   (get-config db-name)
        context  (get-context db-name)
        schema   (get-schema db-name)
        queries  (get-query-doc db-name)
        compiled (stillsuit/decorate schema config resolver-map)]
    {::context   context
     ::config    config
     ::schema    compiled
     ::query-doc queries}))

(defn execute-query
  "Given a setup map as returned by (load-setup), execute the query defined in the associated YAML"
  ([setup query-name]
   (execute-query setup query-name nil))
  ([{::keys [context schema query-doc]} query-name variables]
   (let [query (get-in query-doc [query-name :query])]
     (is (some? query))
     (lacinia/execute schema query variables context))))

(defn verify-queries!
  "Given a setup map returned by load-setup, run through every query, executing each one and
  comparing it to the expected output."
  [setup]
  (testing "Verifying query response"
    (doseq [qname (-> setup ::query-doc keys sort)
            :let [response-str (get-in setup [::query-doc qname :response])]
            :when [response-str]
            :let [expected (edn/read-string response-str)]]
      (testing (str qname)
        (let [result     (execute-query setup qname)
              simplified (util/simplify result)]
          (is (= expected simplified)))))))

(def once (test/join-fixtures [datomic-fixture]))
(def each (test/join-fixtures [catch-fixture]))

;(defn datomic-fork
;  "This fixture redefines datomic/get-connection so that it returns a forked version
;  of the datomic db from setup-base-db. Similar to acme-fixtures/datomic-rollback,
;  biut faster because it doesn't need to reload the migrations per invocation."
;  [test-fn]
;  ;; setup
;  (let [mock-conn (dm/mock-conn mock-datomic-base)]
;    (with-redefs [datomic/get-connection (constantly mock-conn)]
;      ;; run the tests
;      (test-fn))))

