(ns stillsuit.core
  (:require [stillsuit.lacinia.queries :as sq]
            [stillsuit.lacinia.scalars :as ss]
            [stillsuit.lacinia.resolvers :as sr]
            [datomic.api :as d]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.util :as util]))

(defn make-app-context
  "Return an app-context map suitable for handing to (lacinia/execute-query)."
  [base-context schema connection config]
  (merge (select-keys schema [:stillsuit/enum-map])
         {:stillsuit/connection connection
          :stillsuit/config     config}
         base-context))

(def default-config
  {:stillsuit/datomic-entity-type     :DatomicEntity
   :stillsuit/entity-id-query-name    :entity_by_eid
   :stillsuit/query-by-unique-id-name :entity_by_unique_id
   :stillsuit/no-default-resolver?    false
   :stillsuit/no-scalars?             false
   :stillsuit/compile?                true})

(defn decorate-resolver-map
  ([resolver-map]
   (decorate-resolver-map resolver-map nil))
  ([resolver-map options]
   (let [with-defaults (merge default-config options)]
     (merge resolver-map
            (sr/resolver-map with-defaults)
            (sq/resolver-map with-defaults)))))

;; TODO: specs

(defn decorate
  "Main interface to stillsuit. Accepts a map containing various parameters as input; returns
  a map with an app context and a schema."
  [{:stillsuit/keys [schema config resolvers transformers context connection]}]
  (let [opts         (merge default-config config)
        uncompiled   (-> schema
                         (ss/attach-scalars opts)
                         (sq/attach-queries opts)
                         (sr/attach-resolvers opts)
                         (util/attach-resolvers (decorate-resolver-map resolvers opts))
                         (util/attach-scalar-transformers (ss/transformer-map transformers opts)))
        compile-opts (when-not (:stillsuit/no-default-resolver? opts)
                       {:default-field-resolver sr/default-resolver})
        compiled     (if (:stillsuit/compile? opts)
                       (schema/compile uncompiled compile-opts)
                       uncompiled)]
    (when (:stillsuit/trace? opts)
      (log/spy :trace uncompiled))
    {:stillsuit/schema      compiled
     :stillsuit/app-context (make-app-context context schema connection opts)}))
