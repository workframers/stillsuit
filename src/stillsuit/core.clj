(ns stillsuit.core
  (:require [stillsuit.lacinia.core :as sl]
            [stillsuit.datomic.core :as sd]
            [datomic.api :as d]))

(defn decorate
  ""
  [base-schema-edn])


(defn pull-star [db ref]
  (d/q '[:find (pull ?r [*]) .
         :in $ ?r]
    db ref))
