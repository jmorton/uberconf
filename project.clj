(defproject jmorton/uberconf "0.1.0-SNAPSHOT"
  :description "Configuration made Über."
  :url "http://github.com/jmorton/uberconf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [clojure-ini "0.0.2"]
                 [prismatic/schema "1.1.3"]
                 [camel-snake-kebab "0.4.0"]]
  :profiles {:test {:jvm-opts ["-Dfoo.bar.x=1"
                               "-Dfoo.bar.y=2"
                               "-Dfoo.baz.x=3"]}})
