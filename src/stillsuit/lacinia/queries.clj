(ns stillsuit.lacinia.queries
  (:require [com.walmartlabs.lacinia.resolve :as resolve]
            [stillsuit.lacinia.resolvers :as resolvers]
            [clojure.tools.logging :as log]
            [datomic.api :as d])
  (:import (com.walmartlabs.lacinia.resolve ResolverResult)))

(defn stillsuit-entity-id-query
  [{:keys [:stillsuit/entity-id-query-name :stillsuit/datomic-entity-type]}]
  {:type        datomic-entity-type
   :args        {:eid {:type        '(non-null ID)
                       :description "String version of the timezone, ie `America/New_York`"}}
   :resolve     :stillsuit/entity-id-query-resolver
   :description "Return the current time."})

(def entity-id-query-resolver
  ^ResolverResult
  (fn entity-id-query-resolver-fn
    [{:keys [:stillsuit/db] :as context} {:keys [eid] :as args} value]
    (d/entity db db)))

(defn resolver-map [config]
  {:stillsuit/entity-id-query-resolver entity-id-query-resolver})

(defn attach-queries [schema config]
  (let [{:keys [:stillsuit/entity-id-query-name :stillsuit/entity-ref-query-name]} config]
    (-> schema
        (assoc-in [:queries entity-id-query-name] (stillsuit-entity-id-query config)))))
