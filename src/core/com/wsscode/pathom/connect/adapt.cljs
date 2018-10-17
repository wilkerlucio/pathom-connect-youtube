(ns com.wsscode.pathom.connect.adapt
  (:require [clojure.set :as set]))

(defn set-ns
  "Set the namespace of a keyword"
  [ns kw]
  (keyword ns (name kw)))

(defn set-ns-seq
  "Set the namespace for all keywords in a collection. The collection kind will
  be preserved."
  [ns s]
  (into (empty s) (map #(set-ns ns %)) s))

(defn set-ns-x
  "Set the namespace of a value. If sequence will use set-ns-seq."
  [ns x]
  (if (coll? x)
    (set-ns-seq ns x)
    (set-ns ns x)))

(defn namespaced-keys
  "Set the namespace of all map keys (non recursive)."
  [e ns]
  (into {} (map (fn [[k v]] [(set-ns ns k) v]) ) e))

(defn pull-key
  "Pull some key"
  [x key]
  (-> (dissoc x key)
      (merge (get x key))))

(defn pull-namespaced
  "Pull some key, updating the namespaces of it"
  [x key ns]
  (-> (dissoc x key)
      (merge (namespaced-keys (get x key) ns))))

(defn children-props [env]
  (->> env :ast :children (map :dispatch-key) set))

(defn match-props [env entity keys]
  (let [needed-props (set/difference (children-props env)
                       (-> entity keys set))]
    (set/intersection keys needed-props)))
