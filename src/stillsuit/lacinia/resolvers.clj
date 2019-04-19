(ns stillsuit.lacinia.resolvers
  "Implementation functions for stillsuit resolvers."
  (:require [stillsuit.datomic.core :as sd]
            [com.walmartlabs.lacinia.resolve :as resolve]
            [clojure.tools.logging :as log]
            [cuerdas.core :as str]
            [datomic.api :as d]
            [stillsuit.datomic.core :as datomic]
            [com.walmartlabs.lacinia.schema :as schema]))

(defn graphql-field->datomic-attribute
  "Given a datomic entity and a field name from GraphQL, try to look up the field name in
  the entity by inspecting the entity, looking for a unique attribute, and using that as
  the namespace for the keyword. Return the keyword corresponding to the attribute in datomic."
  [entity graphql-field-name options]
  (cond
    (= :dbId graphql-field-name)
    :db/id

    :else
    (let [entity-ns (datomic/guess-entity-ns entity)
          xform     (:stillsuit/ns-to-str options str/kebab)
          entity-kw (xform graphql-field-name)]
      (keyword entity-ns entity-kw))))

(defn get-graphql-value
  "Given a datomic entity and a field name from GraphQL, try to look up the field name in
  the entity by inspecting the entity, looking for a unique attribute, and using that as
  the namespace for the keyword."
  [entity graphql-field-name options]
  (let [attr-kw (graphql-field->datomic-attribute entity graphql-field-name options)
        value   (get entity attr-kw)]
    (log/tracef "Resolved graphql field '%s' as %s, value %s" graphql-field-name attr-kw value)
    value))

(defn- entity-sort
  [{:stillsuit/keys [sort-key sort-order] :as opts} entity-set]
  (let [comparator (if (= sort-order :ascending) compare #(compare %2 %1))
        keyfn      (or sort-key :db/id)]
    (sort-by keyfn comparator entity-set)))

(defn- ensure-single
  "Resolve a reference attribute whose resolver has :stillsuit/cardinality set to :stillsuit.cardinality/one.
  Return a pair [value errors]. The error condition occurs if an attribute marked as single results
  in more than one result."
  [opts entity-set]
  (if (> (count entity-set) 1)
    [nil {:message (format "Expected a single %s result resolving attribute %s, but found %d results!"
                           (:stillsuit/lacinia-type opts)
                           (:stillsuit/attribute opts)
                           (count entity-set))}]
    ;; Else one or zero entities
    [(first entity-set) nil]))

(defmulti ^:private ensure-cardinality
  "Based on the stillsuit options for a given ref field, ensure that it is either a
  single entity (for :stillsuit.cardinality/one) or a list of entities
  (for :stillsuit.cardinality/many)."
  (fn [opts multiple? _]
    (or (:stillsuit/cardinality opts)
        (if multiple?
          :stillsuit.cardinality/many
          :stillsuit.cardinality/one))))

(defmethod ensure-cardinality :stillsuit.cardinality/many [_ _ entities]
  [entities nil])

(defmethod ensure-cardinality :stillsuit.cardinality/one [opts _ entities]
  (if (> (count entities) 1)
    [nil {:message (format "Expected a single %s result resolving attribute %s, but found %d results!"
                           (:stillsuit/lacinia-type opts)
                           (:stillsuit/attribute opts)
                           (count entities))}]
    ;; Else one or zero entities
    [(first entities) nil]))

(defmulti ^:private ensure-type
  "Coerce the given datomic primitive value to be the same as the given lacinia type.
  Currently this just converts `nil` values to `false` for non-null Boolean fields."
  (fn [value lacinia-type] lacinia-type))
(defmethod ensure-type :default [value _] value)
(defmethod ensure-type '(non-null Boolean) [value _] (true? value))

(defn- sort-and-filter-entities
  "Given an app context and the option map to a ref resolver, and a set of entities
  representing the value or values to return from the resolver, check the option map
  for filtering and sorting options. Return a seq of filtered and sorted entities."
  [{:stillsuit/keys [entity-filter] :as opts} context entity-set]
  (let [referenced-filter (get-in context [:stillsuit/entity-filters entity-filter])
        filter-fn         (if-not (fn? referenced-filter)
                            (do
                              (when entity-filter
                                (log/warnf "Referenced entity-filter %s not found!" entity-filter))
                              (constantly true))
                            referenced-filter)]
    (->> entity-set
         (remove nil?)
         (filter (partial filter-fn opts context))
         (entity-sort opts))))

(defn ref-resolver
  "Resolver used to get a literal attribute value out of an entity, eg in
  :resolve [:stillsuit/ref {:stillsuit/attribute :artist/_country}]"
  [{:stillsuit/keys [attribute lacinia-type] :as opts}]
  ^resolve/ResolverResult
  (fn [context args entity]
    (let [value     (ensure-type (get entity attribute) lacinia-type)
          val-coll? (and (coll? value)
                         (not (map? value))
                         (not (sd/entity? value)))
          val-list  (if val-coll? (set value) #{value})
          filtered  (sort-and-filter-entities opts context val-list)
          [sorted errs] (ensure-cardinality opts val-coll? filtered)]
      (resolve/resolve-as
       (schema/tag-with-type sorted lacinia-type)
       errs))))

(defn enum-resolver
  "Resolver used to get an attribute value for a lacinia enum type. This uses the :stillsuit/enum-map
  map from the schema definition, which is attached to the app-context by (stillsuit/decorate)."
  [{:stillsuit/keys [attribute lacinia-type] :as opts}]
  ^resolve/ResolverResult
  (fn [context args entity]
    (let [value    (get entity attribute)
          attr-map (get-in context [:stillsuit/enum-map lacinia-type :stillsuit/datomic-to-lacinia])
          mapped   (if (set? value)
                     (map #(get attr-map %) value)
                     (get attr-map value))]
      (when (and (some? value) (nil? mapped))
        (log/warnf "Unable to find mapping for datomic enum value %s for type %s, attribute %s!"
                   value lacinia-type attribute))
      (resolve/resolve-as
       (when (or (some? mapped) (some? value))
         (schema/tag-with-type (or mapped value) lacinia-type))))))

(defn datomic-entity-interface
  [config]
  (let [db-id (:stillsuit/db-id-name config :dbId)]
    {:description "Base type for datomic entities"
     :fields      {db-id {:type        :JavaLong
                          :description "The entity's EID (as a string)"}}}))

(defn attach-resolvers [schema config]
  (let [entity-type (:stillsuit/datomic-entity-type config)]
    (-> schema
        (assoc-in [:interfaces entity-type] (datomic-entity-interface config)))))

(defn default-resolver
  [field-name]
  ^resolve/ResolverResult
  (fn [{:keys [:stillsuit/config]} args value]
    (resolve/resolve-as
     (if (sd/entity? value)
       (get-graphql-value value field-name config)
       (get value field-name)))))

(defn resolver-map [config]
  {:stillsuit/ref  ref-resolver
   :stillsuit/enum enum-resolver})
