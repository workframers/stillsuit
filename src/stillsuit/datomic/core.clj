(ns stillsuit.datomic.core
  (:require [datomic.api :as d]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

(defn entity? [thing]
  (instance? datomic.Entity thing))

(defn datomic-db? [thing]
  (instance? datomic.db.Db thing))

;; TODO: handle errors here
(defn coerce-to-datomic-type
  "Given an input value as a string (lacinia type 'ID) and a datomic value type,
  coerce the input to the proper type."
  [input datomic-type]
  (let [xform (case datomic-type
                :db.type/string str
                :db.type/instant #(java.util.Date/parse %)
                :db.type/bigdec bigdec
                :db.type/bigint bigint
                :db.type/long #(Long/parseLong %)
                :db.type/int #(Integer/parseInt %)
                :db.type/keyword keyword
                :db.type/uuid #(UUID/fromString %)
                (constantly nil))] ; todo: should throw
    (xform input)))

(defn get-entity-by-eid
  [db eid]
  (d/entity db (Long/parseLong eid)))

(defn get-entity-by-unique-attribute
  [db attribute-ident value]
  (if-let [attr-ent (d/entity db [:db/ident attribute-ident])]
    (if-let [coerced (coerce-to-datomic-type value (:db/valueType attr-ent))]
      (d/entity db [attribute-ident coerced])
      ;; Else coercion failed
      (log/warnf "Unable to coerce input '%s' to type %s in (get-entity-by-unique-attribute %s)"
                 value (:db/valueType attr-ent) attribute-ident))
    ;; Else atttribute not found
    (log/warnf "Attribute %s not found in (get-entity-by-unique-attribute)" attribute-ident)))

(defn guess-entity-ns
  "Given a random entity, iterate through its attributes and look for one that is marked
  as :db.unique/identity. Return the namespace of that attribute as a string."
  [entity]
  (when entity
    (let [db     (d/entity-db entity)
          unique (fn [attr-kw]
                   (let [attr-ent (d/entity db attr-kw)]
                     (when (some? (:db/unique attr-ent))
                       attr-kw)))]
      (some->> entity
               keys
               (remove #(= (namespace %) "db"))
               (some unique)
               namespace))))
