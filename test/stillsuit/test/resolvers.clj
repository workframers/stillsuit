(ns stillsuit.test.resolvers
  (:require [clojure.test :refer :all]
            [stillsuit.test-util.fixtures :as fixtures]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.tools.logging :as log]))

(use-fixtures :once fixtures/once)

(def music-resolver-map
  {})

(deftest test-scalars-outbound
  (testing "load database"
    (let [{::fixtures/keys [context schema queries] :as opt} (fixtures/load-setup :music music-resolver-map)
          query  (:basic queries)
          result (lacinia/execute schema query nil context)
          data   (get-in result [:data :rainbowById])]
      (log/spy opt)
      (is (map? schema))
      (is (nil? (:errors result)))
      (is (some? data)))))
