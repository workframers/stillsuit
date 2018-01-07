(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [stillsuit.test-util.fixtures :as fixtures]
            [stillsuit.core :as stillsuit]
            [com.walmartlabs.lacinia :as lacinia]))

(use-fixtures :once fixtures/once)

(deftest test-scalars
  (testing "load database"
    (let [db       (fixtures/get-db :rainbow)
          schema   (fixtures/get-schema :rainbow)
          context  (fixtures/get-context :rainbow)
          compiled (stillsuit/decorate schema {:stillsuit/scalars  :all
                                               :stillsuit/entity   :true
                                               :stillsuit/compile? true})
          query    "{ datomicEntity(\"[:rainbow/id \\\"rainbow\\\"\") { dbId } }"
          result   (lacinia/execute compiled query nil context)]
      (is (some? db))
      (is (map? schema))
      (is (nil? (:errors result))))))
