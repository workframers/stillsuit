(ns stillsuit.test-util.fixtures
  (:require [datomic.api :as d]
            [datomock.core :as dm]
            [stillsuit.lib.edn :as edn]
            [clojure.tools.logging :as log]))

(def test-db-uri "datomic:mem://stillsuit-test")


(defn provision-db
  [db-name]
  (let [uri   test-db-uri
        edn-path (format "resources/test-schemas/%s/datomic.edn" (name db-name))
        txes     (edn/load-edn-resource edn-path)]
    (if-not (d/create-database test-db-uri)
      (log/errorf "Couldn't create database %s!" test-db-uri))))

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

