(ns uberconf.core-test
  (:require [clojure.test :refer :all]
            [uberconf.core :refer :all]
            [schema.core :as schema]
            [schema.coerce :as coerce]))

(def DbSchema
  "Schema for testing coerce and validate"
  {:hosts [schema/Str]
   :port  schema/Num})

(def cli-spec [[nil "--foo VALUE"]
               [nil "--bar VALUE"]
               [nil "--baz VALUE"]])

(def cli-args ["--foo" "cli-foo-val"
               "--bar" "cli-bar-val"
               "--baz=cli-baz-val"])

(deftest ini-test
  (testing "reading ini"
    (let [cfg (ini->cfg "test/conf/example.ini")]
      (is (= "ini-dev-foo-val" (get-in cfg [:dev :foo]))))))

(deftest edn-test
  (testing "reading edn"
    (let [cfg (edn->cfg "test/conf/example.edn")]
      (is (= "edn-dev-foo-val" (get-in cfg [:dev :foo]))))))

(deftest env-test
  (testing "reading a few thing from env"
    (let [cfg (env->cfg {:prefix "foo."})]
      (is (= "1" (get-in cfg [:bar :x])))
      (is (= "2" (get-in cfg [:bar :y])))
      (is (= "3" (get-in cfg [:baz :x]))))))

(deftest cli-test
  (testing "reading command line opts"
    (let [args cli-args
          spec cli-spec
          cfg (cli->cfg args spec)]
      (is (= "cli-foo-val" (get-in cfg [:foo]))))))

(deftest coerce-test
  (testing "turning a string into a list of strings"
    (let [cfg {:hosts "db1, db2, db3"
               :port  "1024"}
          fix (coerce-cfg DbSchema cfg)]
      (is (check-cfg DbSchema fix))
      (is (= 1024 (:port fix)))
      (is (= ["db1" "db2" "db3"] (:hosts fix))))))

(deftest schema-test
  (testing "config is a keyword, string map"
    ;; Covering the case where environment variable
    ;; capitalization comes into play. I think it's
    ;; okay to essentially ignore case differences.
    (let [cfg (build-cfg {:env {:prefix "foo."}
                          :ini "test/conf/test.ini"
                          :edn "test/conf/test.edn"})]
      (is (check-cfg cfg)))))
