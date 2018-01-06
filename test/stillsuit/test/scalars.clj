(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [stillsuit.test-util.fixtures :as fixtures]))

(use-fixtures :once fixtures/once)

(deftest test-scalars
  (testing "load database"
    (let [db     (fixtures/get-db :rainbow)
          schema (fixtures/get-schema :rainbow)]
      (is (some? db))
      (is (map? schema)))))
