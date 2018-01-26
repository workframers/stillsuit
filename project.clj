(defproject com.workframe/stillsuit "0.1.0-SNAPSHOT"
  :description "lacinia-datomic utilities"
  :url "https://github.com/workframers/stillsuit"
  :pedantic? :warn
  :min-lein-version "2.8.1"
  :license {:name "EPL"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.cli "0.3.5"]
                 [mvxcvi/puget "1.0.2"]
                 [fipp "0.6.12"]
                 [funcool/cuerdas "2.0.5"]
                 [aero "1.1.2"]
                 [io.aviso/pretty "0.1.34"]
                 [com.walmartlabs/lacinia "0.24.0-rc-2"]
                 [com.datomic/datomic-pro "0.9.5656"]
                 [clojure.java-time "0.3.1"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.apache.logging.log4j/log4j-core "2.10.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.10.0"]

                 [org.clojure/tools.reader "1.1.1"]]

  :plugins [[s3-wagon-private "1.3.1" :exclusions [commons-logging]]]

  :repositories [["workframe-private" {:url     "s3p://deployment.workframe.com/maven/releases/"
                                       :no-auth true}]]

  :source-paths ["src"]

  :test-selectors {:watch :watch}

  :codox {:metadata   {:doc/format :markdown}
          :themes     [:rdash]
          :source-uri "https://github.com/workframers/stillsuit/blob/develop/{filepath}#L{line}"}

  :asciidoctor {:sources "doc/*.adoc"
                :format  :html5
                :to-dir  "target/manual"}

  :profiles {:dev  {:plugins      [[lein-ancient "0.6.15"
                                    :exclusions [commons-logging
                                                 com.fasterxml.jackson.core/jackson-annotations
                                                 com.fasterxml.jackson.core/jackson-core
                                                 com.fasterxml.jackson.core/jackson-databind]]
                                   [venantius/ultra "0.5.2" :exclusions [org.clojure/clojure]]
                                   [lein-cloverage "1.0.10"]
                                   [lein-codox "0.10.3"]
                                   [lein-asciidoctor "0.1.14" :exclusions [org.slf4j/slf4j-api]]
                                   [com.jakemccrary/lein-test-refresh "0.22.0"]]
                    :dependencies [[vvvvalvalval/datomock "0.2.0"]
                                   [codox-theme-rdash "0.1.2"]
                                   [io.forward/yaml "1.0.6"]]}
             :test {:resource-paths ["test/resources"]}})
