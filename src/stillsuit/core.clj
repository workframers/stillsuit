(ns stillsuit.core
  (:require [stillsuit.lacinia.queries :as sq]
            [stillsuit.lacinia.scalars :as ss]
            [datomic.api :as d]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]))

(defn app-context
  "Return an app-context map suitable for handing to (lacinia/execute-query)."
  ([{:keys [:stillsuit/datomic-uri] :as options}]
   (app-context options (d/connect datomic-uri)))
  ([options conn]
   (app-context options conn (d/db conn)))
  ([options conn db]
   {:stillsuit/conn conn
    :stillsuit/db   db}))

(defn decorate
  ""
  [base-schema-edn {:keys [:stillsuit/scalars :stillsuit/queries :stillsuit/compile?] :as options}]
  (cond-> base-schema-edn
    scalars  (ss/attach-scalars options)
    queries  (sq/attach-queries queries)
    ;true     (log/spy)
    compile? (schema/compile)))
