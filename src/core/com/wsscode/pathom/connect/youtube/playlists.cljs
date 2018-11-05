(ns com.wsscode.pathom.connect.youtube.playlists
  (:require [clojure.string :as str]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.helpers :as yth]))

(def playlist-output
  [:youtube.playlist/kind
   :youtube.playlist/etag
   :youtube.playlist/id
   :youtube.playlist.snippet/description
   :youtube.playlist.snippet/tags
   :youtube.playlist.snippet/published-at
   :youtube.playlist.snippet/channel-id
   :youtube.playlist.snippet.thumbnails.key/url
   :youtube.playlist.snippet.thumbnails.key/width
   :youtube.playlist.snippet.thumbnails.key/height
   :youtube.playlist.snippet/title
   :youtube.playlist.snippet.localized/title
   :youtube.playlist.snippet.localized/description
   :youtube.playlist.snippet/channel-title
   :youtube.playlist.snippet/default-language
   :youtube.playlist.status/privacy-status
   :youtube.playlist.content-details/item-count
   :youtube.playlist.player/embed-html
   :youtube.playlist.localizations.key/title
   :youtube.playlist.localizations.key/description])

(def playlist-item-output
  [:youtube.playlist-item/kind
   :youtube.playlist-item/etag
   :youtube.playlist-item/id
   :youtube.playlist-item.snippet/description
   :youtube.playlist-item.snippet/published-at
   :youtube.playlist-item.snippet/channel-id
   :youtube.playlist-item.snippet.thumbnails.key/url
   :youtube.playlist-item.snippet.thumbnails.key/width
   :youtube.playlist-item.snippet.thumbnails.key/height
   :youtube.playlist-item.snippet/title
   :youtube.playlist-item.snippet.resource-id/kind
   :youtube.playlist-item.snippet.resource-id/video-id
   :youtube.playlist-item.snippet/position
   :youtube.playlist-item.snippet/channel-title
   :youtube.playlist-item.snippet/playlist-id
   :youtube.playlist-item.content-details/video-id
   :youtube.playlist-item.content-details/start-at
   :youtube.playlist-item.content-details/end-at
   :youtube.playlist-item.content-details/note
   :youtube.playlist-item.content-details/video-published-at
   :youtube.playlist-item.status/privacy-status])

(defn adapt-playlist [playlist]
  (yth/adapt-recursive playlist "youtube.playlist"))

(defn adapt-playlist-item [playlist-item]
  (yth/adapt-recursive playlist-item "youtube.playlist-item"))

(pc/defresolver playlist-by-id [_ _]
  {::pc/input   #{:youtube.playlist/id}
   ::pc/output  playlist-output
   ::pc/batch?  true
   ::pc/resolve (pc/batch-resolver
                  (fn [env ids]
                    (go-catch
                      (->> (yth/youtube-api env "playlists"
                             {:id   (str/join "," (map :youtube.playlist/id ids))
                              :part (yth/request-parts env "youtube.playlist")}) <?
                           :items
                           (mapv adapt-playlist)
                           (pc/batch-restore-sort {::pc/inputs ids
                                                   ::pc/key    :youtube.playlist/id})))))})

(pc/defresolver playlists-by-channel [env {:keys [youtube.channel/id]}]
  {::pc/input  #{:youtube.channel/id}
   ::pc/output [{:youtube.channel/playlists playlist-output}]}
  (go-catch
    (->> (yth/youtube-api env "playlists"
           {:channelId id
            :part      (yth/request-parts
                         (assoc env ::yth/parts-query (-> env :ast :query)
                                    ::p/entity (yth/output->blank-entity playlist-output))
                         "youtube.playlist")}) <?
         :items
         (mapv adapt-playlist)
         (hash-map :youtube.channel/playlists))))

(pc/defresolver playlist-items-by-id [env {:keys [youtube.playlist/id]}]
  {::pc/input  #{:youtube.playlist/id}
   ::pc/output [{:youtube.playlist/items playlist-item-output}]}
  (go-catch
    (->> (yth/youtube-api env "playlistItems"
           {:playlistId id
            :part       (yth/request-parts
                          (assoc env ::yth/parts-query (-> env :ast :query)
                                     ::p/entity (yth/output->blank-entity playlist-item-output))
                          "youtube.playlist-item")}) <?
         :items
         (mapv adapt-playlist-item)
         (hash-map :youtube.playlist/items))))

(defn resolver-alias [from to]
  (pc/resolver (symbol (munge (str from "-" to)))
    {::pc/input #{from} ::pc/output [to]}
    (fn [_ input] {to (get input from)})))

(def resolvers
  [playlist-by-id playlists-by-channel playlist-items-by-id
   (resolver-alias :youtube.playlist-item.content-details/video-id :youtube.video/id)])

(comment
  (yth/youtube->output "youtube.playlist-item" playlist-item-schema))

(def playlist-schema
  {:kind   "youtube#playlist",
   :etag   "etag",
   :id     "string",
   :snippet
           {:description     "string",
            :tags            ["string"],
            :publishedAt     "datetime",
            :channelId       "string",
            :thumbnails
                             {:key
                              {:url    "string",
                               :width  "unsigned integer",
                               :height "unsigned integer"}},
            :title           "string",
            :localized
                             {:title "string", :description "string"},
            :channelTitle    "string",
            :defaultLanguage "string"},
   :status {:privacyStatus "string"},
   :contentDetails
           {:itemCount "unsigned integer"},
   :player {:embedHtml "string"},
   :localizations
           {:key
            {:title "string", :description "string"}}})

(def playlist-item-schema
  {:kind   "youtube#playlistItem",
   :etag   "etag",
   :id     "string",
   :snippet
           {:description  "string",
            :publishedAt  "datetime",
            :channelId    "string",
            :thumbnails
                          {:key
                           {:url    "string",
                            :width  "unsigned integer",
                            :height "unsigned integer"}},
            :title        "string",
            :resourceId
                          {:kind "string", :videoId "string"},
            :position     "unsigned integer",
            :channelTitle "string",
            :playlistId   "string"},
   :contentDetails
           {:videoId          "string",
            :startAt          "string",
            :endAt            "string",
            :note             "string",
            :videoPublishedAt "datetime"},
   :status {:privacyStatus "string"}})
