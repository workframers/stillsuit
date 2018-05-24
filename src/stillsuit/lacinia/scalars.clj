(ns stillsuit.lacinia.scalars
  "Implementation functions for stillsuit scalar transformers.

  The intent here is to provide transformers for all of datomic's primitive values,
  though we ignore some oddball types like `:db.type/uri`."
  (:require [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import (java.util Date UUID)
           (java.time OffsetDateTime LocalDateTime ZoneOffset Instant)
           (java.time.format DateTimeFormatter)
           (clojure.lang Keyword)))

(def parse-edn
  "Parse the incoming string as EDN."
  (schema/as-conformer edn/read-string))

(def serialize-str
  (schema/as-conformer str))

(def serialize-pr-str
  (schema/as-conformer pr-str))

(def parse-iso8601
  "Parser which will parse dates in ISO-8601 format, convert them to UTC time, and return
  a `java.util.Date` result."
  (schema/as-conformer
   (fn [^String date-str]
     ;; Java time libraries, from Hell's heart I stab at thee
     (-> date-str
         (LocalDateTime/parse DateTimeFormatter/ISO_DATE_TIME)
         (.toInstant ZoneOffset/UTC)
         Date/from))))

(def serialize-iso8601
  "Serializer which will serialize dates as ISO-8601 strings in the UTC timezone."
  (schema/as-conformer
   (fn [^Date date]
     (-> date
         .toInstant
         (OffsetDateTime/ofInstant ZoneOffset/UTC)
         (.format DateTimeFormatter/ISO_DATE_TIME)))))

(def parse-epoch-millisecs
  "Parser converting number of milliseconds since the UTC epoch (as a string) to a
  `java.util.Date` value."
  (schema/as-conformer
   (fn [^String msec-str]
     (-> msec-str
         Long/parseLong
         Instant/ofEpochMilli
         Date/from))))

(def serialize-epoch-millisecs
  "Serializer representing a `java.util.Date` object as a string representing the number
  of milliseconds since the UTC epoch."
  (schema/as-conformer
   (fn [^Date date]
     (-> date
         .getTime
         .toString))))

(def parse-uuid
  (schema/as-conformer (fn [^String u] (UUID/fromString u))))

(defn parse-as-value
  [type-convert]
  (schema/as-conformer (fn [^String k] (type-convert k))))

(defn- strip-leading-colons
  [s]
  (str/replace s #"^:+" ""))

(def keyword-parser
  (schema/as-conformer
   (fn keyword-parse-fn
     [^String keyword-str]
     (-> keyword-str
         strip-leading-colons
         keyword))))

(defn keyword-serializer
  [with-colon?]
  (let [colon-xform (if with-colon? identity strip-leading-colons)]
    (schema/as-conformer
     (fn keyword-serialize-fn
       [^Keyword k]
       (-> k
           str
           colon-xform)))))

(defn transformer-map
  "Given a base resolver map used attach scalar transformers to a lacinia schema, attach the
  transformers for datomic primitive types."
  [base-map config]
  (merge base-map
         {:stillsuit.parse/edn                    parse-edn
          :stillsuit.parse/uuid                   parse-uuid
          :stillsuit.parse/iso8601                parse-iso8601
          :stillsuit.parse/epoch-millisecs        parse-epoch-millisecs
          :stillsuit.parse/keyword                keyword-parser
          :stillsuit.parse/long                   (parse-as-value #(Long/parseLong ^String %))
          :stillsuit.parse/bigint                 (parse-as-value bigint)
          :stillsuit.parse/bigdec                 (parse-as-value bigdec)
          :stillsuit.parse/double                 (parse-as-value #(Double/parseDouble ^String %))
          :stillsuit.serialize/iso8601            serialize-iso8601
          :stillsuit.serialize/str                serialize-str
          :stillsuit.serialize/pr-str             serialize-pr-str
          :stillsuit.serialize/epoch-millisecs    serialize-epoch-millisecs
          :stillsuit.serialize/keyword-with-colon (keyword-serializer true)
          :stillsuit.serialize/keyword-no-colon   (keyword-serializer false)}))
