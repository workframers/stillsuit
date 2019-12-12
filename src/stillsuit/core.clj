(ns stillsuit.core
  "Main public API for stillsuit.

  For more details, see the [user manual](http://docs.workframe.com/stillsuit/current/manual/).

  Note that this namespace constitutes the entirety of the public API; all the other
  namespaces in stillsuit can be considered to be implementation details and may change
  over time."
  (:require [stillsuit.lacinia.queries :as sq]
            [stillsuit.lacinia.scalars :as ss]
            [stillsuit.lacinia.resolvers :as sr]
            [stillsuit.lacinia.enums :as se]
            [stillsuit.lib.util :as slu]
            [datomic.api :as d]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.util :as util]))

(defn- datomic-connect
  [db-uri]
  (if-not db-uri
    (log/error "No datomic URL defined in config or schema!")
    (let [conn (d/connect db-uri)]
      (log/infof "Connecting to datomic at %s..." db-uri)
      conn)))

(defn- make-app-context
  "Return an app-context map suitable for handing to (lacinia/execute-query)."
  [base-context schema connection enum-map entity-filters config]
  (let [context-conn (or connection (datomic-connect (or (:catchpocket/datomic-uri config)
                                                         (:stillsuit/datomic-uri schema))))]
    (merge {:stillsuit/connection     context-conn
            :stillsuit/enum-map       enum-map
            :stillsuit/config         config
            :stillsuit/entity-filters entity-filters}
           base-context)))

(defn- decorate-resolver-map
  [resolver-map config]
  (merge resolver-map
         (sr/resolver-map config)
         (sq/resolver-map config)))

(def ^:private base-schema
  (delay (slu/load-edn-resource "stillsuit/base-schema.edn")))

(def ^:private default-config-schema
  (delay (slu/load-edn-resource "stillsuit/config-defaults.edn")))

(defn datomic-enum
  "Given a stillsuit-decorated app context and a keyword representing a lacinia enum which has
  been described in the stillsuit config, return the keyword corresponding to the datomic value
  for that keyword.

  Parameters:

  - `app-context`: the lacinia application context (first argument to a resolver function)
  - `lacinia-type`: the keyword corresponding to a lacinia `:enum` definition
  - `lacinia-enum-keyword`: a keyword representing the value we're trying to convert to
    its datomic equivalent

  For more information, see [the user manual](http://docs.workframe.com/stillsuit/current/manual/#_stillsuit_enums)."
  [app-context lacinia-type lacinia-enum-keyword]
  (get-in app-context [:stillsuit/enum-map lacinia-type :stillsuit/lacinia-to-datomic lacinia-enum-keyword]))

(defn lacinia-enum
  "Reverse of datomic-enum above"
  [context lacinia-type datomic-keyword]
  (get-in context [:stillsuit/enum-map lacinia-type :stillsuit/datomic-to-lacinia datomic-keyword]))

(defn connection
  "Given a stillsuit-decorated app context, return the datomic connection object that was associated with the
  context at the time `(stillsuit/decorate)` was called.

  Parameters:

  - `app-context`: the lacinia application context (first argument to a resolver function)

  For more information, see [the user manual](http://docs.workframe.com/stillsuit/current/manual/#_stillsuit_enums)."
  [app-context]
  (:stillsuit/connection app-context))

(defn db
  "Given a stillsuit-decorated app context, get the most recent datomic db value from its datomic connection.

  Parameters:

  - `app-context`: the lacinia application context (first argument to a resolver function)

  For more information, see [the user manual](http://docs.workframe.com/stillsuit/current/manual/#_stillsuit_enums)."
  [app-context]
  (some-> app-context :stillsuit/connection d/db))

(defn decorate
  "Main interface to stillsuit. Accepts a map containing various parameters as input; returns
  a map with an app context and a schema. The map can be passed to [[execute]] in order to
  invoke lacinia with its configuration.

  The single argument to `(decorate)` should be a map with the following keys:

  - `:stillsuit/schema`: a normal
    [lacinia schema definition](http://lacinia.readthedocs.io/en/latest/tutorial/init-schema.html#schema-edn-file).
  - `:stillsuit/connection`: a datomic [connection object]().
  - `:stillsuit/resolvers`: a map of keywords to resolver function objects, identical to the
    map you'd pass to
    [`(lacinia.util/attach-resolvers)`](http://lacinia.readthedocs.io/en/latest/resolve/attach.html).
  - `:stillsuit/config` (optional): a map of
    [configuration options](http://docs.workframe.com/stillsuit/current/manual/#_compiling_a_schema)
    for stillsuit.
  - `:stillsuit/context` (optional): a map containing any custom information your resolvers need,
    identical to the `context` argument you'd pass to `(lacinia/execute)`.
  - `:stillsuit/transformers` (optional): a map of keywords to scalar transformer function objects,
    identical to the map you'd pass to
    [`(lacinia.util/attach-scalar-transformers)`](http://lacinia.readthedocs.io/en/latest/custom-scalars.html#attaching-scalar-transformers).
  - `:stillsuit/entity-filters` (optional): a map of keywords to entity filter functions, which are
    used to filter the results of :stillsuit/ref resolvers.

  The return value of this function is a map with two keys:

  - `:stillsuit/schema`: the compiled, transformed schema definition
  - `:stillsuit/app-context`: an application context object

  These two data structures can be passed to `(lacinia/execute)` directly, or there is a
  simple wrapper function [[execute]] that will invoke lacinia for you.

  For more information, see [the user manual](http://docs.workframe.com/stillsuit/current/manual/)."
  [{:stillsuit/keys [schema config resolvers transformers context connection entity-filters]}]
  (let [opts         (merge @default-config-schema (:stillsuit/config schema) config)
        uncompiled   (-> @base-schema
                         (slu/deep-map-merge schema)
                         (sq/attach-queries opts)
                         (sr/attach-resolvers opts)
                         (util/attach-resolvers (decorate-resolver-map resolvers opts))
                         (util/attach-scalar-transformers (ss/transformer-map transformers opts)))
        compile-opts (when-not (:stillsuit/no-default-resolver? opts)
                       {:default-field-resolver sr/default-resolver})
        enum-map     (se/make-enum-map opts uncompiled)
        compiled     (if (:stillsuit/compile? opts)
                       (schema/compile uncompiled compile-opts)
                       uncompiled)]
    (when (:stillsuit/trace? opts)
      (log/spy :trace uncompiled))
    {:stillsuit/schema      compiled
     :stillsuit/app-context (make-app-context context schema connection
                                              enum-map entity-filters opts)}))

(defn execute
  "Convenience function to take the result of [[decorate]] and execute a query against it.
  The `query` and `variables` parameters are the same ones that would be passed to
  `(lacinia.core/execute)`.

  For more information, see [the user manual](http://docs.workframe.com/stillsuit/current/manual/#__code_stillsuit_execute_code)."
  ([stillsuit-result query]
   (execute stillsuit-result query nil))
  ([stillsuit-result query variables]
   (let [schema  (:stillsuit/schema stillsuit-result)
         context (:stillsuit/app-context stillsuit-result)]
     (lacinia/execute schema query variables context))))
