(ns uberconf.core
  ""
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure-ini.core :as ini]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [schema.core :as schema]
            [schema.coerce :as coerce]))

(def opt-spec [])

(def cfg-schema
  {schema/Keyword schema/Any})

(def defaults
  {:args *command-line-args*
   :spec opt-spec
   :schema cfg-schema})

(defn ini->cfg
  "Get .ini file as map."
  [path]
  (transform-keys ->kebab-case-keyword
                  (ini/read-ini path)))

(defn edn->cfg
  "Get .edn file as map."
  [path]
  (transform-keys ->kebab-case-keyword
                  (edn/read-string (slurp path))))

(defn key-prefix? [[k v] prefix]
  (clojure.string/starts-with? k prefix))

(defn key-change [[k v] prefix]
  [(clojure.string/replace-first k (re-pattern prefix) "") v])

(defn key-split [[k v] separator]
  [(clojure.string/split k (re-pattern separator)) v])

(defn deep-map
  ([config prefix separator]
   (->> config
        (filter #(key-prefix? % prefix))
        (map    #(key-change % prefix))
        (map    #(key-split % separator))
        (into {})
        (reduce-kv #(assoc-in %1 %2 %3) {})
        (transform-keys ->kebab-case-keyword)))
  ([config]
   (deep-map config "" #"\.")))

(defn env->cfg
  "Get environment, including java properties, as map."
  ([{:keys [prefix separator]
     :or   {prefix "" separator #"\."}
     :as   opts}]
   (let [props (System/getProperties)
         env (into {} (System/getenv))
         env+props (merge env props)]
     (deep-map env+props prefix separator))))

(defn cli->cfg
  "Get command line arguments as map."
  [args opt-spec]
  (transform-keys ->kebab-case-keyword
                  (-> (cli/parse-opts args opt-spec) :options)))

;;; Coercing functions

(defn string->strings [schema]
  (let [comma #"[, ]+"]
    (when (= [schema/Str] schema)
      (coerce/safe
       (fn [value]
         (if (string? value)
           (map clojure.string/trim
                (clojure.string/split value comma))
           value))))))

(defn string->numeric [schema]
  (let [nf (java.text.NumberFormat/getInstance)]
    (when (= schema/Num schema)
      (coerce/safe
       (fn [value]
         (if (string? value)
           (.parse nf value)
           value))))))

(def config-coercers (coerce/first-matcher [string->numeric
                                            string->strings]))

(def Config
  "A schema for config maps"
  {schema/Keyword schema/Any})

(defn check-cfg
  "Validate cfg against schema."
  ([cfg]
   (schema/validate Config cfg))
  ([schema cfg]
   (schema/validate schema cfg)))

(defn coerce-cfg
  "Transform values of config using coercers."
  [schema cfg]
  (let [cfn (coerce/coercer schema config-coercers)]
    (cfn cfg)))

(defn build-cfg
  "Helper function that invokes each cfg-map building function."
  [{:keys [ini edn args spec env] :as opts}]
  (merge (if env (env->cfg env) {})
         (if ini (ini->cfg ini) {})
         (if edn (edn->cfg edn) {})
         (if (and args spec (cli->cfg args spec)) {})))

(defn init-cfg
  "Produce a validated configuration map. When configuration is
  built in the context of another system, you may want to compose
  a schema for only the components you will use."
  [{schema :schema :as args}]
  (->> (build-cfg args)
       (coerce-cfg schema)
       (check-cfg schema)))
