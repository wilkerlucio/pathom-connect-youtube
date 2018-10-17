(ns com.wsscode.pathom.connect.youtube.workspaces-main
  (:require [cljs.core.async :as async :refer [go <!]]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.core :as ws]
            [com.wsscode.pathom.viz.workspaces :as pvw]
            [com.wsscode.pathom.connect.youtube :as youtube]
            [com.wsscode.pathom.diplomat.http :as p.http]
            [com.wsscode.pathom.diplomat.http.fetch :as p.http.fetch]
            [nubank.workspaces.lib.local-storage :as ls]))

(def indexes (atom {}))

(defmulti resolver-fn pc/resolver-dispatch)
(def defresolver (pc/resolver-factory resolver-fn indexes))

(defmulti mutation-fn pc/mutation-dispatch)
(def defmutation (pc/mutation-factory mutation-fn indexes))

(def parser
  (p/parallel-parser {::p/env          (fn [env]
                                         (merge
                                           {::p/reader             [p/map-reader pc/parallel-reader pc/ident-reader]
                                            ::pc/resolver-dispatch resolver-fn
                                            ::pc/mutate-dispatch   mutation-fn
                                            ::pc/indexes           @indexes
                                            ::youtube/access-token (ls/get :youtube/token)
                                            ::p.http/driver        p.http.fetch/request-async}
                                           env))
                      ::pc/defresolver defresolver
                      ::pc/defmutation defmutation
                      ::p/mutate       pc/mutate-async
                      ::p/plugins      [p/error-handler-plugin
                                        p/request-cache-plugin
                                        p/trace-plugin
                                        pc/connect-plugin
                                        (youtube/youtube-plugin)]}))

(ws/defcard simple-parser-demo
  (pvw/pathom-card {::pvw/parser parser}))

(ws/mount)
