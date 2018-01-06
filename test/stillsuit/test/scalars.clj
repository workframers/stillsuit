(ns stillsuit.test.scalars
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(deftest test-scalars
  (testing "one and one"
    (log/debug "info")
    (log/info "info")
    (log/warn "warn")
    (log/error "error")
    (log/fatal "fatal")

    (println (str "resource:" (io/resource "log4j2.xml")))

    (is (= 3 (+ 1 1)))))
