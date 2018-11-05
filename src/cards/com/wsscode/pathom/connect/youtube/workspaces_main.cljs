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

(def entity-autocomplete
  (pc/resolver `entity-autocomplete
    {::pc/output [{:entity/autocomplete
                   [:autocomplete/group-key
                    :autocomplete/group-entity-type
                    {:autocomplete/items {:entity.type/company [:company/id :company/name :company/website :company/description]
                                          :entity.type/contact [:company/id :contact/name :contact/email :contact/description]}}]}]}
    (fn [{:keys [ast] :as env} _]
      {::p/env              (assoc env ::p/union-path :entity/type)
       :entity/autocomplete {:autocomplete/group-entity-type :entity.type/company
                             :autocomplete/group-key         :atlas-crm.domain.autocomplete.group-key/search-results

                             :autocomplete/items             {:company/id   1
                                                              :company/name "Avisi"
                                                              :entity/type  :entity.type/company}}})))

(def my-videos
  (pc/resolver `my-videos
    {::pc/output [{:my-videos [:youtube.video/id]}]}
    (fn [_ _]
      {:my-videos [{:youtube.video/id "r3zywlNflJI"}
                   {:youtube.video/id "q_tfHUvxJXs"}
                   {:youtube.video/id "DAyKeqm2_uc"}]})))

(def parser
  (p/parallel-parser
    {::p/env     (fn [env]
                   (merge
                     {::p/reader               [p/map-reader pc/parallel-reader pc/ident-reader p/env-placeholder-reader]
                      ::p/placeholder-prefixes #{">"}
                      ::youtube/access-token   (ls/get :youtube/token)
                      ::p.http/driver          p.http.fetch/request-async}
                     env))
     ::p/mutate  pc/mutate-async
     ::p/plugins [p/error-handler-plugin
                  p/request-cache-plugin
                  p/trace-plugin
                  (pc/connect-plugin {::pc/resolvers [entity-autocomplete
                                                      meus-videos
                                                      title-bla
                                                      duration-bla]})
                  (youtube/youtube-plugin)]}))

(ws/defcard simple-parser-demo
  (pvw/pathom-card {::pvw/parser parser}))

(ws/mount)
