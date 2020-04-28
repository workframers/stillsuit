(defproject com.workframe/stillsuit "0.17.0-SNAPSHOT"
  :description "lacinia-datomic interface library"
  :url "https://github.com/workframers/stillsuit"
  :scm {:name "git" :url "https://github.com/workframers/stillsuit"}
  :pedantic? :warn
  :min-lein-version "2.8.1"
  :license {:name "Apache 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [org.clojure/tools.cli "0.4.1"]
                 [mvxcvi/puget "1.1.0"]
                 [fipp "0.6.17"]
                 [funcool/cuerdas "2.1.0"]
                 [io.aviso/pretty "0.1.37"]
                 [com.walmartlabs/lacinia "0.32.0"]
                 [clojure.java-time "0.3.2"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.reader "1.3.2"]
                 [com.datomic/datomic-free "0.9.5697"
                  :optional true
                  :scope "provided"
                  :exclusions [org.slf4j/slf4j-nop]]]

  :source-paths ["src"]

  :test-selectors {:watch :watch}

  :codox {:metadata   {:doc/format :markdown}
          :themes     [:rdash]
          :source-uri "https://github.com/workframers/stillsuit/blob/master/{filepath}#L{line}"}

  :asciidoctor [{:sources          "doc/manual/*.adoc"
                 :format           :html5
                 :source-highlight true
                 :to-dir           "target/manual"}]

  :profiles {:dev       {:plugins      [[lein-cloverage "1.1.1" :exclusions [org.clojure/clojure]]
                                        [lein-shell "0.5.0"]
                                        [com.jakemccrary/lein-test-refresh "0.23.0"]]
                         :dependencies [[vvvvalvalval/datomock "0.2.2"]
                                        [io.forward/yaml "1.0.9"]
                                        [org.apache.logging.log4j/log4j-core "2.11.2"]
                                        [org.apache.logging.log4j/log4j-slf4j-impl "2.11.2"]]}
             :free      {:dependencies [[com.datomic/datomic-free "0.9.5697"
                                         :exclusions [org.slf4j/slf4j-nop]]]}
             :docs      {:plugins      [[lein-codox "0.10.6"]
                                        [lein-asciidoctor "0.1.17" :exclusions [org.slf4j/slf4j-api]]]
                         :dependencies [[codox-theme-rdash "0.1.2"]]}
             :ancient   {:plugins [[lein-ancient "0.6.15"]]}
             :ultra     {:plugins [[venantius/ultra "0.6.0" :exclusions [org.clojure/clojure]]]}
             :test      {:resource-paths ["test/resources"]}
             :workframe {:plugins      [[s3-wagon-private "1.3.2" :exclusions [commons-logging]]]
                         :repositories [["workframe-private"
                                         {:url           "s3p://deployment.workframe.com/maven/releases/"
                                          :no-auth       true
                                          :sign-releases false}]]}}

  :aliases {"refresh" ["with-profile" "+ultra,+free" "test-refresh" ":watch"]}

  :release-tasks [;; Make sure we're up to date
                  ["vcs" "assert-committed"]
                  ["shell" "git" "checkout" "develop"]
                  ["shell" "git" "pull"]
                  ["shell" "git" "checkout" "master"]
                  ["shell" "git" "pull"]
                  ;; Merge develop into master
                  ["shell" "git" "merge" "develop"]
                  ;; Update version to non-snapshot version, commit change to master, tag
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "stillsuit-" "--no-sign"]
                  ;; Merge master back into develop (we'll now have the non-SNAPSHOT version)
                  ["shell" "git" "checkout" "develop"]
                  ["shell" "git" "merge" "master"]
                  ;; Bump up SNAPSHOT version in develop and commit
                  ["change" "version" "leiningen.release/bump-version" "minor"]
                  ["vcs" "commit"]
                  ;; All done
                  ["shell" "echo"]
                  ["shell" "echo" "Release tagged in master; develop bumped to ${:version}."]
                  ["shell" "echo" "To push it, run 'git push origin develop master --tags'"]])
