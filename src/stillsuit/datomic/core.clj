(ns stillsuit.datomic.core)

(defn entity? [thing]
  (instance? datomic.Entity thing))

(defn datomic-db? [thing]
  (instance? datomic.db.Db thing))

