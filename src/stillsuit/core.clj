(ns stillsuit.core
  (:require [stillsuit.lacinia.queries :as sq]
            [stillsuit.lacinia.scalars :as ss]
            [stillsuit.lacinia.resolvers :as sr]
            [datomic.api :as d]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.util :as util]))

(defn app-context
  "Return an app-context map suitable for handing to (lacinia/execute-query)."
  ([options]
   (let [datomic-uri (or (:stillsuit/datomic-uri options) (:catchpocket/datomic-uri options))]
     (log/debugf "Connecting to datomic at %s..." datomic-uri)
     (app-context options (d/connect datomic-uri))))
  ([options conn]
   (app-context options conn (d/db conn)))
  ([options conn db]
   {:stillsuit/conn conn
    :stillsuit/db   db}))

(def default-options
  {:stillsuit/datomic-entity-type  :DatomicEntity
   :stillsuit/entity-id-query-name :entity_by_eid})

(defn decorate-resolver-map
  ([resolver-map]
   (decorate-resolver-map resolver-map nil))
  ([resolver-map options]
   (let [with-defaults (merge default-options options)]
     (merge resolver-map
            (sr/resolver-map with-defaults)
            (sq/resolver-map with-defaults)))))

(defn decorate
  ""
  [base-schema-edn {:keys [:stillsuit/scalars :stillsuit/compile?] :as config}]
  (let [opts       (merge default-options config)
        uncompiled (-> base-schema-edn
                       (ss/attach-scalars opts)
                       (sq/attach-queries opts)
                       (sr/attach-resolvers opts))]
    (when (:stillsuit/trace? opts)
      (log/spy :trace uncompiled))
    (if compile?
      (let [with-resolvers (util/attach-resolvers uncompiled (decorate-resolver-map {}))]
        (schema/compile with-resolvers {:default-field-resolver sr/default-resolver}))
      uncompiled)))
