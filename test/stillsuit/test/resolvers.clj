(ns stillsuit.test.resolvers
  (:require [clojure.test :refer :all]
            [stillsuit.test-util.fixtures :as fixtures]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            [stillsuit.core :as stillsuit]))

(use-fixtures :once fixtures/once)
(use-fixtures :each fixtures/each)

(defn- get-artist-by-id
  [context {:keys [id]} v]
  (let [db (stillsuit/db context)]
    (some->> (d/q '[:find ?a .
                    :in $ ?id
                    :where [?a :artist/id ?id]]
                  db (Long/parseLong id))
             (d/entity db))))

(defn- get-all-artists
  [context {:keys [id]} v]
  (let [db (stillsuit/db context)]
    (some->> (d/q '[:find [?a ...]
                    :where [?a :artist/id]]
                  db)
             (map (partial d/entity db))
             (sort-by :artist/name))))

(defn first-track-filter [opts context entity]
  (= (:track/position entity) 1))

(def music-resolver-map
  {:query/artist-by-id get-artist-by-id
   :query/all-artists  get-all-artists})

(def filters
  {:music-filter/first-track first-track-filter})

(deftest test-music-queries
  (fixtures/verify-queries! (fixtures/load-setup :music music-resolver-map filters)))
