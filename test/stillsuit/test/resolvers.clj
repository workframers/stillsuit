(ns stillsuit.test.resolvers
  (:require [clojure.test :refer :all]
            [stillsuit.test-util.fixtures :as fixtures]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [datomic.api :as d]
            [clojure.tools.reader.edn :as edn]))

(use-fixtures :once fixtures/once)
(use-fixtures :each fixtures/each)

(defn- get-artist-by-id
  [{:stillsuit/keys [db]} {:keys [id]} v]
  (some->> (d/q '[:find [?a ...]
                  :where [?a :artist/id ?id]]
                db id)
           (map (partial d/entity db))
           (sort-by :artist/name)))

(defn- get-all-artists
  [{:stillsuit/keys [db]} {:keys [id]} v]
  (some->> (d/q '[:find [?a ...]
                  :where [?a :artist/id]]
                db)
           (map (partial d/entity db))
           (sort-by :artist/name)))

(def music-resolver-map
  {:query/artist-by-id get-artist-by-id
   :query/all-artists  get-all-artists})

(deftest test-music-queries
  (fixtures/verify-queries! (fixtures/load-setup :music music-resolver-map)))
