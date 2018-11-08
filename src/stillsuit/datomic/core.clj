(ns stillsuit.datomic.core
  "Implementation functions for dealing with datomic interactions."
  (:require [datomic.api :as d]
            [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:import (java.util UUID)))

(defn entity? [thing]
  (instance? datomic.Entity thing))

(defn datomic-db? [thing]
  (instance? datomic.db.Db thing))

;; TODO: handle errors here
(defn coerce-to-datomic-type
  "Given an input value as a string (lacinia type 'ID) and a datomic value type,
  coerce the input to the proper type."
  [^String input datomic-type]
  (let [xform (case datomic-type
                :db.type/string identity
                :db.type/instant #(java.util.Date/parse %)
                :db.type/bigdec bigdec
                :db.type/bigint bigint
                :db.type/long #(Long/parseLong %)
                :db.type/int #(Integer/parseInt %)
                :db.type/keyword keyword
                :db.type/uuid #(UUID/fromString %)
                (do
                  (log/errorf "Unknown datomic type %s encountered, returning '%s' as string"
                              datomic-type input)
                  identity))]
    (xform input)))

(defn get-entity-by-eid
  [db eid]
  (d/entity db (Long/parseLong eid)))

(defn get-entity-by-unique-attribute
  [db attribute-ident value]
  (if-let [attr-ent (d/entity db [:db/ident attribute-ident])]
    (if-let [coerced (if (string? value)
                       (coerce-to-datomic-type value (:db/valueType attr-ent))
                       value)]
      (d/entity db [attribute-ident coerced])
      ;; Else coercion failed
      (log/warnf "Unable to coerce input '%s' to type %s in (get-entity-by-unique-attribute %s)"
                 value (:db/valueType attr-ent) attribute-ident))
    ;; Else attribute not found
    (log/warnf "Attribute %s not found in (get-entity-by-unique-attribute)" attribute-ident)))

(defn identity-attributes
  "Return a seq of the attributes for a given entity which are unique"
  [entity]
  (let [db (d/entity-db entity)]
    (d/q '[:find [?id ...]
           :in $ ?e
           :where
           [?e ?a]
           [?a :db/unique :db.unique/identity]
           [(not= ?a :db/id)]
           [(datomic.api/attribute $ ?a) ?attr]
           [(:ident ?attr) ?id]]
         db (:db/id entity))))

(defn guess-entity-ns
  "Given a random entity, iterate through its attributes and look for one that is marked
  as :db.unique/identity. Return the namespace of that attribute as a string."
  [entity]
  (when entity
    (let [attrs (identity-attributes entity)
          nses  (->> attrs (map namespace) distinct)]
      (cond
        (empty? nses)
        (do (log/warnf "Could not find unique attribute for:\n%s\nField resolution probably won't work!!"
                       (d/touch entity))
            nil)

        (not= (count nses) 1)
        (do (log/warnf "Found nultiple unique namespaces '%s' in entity:\n%s\nField resolution probably won't work!!"
                       (string/join ", " nses) (d/touch entity))
            nil)

        :else
        (first nses)))))
