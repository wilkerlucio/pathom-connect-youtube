(ns com.wsscode.pathom.connect.youtube.helpers
  (:require [clojure.string :as str]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.diplomat.http :as http]
            [goog.string :as gstr]))

(defn output->blank-entity
  "Creates an empty entity from some output description.

  Examples:

  ```
  (output->blank-entity [:a :b]) ; => {:a nil :b nil}

  ; understands nesting
  (output->blank-entity [:a {:b [:c]}]) ; => {:a nil :b {:c nil}}
  ```"
  [output]
  (into {}
        (map (fn [x]
               (if (map? x)
                 [(first (keys x)) (output->blank-entity (first (vals x)))]
                 [x nil])))
        output))

(defn query-string [params]
  (str/join "&" (map (fn [[k v]] (str (name k) "=" v)) params)))

(defn youtube-query-part [prefix keyword]
  (let [ns (namespace keyword)]
    (if (str/starts-with? ns prefix)
      (-> ns (str/split ".") (->> (drop 2)) first))))

(defn request-parts [env prefix]
  (let [query (or (::parts-query env) (::p/parent-query env))
        parts (->> (pc/project-query-attributes env query)
                   (into #{} (comp (keep (partial youtube-query-part prefix))
                                   (map gstr/toCamelCase))))]
    (if (seq parts)
      (str/join "," parts)
      "id")))

(defn youtube-params [m]
  (into {} (keep (fn [[k v]]
                   (if (str/starts-with? (str k) ":youtube.param/")
                     [(gstr/toCamelCase (name k)) v]))) m))

(defn youtube-api [{:com.wsscode.pathom.connect.youtube/keys [access-token] :as env} path data]
  (if access-token
    (let [data (merge {:key access-token}
                 (youtube-params (-> env :ast :params))
                 data)]
      (go-catch
        (-> (http/request env
              (str "https://www.googleapis.com/youtube/v3/" path "?" (query-string data))
              {::http/accept ::http/json}) <?
            ::http/body)))
    (do
      (js/console.error "Missing youtube token")
      (throw (ex-info "Youtube token not available" {})))))

(defn youtube->output [prefix resource-schema]
  (reduce-kv
    (fn [out k v]
      (cond
        (map? v)
        (into out (youtube->output (str prefix "." (gstr/toSelectorCase (name k))) v))

        (or (string? v)
            (and (vector? v) (string? (first v))))
        (conj out (keyword prefix (gstr/toSelectorCase (name k))))

        (and (vector? v) (map? (first v)))
        (conj out {(keyword prefix (gstr/toSelectorCase (name k)))
                   (youtube->output (str prefix "." (gstr/toSelectorCase (name k)))
                     (first v))})

        :else
        (do
          (println "CANT HANDLE" (pr-str [k v]))
          out)))
    []
    resource-schema))

(defn adapt-recursive
  "Pull some key, updating the namespaces of it"
  [x ns]
  (reduce-kv
    (fn [out k v]
      (let [k' (keyword ns (gstr/toSelectorCase (name k)))]
        (cond
          (map? v)
          (into out (adapt-recursive v (str ns "." (gstr/toSelectorCase (name k)))))

          :else
          (assoc out k' v))))
    {}
    x))
