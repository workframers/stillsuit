(ns stillsuit.core
  (:require [stillsuit.lacinia.queries :as sq]
            [stillsuit.lacinia.scalars :as ss]
            [stillsuit.lacinia.resolvers :as sr]
            [stillsuit.lacinia.enums :as se]
            [datomic.api :as d]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.util :as util]))

(defn datomic-connect
  [db-uri]
  (if-not db-uri
    (log/error "No datomic URL defined in config or schema!")
    (let [conn (d/connect db-uri)]
      (log/infof "Connecting to datomic at %s..." db-uri)
      conn)))


(defn make-app-context
  "Return an app-context map suitable for handing to (lacinia/execute-query)."
  [base-context schema connection enum-map config]
  (let [context-conn (or connection (datomic-connect (or (:catchpocket/datomic-uri config)
                                                         (:stillsuit/datomic-uri schema))))]
    (merge {:stillsuit/connection context-conn
            :stillsuit/enum-map   enum-map
            :stillsuit/config     config}
           base-context)))

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

(defn datomic-enum
  "Given a stillsuit-decorated app context and a keyword representing a lacinia enum which has
  been described in the stillsuit config, return the keyword corresponding to the datomic value
  for that keyword."
  [app-context lacinia-type lacinia-enum-keyword]
  (let [value (get-in app-context [:stillsuit/enum-map lacinia-type :stillsuit/lacinia-to-datomic lacinia-enum-keyword])]
    (when (nil? value)
      (log/warnf "Unable to find datomic enum equivalent for lacinia enum value %s!" lacinia-enum-keyword))
    value))

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
        enum-map     (se/make-enum-map opts uncompiled)
        compiled     (if (:stillsuit/compile? opts)
                       (schema/compile uncompiled compile-opts)
                       uncompiled)]
    (when (:stillsuit/trace? opts)
      (log/spy :trace uncompiled))
    {:stillsuit/schema      compiled
     :stillsuit/app-context (make-app-context context schema connection enum-map opts)}))
