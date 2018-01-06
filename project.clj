(defproject com.workframe/stillsuit "0.1.0-SNAPSHOT"
  :description "lacinia-datomic utilities"
  :url "https://github.com/workframers/stillsuit"
  :pedantic? :warn
  :license {:name "EPL"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/tools.cli "0.3.5"]
                 [mvxcvi/puget "1.0.2"]
                 [fipp "0.6.12"]
                 [funcool/cuerdas "2.0.4"]
                 [aero "1.1.2"]
                 [io.aviso/pretty "0.1.34"]
                 [com.walmartlabs/lacinia "0.23.0"]
                 [com.datomic/datomic-pro "0.9.5656"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.apache.logging.log4j/log4j-core "2.10.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.10.0"]]

  :min-lein-version "2.8.1"

  :source-paths ["src"]

  :profiles {:dev  {:plugins      [[lein-ancient "0.6.15"]
                                   [venantius/ultra "0.5.2" :exclusions [org.clojure/clojure]]
                                   [com.jakemccrary/lein-test-refresh "0.22.0"]]
                    :dependencies [[vvvvalvalval/datomock "0.2.0"]]}
             :test {:resource-paths ["test/resources"]}})
