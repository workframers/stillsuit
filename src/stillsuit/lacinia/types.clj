(ns stillsuit.lacinia.types
  "Implementation functions relating to lacinia types."
  (:require [stillsuit.datomic.core :as datomic]
            [cuerdas.core :as str]
            [clojure.tools.logging :as log]))

(defn lacinia-type
  "Given an entity, infer its lacinia type."
  [entity config]
  (let [entity-ns (datomic/guess-entity-ns entity)
        xform     (:stillsuit/ns-to-str config #(-> % str/kebab str/capitalize))]
    (log/spy entity-ns)
    (some-> entity-ns xform keyword)))
