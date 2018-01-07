(ns stillsuit.core
  (:require [stillsuit.lacinia.core :as sl]
            [stillsuit.lacinia.scalars :as ss]
            [datomic.api :as d]
            [com.walmartlabs.lacinia.schema :as schema]))

(defn decorate
  ""
  [base-schema-edn {:keys [:stillsuit/scalars :stillsuit/queries :stillsuit/compile?] :as options}]
  (cond-> base-schema-edn
    scalars (ss/attach-scalars options)
    queries (sl/attach-queries queries)
    compile? (schema/compile)))
