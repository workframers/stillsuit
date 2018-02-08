(ns stillsuit.lib.test-util
  "Some functions helpful for setting up tests."
  (:require [clojure.walk :as walk]
            [datomic.api :as d]
            [stillsuit.lib.util :as util]
            [clojure.tools.logging :as log]))

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

(defn make-datomic-fixture
  "Function which returns a fixture usable in `(clojure.test/use-fixtures)`. The
  returned fixture "
  [db-list]
  (fn datomic-fixture-fn [test-fn]
    (setup-datomic db-list)
    (test-fn)
    (teardown-datomic db-list)))

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
  ([db-name resolver-map overrides]
   (let [config    (get-config db-name)
         schema    (get-schema db-name)
         queries   (get-query-doc db-name)
         decorated (stillsuit/decorate #:stillsuit{:schema     schema
                                                   :config     config
                                                   :connection (get-connection db-name)
                                                   :resolvers  resolver-map})]
     (util/deep-map-merge {::context   (:stillsuit/app-context decorated)
                           ::config    config
                           ::schema    (:stillsuit/schema decorated)
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

(defn execute-query
  "Given a setup map as returned by (load-setup), execute the query defined in the associated YAML"
  ([setup query-name]
   (execute-query setup query-name nil))
  ([{::keys [context schema query-doc]} query-name variables]
   (let [query (get-in query-doc [query-name :query])]
     (lacinia/execute schema query variables context))))
