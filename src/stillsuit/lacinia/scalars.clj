(ns stillsuit.lacinia.scalars
  "Implementation functions for stillsuit scalar transformers.

  The intent here is to provide transformers for all of datomic's primitive values,
  though we ignore some oddball types like `:db.type/uri`."
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import (java.util Date UUID)
           (java.time OffsetDateTime LocalDateTime ZoneOffset Instant)
           (java.time.format DateTimeFormatter)
           (clojure.lang Keyword)))

(defn parse-edn [value-str]
  "Parse the incoming string as EDN."
  (edn/read-string value-str))

(def serialize-str
  "Basic convert-to-string serializer, which just relies on good old `(str)`"
  str)

(def serialize-pr-str pr-str)

(defn parse-iso8601
  "Parser which will parse dates in ISO-8601 format, convert them to UTC time, and return
  a `java.util.Date` result."
  [^String date-str]
  ;; Java time libraries, from Hell's heart I stab at thee
  (-> date-str
      (LocalDateTime/parse DateTimeFormatter/ISO_DATE_TIME)
      (.toInstant ZoneOffset/UTC)
      Date/from))

(defn serialize-iso8601
  "Serializer which will serialize dates as ISO-8601 strings in the UTC timezone."
  [^Date date]
  (-> date
      .toInstant
      (OffsetDateTime/ofInstant ZoneOffset/UTC)
      (.format DateTimeFormatter/ISO_DATE_TIME)))

(defn parse-epoch-millisecs
  "Parser converting number of milliseconds since the UTC epoch (as a string) to a
  `java.util.Date` value."
  [^String msec-str]
  (-> msec-str
      Long/parseLong
      Instant/ofEpochMilli
      Date/from))

(defn serialize-epoch-millisecs
  "Serializer representing a `java.util.Date` object as a string representing the number
  of milliseconds since the UTC epoch."
  [^Date date]
  (-> date
      .getTime
      .toString))

(defn parse-uuid
  [^String u]
  (UUID/fromString u))

(defn- strip-leading-colons
  [s]
  (str/replace s #"^:+" ""))

(defn keyword-parser
  [^String keyword-str]
  (-> keyword-str
      strip-leading-colons
      keyword))

(defn keyword-serializer
  [with-colon?]
  (let [colon-xform (if with-colon? identity strip-leading-colons)]
    (fn keyword-serialize-fn
      [^Keyword k]
      (-> k
          str
          colon-xform))))

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
          :stillsuit.parse/long                   #(Long/parseLong ^String %)
          :stillsuit.parse/bigint                 bigint
          :stillsuit.parse/bigdec                 bigdec
          :stillsuit.parse/double                 #(Double/parseDouble ^String %)
          :stillsuit.serialize/iso8601            serialize-iso8601
          :stillsuit.serialize/str                serialize-str
          :stillsuit.serialize/pr-str             serialize-pr-str
          :stillsuit.serialize/epoch-millisecs    serialize-epoch-millisecs
          :stillsuit.serialize/keyword-with-colon (keyword-serializer true)
          :stillsuit.serialize/keyword-no-colon   (keyword-serializer false)}))
