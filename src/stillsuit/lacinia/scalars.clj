(ns stillsuit.lacinia.scalars
  (:require [com.walmartlabs.lacinia.schema :as schema]
            [clojure.edn :refer [read-string]]
            [clojure.tools.logging :as log])
  (:import (java.util Date UUID))
  (:refer-clojure :exclude [read-string]))

(defn date->string
  [^Date d])

(defn ^Date string->date
  [^Date d])

(defn parse-val
  [xform]
  (schema/as-conformer
    (fn [thing]
      (let [value (if (string? thing)
                    (read-string thing)
                    thing)]
        (xform value)))))

(def scalar-options
  "Map from datomic db.type values to data for custom scalars"
  {:db.type/bigdec
   {::name      :JavaBigDec
    ::parse     (parse-val bigdec)
    ::serialize (schema/as-conformer str)
    ::description "A Java BigDecimal value, serialized as a string."}
   :db.type/bigint
   {::name      :JavaBigInt
    ::parse     (parse-val bigint)
    ::serialize (schema/as-conformer str)
    ::description "A Java BigInteger value, serialized as a string."}
   :db.type/long
   {::name      :JavaLong
    ::parse     (parse-val long)
    ::serialize (schema/as-conformer str)
    ::description "A Java long value, serialized as a string (because it can be more than 32 bits)."}
   :db.type/keyword
   {::name      :ClojureKeyword
    ::parse     (parse-val keyword)
    ::serialize (schema/as-conformer str)
    ::description "A Clojure keyword value, serialized as a string."}
   :db.type/instant
   {::name      :JavaDate
    ::parse     (schema/as-conformer read-string)
    ::serialize (schema/as-conformer pr-str)
    ::description "A java.util.Date value, serialized as a string."}
   :db.type/uuid
   {::name      :JavaUUID
    ::parse     (parse-val #(UUID/fromString (str %)))
    ::serialize (schema/as-conformer str)
    ::description "A java.util.UUID value, serialized as a string."}})

(defn attach-scalar [scalars db-type]
  (let [{:keys [::name ::parse ::serialize ::description]} (get scalar-options db-type)]
    (assoc scalars name {:parse parse :serialize serialize :description description})))

(defn- attach-overrides
  [schema overrides]
  (assoc schema :scalars (reduce attach-scalar (:scalars schema) overrides)))

(defn attach-scalars
  [schema {:keys [:stillsuit/scalars] :as config}]
  (cond-> schema
    (not (:stillsuit.scalar/skip-defaults? config))
    (attach-overrides (-> scalar-options keys set))

    (set? (:stillsuit.scalar/for-fields scalars))
    (attach-overrides (:stillsuit.scalar/for-fields scalars))))
