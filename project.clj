(defproject com.workframe/stillsuit "0.1.0-SNAPSHOT"
  :description "lacinia-datomic utilities"
  :url "https://github.com/workframers/stillsuit"
  :pedantic? :warn
  :license {:name "MIT"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/tools.cli "0.3.5"]
                 [mvxcvi/puget "1.0.2"]
                 [fipp "0.6.12"]
                 [funcool/cuerdas "2.0.4"]
                 [aero "1.1.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [io.aviso/pretty "0.1.34"]
                 [com.datomic/datomic-pro "0.9.5656"]
                 [com.datomic/clj-client "0.8.606"]]

  :min-lein-version "2.8.1"

  :source-paths ["src"]

  :profiles {:dev {:plugins [[lein-ancient "0.6.15"]]}})
