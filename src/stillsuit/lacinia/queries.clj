(ns stillsuit.lacinia.queries
  (:require [com.walmartlabs.lacinia.resolve :as resolve]
            [stillsuit.datomic.core :as sd]
            [cuerdas.core :as str]
            [clojure.tools.logging :as log]
            [datomic.api :as d]))

(defn entity-query-by-eid
  [{:keys [:stillsuit/db] :as context} args value]
  (d/entity db (:eid args)))

(defn guess-entity-ns
  "Given a random entity, iterate through its attributes and look for one that is marked
  as :db.unique/identity. Return the namespace of that attribute as a string."
  [entity]
  (when entity
    (let [db     (d/entity-db entity)
          unique (fn [attr-kw]
                   (let [attr-ent (d/entity db attr-kw)]
                     (when (= :db.unique/identity (:db/unique attr-ent))
                       attr-kw)))]
      (some->> entity
               keys
               (some unique)
               namespace))))

(defn graphql-field->datomic-attribute
  "Given a datomic entity and a field name from GraphQL, try to look up the field name in
  the entity by inpsecting the entity, looking for a unique attribute, and using that as
  the namespace for the keyword. Return the keyword corresponding to the attribute in datomic."
  [entity graphql-field-name options]
  (cond
    (= :dbId graphql-field-name)
    :db/id

    :else
    (let [entity-ns (guess-entity-ns entity)
          xform     (:stillsuit/ns-to-str options str/kebab)
          entity-kw (xform graphql-field-name)]
      (keyword entity-ns entity-kw))))

(defn get-graphql-value
  "Given a datomic entity and a field name from GraphQL, try to look up the field name in
  the entity by inpsecting the entity, looking for a unique attribute, and using that as
  the namespace for the keyword."
  [entity graphql-field-name options]
  (let [attr-kw (graphql-field->datomic-attribute entity graphql-field-name options)
        value   (get entity attr-kw)]
    (log/tracef "Resolved graphql field '%s' as %s, value %s" graphql-field-name attr-kw value)
    ;(if (instance? java.util.Date value)
    ;  (date-time-result value)
    value))

(defn ref-resolver
  [field-name]
  ^resolve/ResolverResult
  (fn [{:keys [:stillsuit/options]} args value]
    (log/spy args value)
    (resolve/resolve-as
      (if (sd/entity? value)
        (get-graphql-value value field-name options)
        (get value field-name)))))

(defn default-resolver
  [field-name]
  ^resolve/ResolverResult
  (fn [{:keys [:stillsuit/options]} args value]
    (resolve/resolve-as
      (if (sd/entity? value)
        (get-graphql-value value field-name options)
        (get value field-name)))))



(defn attach-queries [schema options]
  schema)
