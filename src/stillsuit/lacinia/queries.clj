(ns stillsuit.lacinia.queries
  (:require [com.walmartlabs.lacinia.resolve :as resolve]
            [stillsuit.lacinia.resolvers :as resolvers]
            [stillsuit.datomic.core :as datomic]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            [stillsuit.lacinia.types :as types]
            [com.walmartlabs.lacinia.schema :as schema])
  (:import (com.walmartlabs.lacinia.resolve ResolverResult)))

(defn stillsuit-entity-id-query
  [{:stillsuit/keys [entity-id-query-name datomic-entity-type]}]
  {:type        datomic-entity-type
   :args        {:eid {:type        '(non-null ID)
                       :description "The `:db/id` of the entity"}}
   :resolve     :stillsuit/resolve-by-enitity-id
   :description "Return the current time."})

(def entity-id-query-resolver
  ^ResolverResult
  (fn entity-id-query-resolver-fn
    [{:stillsuit/keys [config db] :as context} {:keys [eid] :as args} value]
    (let [ent      (datomic/get-entity-by-eid db eid)
          ent-type (types/lacinia-type ent config)]
      (when (some? ent)
        (resolve/resolve-as
         (schema/tag-with-type ent ent-type))))))

(defn stillsuit-unique-attribute-query
  [{:stillsuit/keys [entity-id-query-name datomic-entity-type]}]
  {:type        datomic-entity-type
   :args        {:eid {:type        '(non-null ID)
                       :description "The `:db/id` of the entity"}}
   :resolve     [:stillsuit-resolve-by-unique-id datomic-entity-type]
   :description "Get a `%s` entity by specifying its `%` attribute."})

;; TODO: break up attr-info
(defn unique-attribute-query-resolver
  "Catchpocket interface to a generic query, expected to be referenced as a resolver as
  :resolve [:stillsuit-resolve-by-unique-id :ReturnType attr-info]"
  [& [return-type attr-info]]
  ^resolve/ResolverResult
  (fn unique-attribute-query-resolver-fn [{:stillsuit/keys [db options]} args value]
    (let [{:attribute/keys [ident field-type]} attr-info
          arg    (-> args vals first)
          result (datomic/get-entity-by-unique-attribute db ident arg)]
      ;(log/spy [arg ident field-type result])
      (resolve/resolve-as
       (schema/tag-with-type result return-type)))))

(defn resolver-map
  [{:stillsuit/keys [entity-id-query-name query-by-unique-id-name]}]
  {:stillsuit/resolve-by-enitity-id entity-id-query-resolver
   :stillsuit-resolve-by-unique-id  unique-attribute-query-resolver})

(defn attach-queries [schema config]
  (let [{:stillsuit/keys [entity-id-query-name query-by-unique-id-name]} config]
    (-> schema
        (assoc-in [:queries entity-id-query-name]
                  (stillsuit-entity-id-query config))
        (assoc-in [:queries query-by-unique-id-name]
                  (stillsuit-unique-attribute-query config)))))
