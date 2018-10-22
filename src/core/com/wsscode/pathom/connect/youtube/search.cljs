(ns com.wsscode.pathom.connect.youtube.search
  (:require [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.helpers :as yth]
            [com.wsscode.pathom.connect.youtube.videos :as yt.video]))

(def search-video-output
  [:youtube.video/id
   :youtube.video.snippet/published-at
   :youtube.video.snippet/channel-id
   :youtube.video.snippet/title
   :youtube.video.snippet/description
   {:youtube.video.snippet.thumbnails/default [:youtube.thumbnail/url
                                               :youtube.thumbnail/width
                                               :youtube.thumbnail/height]}
   {:youtube.video.snippet.thumbnails/medium [:youtube.thumbnail/url
                                              :youtube.thumbnail/width
                                              :youtube.thumbnail/height]}
   {:youtube.video.snippet.thumbnails/high [:youtube.thumbnail/url
                                            :youtube.thumbnail/width
                                            :youtube.thumbnail/height]}
   {:youtube.video.snippet.thumbnails/standard [:youtube.thumbnail/url
                                                :youtube.thumbnail/width
                                                :youtube.thumbnail/height]}
   {:youtube.video.snippet.thumbnails/maxres [:youtube.thumbnail/url
                                              :youtube.thumbnail/width
                                              :youtube.thumbnail/height]}
   :youtube.video.snippet/channel-title
   :youtube.video.snippet/live-broadcast-content])

(defn adapt-search-video [video]
  (-> (update video :id :videoId)
      yt.video/adapt-video))

(def channel-latest-videos
  (pc/resolver `channel-latest-videos
    {::pc/input  #{:youtube.channel/id}
     ::pc/output [{:youtube.channel/latest-videos search-video-output}]}
    (fn [env {:keys [youtube.channel/id]}]
      (go-catch
        {:youtube.channel/latest-videos
         (->> (yth/youtube-api env "search"
                {:channelId id
                 :order     "date"
                 :type      "video"
                 :part      "snippet"}) <?
              :items
              (mapv adapt-search-video))}))))

(def resolvers [channel-latest-videos])

(def search-result-schema
  {:kind "youtube#searchResult",
   :etag "etag",
   :id
         {:kind       "string",
          :videoId    "string",
          :channelId  "string",
          :playlistId "string"},
   :snippet
         {:publishedAt          "datetime",
          :channelId            "string",
          :title                "string",
          :description          "string",
          :thumbnails
                                {:key
                                 {:url    "string",
                                  :width  "unsigned integer",
                                  :height "unsigned integer"}},
          :channelTitle         "string",
          :liveBroadcastContent "string"}})
