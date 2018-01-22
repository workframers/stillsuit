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
   {:stillsuit/conn    conn
    :stillsuit/options options
    :stillsuit/db      db}))

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

(defn decorate
  ""
  ([base-schema-edn config]
   (decorate base-schema-edn config {}))
  ([base-schema-edn config resolver-map]
   (let [opts         (merge default-config config)
         uncompiled   (-> base-schema-edn
                          (ss/attach-scalars opts)
                          (sq/attach-queries opts)
                          (sr/attach-resolvers opts))
         compile-opts (when-not (:stillsuit/no-default-resolver? opts)
                        {:default-field-resolver sr/default-resolver})]
     (when (:stillsuit/trace? opts)
       (log/spy :trace uncompiled))
     (if (:stillsuit/compile? opts)
       (let [with-resolvers (util/attach-resolvers uncompiled (decorate-resolver-map resolver-map))
             with-scalars   (util/attach-scalar-transformers with-resolvers (ss/transformer-map opts))]
         (schema/compile with-scalars compile-opts))
       uncompiled))))
