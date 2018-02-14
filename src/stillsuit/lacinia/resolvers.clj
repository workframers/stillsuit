(ns stillsuit.lacinia.resolvers
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
    ;(log/spy [entity graphql-field-name attr-kw value])
    (log/tracef "Resolved graphql field '%s' as %s, value %s" graphql-field-name attr-kw value)
    ;(if (instance? java.util.Date value)
    ;  (date-time-result value)
    value))

(defn- entity-sort
  [{:stillsuit/keys [sort-key sort-order] :as opts} entity-set]
  (let [comparator (if (= sort-order :ascending) compare #(compare %2 %1))
        keyfn      (or sort-key :db/id)]
    (sort-by keyfn comparator entity-set)))

(defn- ensure-single
  "Resolve a reference attribute whose resolver was tagged with :stillsuit/single?. Return
  a pair [value errors]. The error condition occurs if an attribute marked as single results
  in more than one result."
  [opts entity-set]
  (if (> (count entity-set) 1)
    [nil {:message (format "Expected a single %s result resolving attribute %s, but found %d results!"
                           (:stillsuit/lacinia-type opts)
                           (:stillsuit/attribute opts)
                           (count entity-set))}]
    ;; Else one or zero entities
    [(first entity-set) nil]))

(defn ref-resolver
  "Resolver used to get a literal attribute value out of an entity, eg in
  :resolve [:stillsuit/attribute {:stillsuit/attribute :artist/_country}]"
  [{:stillsuit/keys [attribute lacinia-type single?] :as opts}]
  ^resolve/ResolverResult
  (fn [context args entity]
    (let [value (get entity attribute)
          [sorted errs] (if (set? value)
                          (if single?
                            (ensure-single opts value)
                            [(entity-sort opts value) nil])
                          [value nil])]
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
                     (map #(get % attr-map) value)
                     (get attr-map value))]
      (when (and (some? value) (nil? mapped))
        (log/warnf "Unable to find mapping for datomic enum value %s for type %s, attribute %s!"
                   value lacinia-type attribute))
      (resolve/resolve-as
        (schema/tag-with-type (or mapped value) lacinia-type)))))

(defn datomic-entity-interface
  [config]
  (let [db-id (:stillsuit/db-id-name config :dbId)]
    {:description "Base type for datomic entities"
     :fields      {db-id {:type        'ID
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

