(ns stillsuit.lacinia.enums
  "Implementation functions related to translating enum values between lacinia and stillsuit."
  (:require [clojure.tools.logging :as log]))

(defn make-enum-map
  "Look through a decorated lacinia schema, scanning its enum values. For each enum description,
  check for a `:stillsuit/datomic-value` key, representing what value the enum should have in the
  datomic database.

  Return a map with an entry for each enum which contains such a key. Inside of this map will be
  two entries: `:stillsuit/datomic-to-lacinia` maps from datomic names to their lacinia enum
  counterparts, and `:stillsuit/lacinia-to-datomic` goes the other way. These maps are stored
  in the app context and used in stillsuit enum resolvers and the `(stillsuit/datomic-enum)` function."
  [config schema]
  (reduce-kv (fn [acc enum-name {:keys [values]}]
               (let [coords (->> (for [{:keys [:enum-value :stillsuit/datomic-value]} values]
                                   [[enum-name :stillsuit/datomic-to-lacinia datomic-value enum-value]
                                    [enum-name :stillsuit/lacinia-to-datomic enum-value datomic-value]])
                                 (apply concat))]
                 ;; coords is a list of coordinates into the accumulator map, now create the maps themselves
                 (reduce (fn [accum coord]
                           (assoc-in accum (butlast coord) (last coord)))
                         acc
                         coords)))
             {}
             (:enums schema)))
