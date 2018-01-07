(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [stillsuit.test-util.fixtures :as fixtures]
            [stillsuit.core :as stillsuit]))

(use-fixtures :once fixtures/once)

(deftest test-scalars
  (testing "load database"
    (let [db       (fixtures/get-db :rainbow)
          schema   (fixtures/get-schema :rainbow)
          query    ""
          compiled (stillsuit/decorate schema {:stillsuit/scalars  :all
                                               :stillsuit/entity   :true
                                               :stillsuit/compile? true})]
      (is (some? db))
      (is (map? schema)))))
