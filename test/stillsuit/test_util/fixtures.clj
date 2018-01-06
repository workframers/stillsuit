(ns stillsuit.test-util.fixtures
  (:require [datomic.api :as d]
            [datomock.core :as dm]
            [stillsuit.lib.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.test :as test]))

(def test-db-uri "datomic:mem://stillsuit-test-")

(def all-db-names [:rainbow])

(def ^:private db-store (atom {}))

(defn provision-db
  [db-name]
  (let [db-str (name db-name)
        uri    (str test-db-uri db-str)
        path   (format "resources/test-schemas/%s/datomic.edn" db-str)
        txes   (edn/load-edn-resource path)]
    (if-not (d/create-database uri)
      (log/errorf "Couldn't create database %s!" uri)
      (let [conn (d/connect uri)]
        (doseq [tx txes]
          @(d/transact conn tx))
        (log/debugf "Loaded %d transactions from %s" (count txes) db-str)
        conn))))

(defn get-schema
  [db-name]
  (->> db-name
       name
       (format "resources/test-schemas/%s/lacinia.edn")
       edn/load-edn-resource))

(defn get-db [name]
  (provision-db name))

(defn setup-datomic []
  (doseq [db-name all-db-names
          :let [conn (provision-db db-name)]]
    (swap! db-store assoc db-name conn)))

(defn teardown-datomic []
  (doseq [db-name all-db-names]
    (d/delete-database (str test-db-uri (name db-name)))
    (log/debugf "Deleted database %s" (name db-name))
    (swap! db-store dissoc db-name)))

(defn datomic-fixture [test-fn]
  (setup-datomic)
  (test-fn)
  (teardown-datomic))

(defn get-connection [db-name]
  (get @db-store db-name))

(defn get-db [db-name]
  (d/db (get-connection db-name)))

(def once (test/join-fixtures [datomic-fixture]))

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

