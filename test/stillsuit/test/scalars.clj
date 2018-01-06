(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [stillsuit.test-util.fixtures :as fixtures]))

(deftest test-scalars
  (testing "one and one"
    (is (some? (fixtures/provision-db :rainbow)))))
