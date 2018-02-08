(ns stillsuit.lacinia.scalars
  (:require [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn])
  (:import (java.util Date UUID)
           (java.time ZonedDateTime))
  (:refer-clojure :exclude [read-string]))

(def scalar-options
  "Map from datomic db.type values to data for custom scalars"
  {:db.type/bigdec
   {::scalar      :JavaBigDec
    ::parse       :stillsuit.scalars/parse-bigdec
    ::description "A Java BigDecimal value, serialized as a string."}
   :db.type/bigint
   {::scalar      :JavaBigInt
    ::parse       :stillsuit.scalars/parse-bigint
    ::description "A Java BigInteger value, serialized as a string."}
   :db.type/long
   {::scalar      :JavaLong
    ::description "A Java long value, serialized as a string (because it can be more than 32 bits)."}
   :db.type/keyword
   {::scalar      :ClojureKeyword
    ::description "A Clojure keyword value, serialized as a string."}
   :db.type/instant
   {::scalar      :JavaDate
    ::serialize   :stillsuit.scalars/serialize-java-date
    ::parse       :stillsuit.scalars/parse-java-date
    ::description "A java.util.Date value, serialized as an ISO-8601 string in the UTC time zone."}
   :db.type/float
   {::scalar      :JavaDouble
    ::parse       :stillsuit.scalars/parse-double
    ::description "A Java float value, serialized as a string."}
   :db.type/double
   {::scalar      :JavaDouble
    ::parse       :stillsuit.scalars/parse-double
    ::description "A Java double value, serialized as a string."}
   :db.type/uuid
   {::scalar      :JavaUUID
    ::parse       :stillsuit.scalars/parse-uuid
    ::description "A java.util.UUID value, serialized as a string."}})

(defn attach-scalar [scalars db-type]
  (let [{:keys [::scalar ::parse ::serialize ::description]} (get scalar-options db-type)]
    (assoc scalars scalar {:parse       (or parse :stillsuit.scalars/parse-edn)
                           :serialize   (or serialize :stillsuit.scalars/serialize-str)
                           :description description})))

(defn- attach-overrides
  [schema overrides]
  (assoc schema :scalars (reduce attach-scalar (:scalars schema) overrides)))

(def parse-edn
  (schema/as-conformer edn/read-string))

(def serialize-str
  (schema/as-conformer str))

(def serialize-pr-str
  (schema/as-conformer pr-str))

(def serialize-java-date
  (schema/as-conformer
   (fn [^Date date]
     (-> date
         .toInstant
         .toString))))

(def parse-uuid
  (schema/as-conformer (fn [^String u] (UUID/fromString u))))

(def parse-java-date
  (schema/as-conformer
   (fn [^String d]
     ;; Java time libraries, from Hell's heart I stab at thee
     (-> (ZonedDateTime/parse d)
         .toInstant
         Date/from))))

(defn parse-as-value
  [type-convert]
  (schema/as-conformer (fn [^String k]
                         (type-convert (edn/read-string k)))))

(defn transformer-map
  "Given a base resolver map used attach scalar transformers to a lacinia schema, attach the
  transformers for datomic primitive types."
  [base-map config]
  (merge base-map
         {:stillsuit.scalars/parse-edn           parse-edn
          :stillsuit.scalars/serialize-str       serialize-str
          :stillsuit.scalars/serialize-java-date serialize-java-date
          :stillsuit.scalars/serialize-pr-str    serialize-pr-str
          :stillsuit.scalars/parse-uuid          parse-uuid
          :stillsuit.scalars/parse-java-date     parse-java-date
          :stillsuit.scalars/parse-bigint        (parse-as-value bigint)
          :stillsuit.scalars/parse-bigdec        (parse-as-value bigdec)
          :stillsuit.scalars/parse-double        (parse-as-value double)}))

(defn attach-scalars
  "Given a lacinia schema, add in the scalar transformer definitions to convert from datomic
  types to serialized GraphQL values."
  [schema {:keys [:stillsuit/scalars] :as config}]
  (cond-> schema
          (not (:stillsuit.scalar/skip-defaults? config))
          (attach-overrides (-> scalar-options keys set))

          (set? (:stillsuit.scalar/for-fields scalars))
          (attach-overrides (:stillsuit.scalar/for-fields scalars))))
