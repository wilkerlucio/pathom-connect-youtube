(ns com.wsscode.pathom.connect.youtube.activities
  (:require [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.helpers :as yth]
            [goog.string :as gstr]))

(def activity-output-base
  [:youtube.activity/kind
   :youtube.activity/etag
   :youtube.activity/id
   :youtube.activity.snippet/published-at
   :youtube.activity.snippet/channel-id
   :youtube.activity.snippet/title
   :youtube.activity.snippet/description
   {:youtube.activity.snippet.thumbnails/default [:youtube.thumbnail/url
                                                  :youtube.thumbnail/width
                                                  :youtube.thumbnail/height]}
   {:youtube.activity.snippet.thumbnails/medium [:youtube.thumbnail/url
                                                 :youtube.thumbnail/width
                                                 :youtube.thumbnail/height]}
   {:youtube.activity.snippet.thumbnails/high [:youtube.thumbnail/url
                                               :youtube.thumbnail/width
                                               :youtube.thumbnail/height]}
   {:youtube.activity.snippet.thumbnails/standard [:youtube.thumbnail/url
                                                   :youtube.thumbnail/width
                                                   :youtube.thumbnail/height]}
   {:youtube.activity.snippet.thumbnails/maxres [:youtube.thumbnail/url
                                                 :youtube.thumbnail/width
                                                 :youtube.thumbnail/height]}
   :youtube.activity.snippet/channel-title
   :youtube.activity.snippet/type
   :youtube.activity.snippet/group-id])

(def activity-output
  {:youtube.like/id           (into activity-output-base
                                    [:youtube.activity.content-details.like.resource-id/kind
                                     :youtube.activity.content-details.like.resource-id/video-id])
   :youtube.social/id         (into activity-output-base
                                    [:youtube.activity.content-details.social/type
                                     :youtube.activity.content-details.social.resource-id/kind
                                     :youtube.activity.content-details.social.resource-id/video-id
                                     :youtube.activity.content-details.social.resource-id/channel-id
                                     :youtube.activity.content-details.social.resource-id/playlist-id
                                     :youtube.activity.content-details.social/author
                                     :youtube.activity.content-details.social/reference-url
                                     :youtube.activity.content-details.social/image-url])
   :youtube.recommendation/id (into activity-output-base
                                    [:youtube.activity.content-details.recommendation.resource-id/kind
                                     :youtube.activity.content-details.recommendation.resource-id/video-id
                                     :youtube.activity.content-details.recommendation.resource-id/channel-id
                                     :youtube.activity.content-details.recommendation/reason
                                     :youtube.activity.content-details.recommendation.seed-resource-id/kind
                                     :youtube.activity.content-details.recommendation.seed-resource-id/video-id
                                     :youtube.activity.content-details.recommendation.seed-resource-id/channel-id
                                     :youtube.activity.content-details.recommendation.seed-resource-id/playlist-id])
   :youtube.playlist-item/id  (into activity-output-base
                                    [:youtube.activity.content-details.playlist-item.resource-id/kind
                                     :youtube.activity.content-details.playlist-item.resource-id/video-id
                                     :youtube.playlist/id
                                     :youtube.playlist-item/id])
   :youtube.bulletin/id       (into activity-output-base
                                    [:youtube.activity.content-details.bulletin.resource-id/kind
                                     :youtube.activity.content-details.bulletin.resource-id/video-id
                                     :youtube.activity.content-details.bulletin.resource-id/channel-id
                                     :youtube.activity.content-details.bulletin.resource-id/playlist-id])
   :youtube.comment/id        (into activity-output-base
                                    [:youtube.activity.content-details.comment.resource-id/kind
                                     :youtube.activity.content-details.comment.resource-id/video-id
                                     :youtube.activity.content-details.comment.resource-id/channel-id])
   :youtube.favorite/id       (into activity-output-base
                                    [:youtube.activity.content-details.favorite.resource-id/kind
                                     :youtube.activity.content-details.favorite.resource-id/video-id])
   :youtube.subscription/id   (into activity-output-base
                                    [:youtube.activity.content-details.subscription.resource-id/kind
                                     :youtube.activity.content-details.subscription.resource-id/channel-id])
   :youtube.upload/id         (into activity-output-base
                                    [:youtube.activity.content-details.upload/video-id])})

(defn adapt-activity [activity]
  (let [activity' (yth/adapt-recursive activity "youtube.activity")
        kind      (:youtube.activity.snippet/type activity')
        id-key    (keyword (str "youtube." (gstr/toSelectorCase kind)) "id")]
    (-> (yth/adapt-recursive activity "youtube.activity")
        (assoc id-key (:youtube.activity/id activity')))))

(def channel-activity
  (pc/resolver `channel-activity
    {::pc/input  #{:youtube.channel/id}
     ::pc/params [:youtube.page/max-results]
     ::pc/output [{:youtube.channel/activity activity-output}]}
    (fn [env {:keys [youtube.channel/id]}]
      (let [max-results (get-in env [:ast :params :youtube.page/max-results])]
        (go-catch
          {:youtube.channel/activity
           (->> (yth/youtube-api env "activities"
                  (cond->
                    {:channelId id
                     :part      "snippet,contentDetails"}
                    max-results (assoc :maxResults max-results))) <?
                :items
                (mapv adapt-activity))})))))

(def resolvers [channel-activity])

(def activity-schema
  {:kind "youtube#activity",
   :etag "etag",
   :id   "string",
   :snippet
         {:publishedAt  "datetime",
          :channelId    "string",
          :title        "string",
          :description  "string",
          :thumbnails
                        {:KEY
                         {:url    "string",
                          :width  "unsigned integer",
                          :height "unsigned integer"}},
          :channelTitle "string",
          :type         "string",
          :groupId      "string"},
   :contentDetails
         {:like
                       {:resourceId
                        {:kind "string", :videoId "string"}},
          :social
                       {:type         "string",
                        :resourceId
                                      {:kind       "string",
                                       :videoId    "string",
                                       :channelId  "string",
                                       :playlistId "string"},
                        :author       "string",
                        :referenceUrl "string",
                        :imageUrl     "string"},
          :recommendation
                       {:resourceId
                                {:kind      "string",
                                 :videoId   "string",
                                 :channelId "string"},
                        :reason "string",
                        :seedResourceId
                                {:kind       "string",
                                 :videoId    "string",
                                 :channelId  "string",
                                 :playlistId "string"}},
          :playlistItem
                       {:resourceId
                                        {:kind "string", :videoId "string"},
                        :playlistId     "string",
                        :playlistItemId "string"},
          :bulletin
                       {:resourceId
                        {:kind       "string",
                         :videoId    "string",
                         :channelId  "string",
                         :playlistId "string"}},
          :comment
                       {:resourceId
                        {:kind      "string",
                         :videoId   "string",
                         :channelId "string"}},
          :channelItem {:resourceId {}},
          :favorite
                       {:resourceId
                        {:kind "string", :videoId "string"}},
          :subscription
                       {:resourceId
                        {:kind "string", :channelId "string"}},
          :upload      {:videoId "string"}}})
