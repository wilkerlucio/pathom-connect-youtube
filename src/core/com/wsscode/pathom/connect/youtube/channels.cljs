(ns com.wsscode.pathom.connect.youtube.channels
  (:require [clojure.string :as str]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.helpers :as yth]
            [goog.string :as gstr]))

(def channel-output
  [:youtube.channel.content-owner-details/content-owner
   :youtube.channel.content-owner-details/time-linked
   :youtube.channel.localizations.key/title
   :youtube.channel.localizations.key/description
   :youtube.channel.branding-settings.channel/description
   :youtube.channel.branding-settings.channel/show-browse-view
   :youtube.channel.branding-settings.channel/keywords
   :youtube.channel.branding-settings.channel/title
   :youtube.channel.branding-settings.channel/tracking-analytics-account-id
   :youtube.channel.branding-settings.channel/default-tab
   :youtube.channel.branding-settings.channel/featured-channels-title
   :youtube.channel.branding-settings.channel/profile-color
   :youtube.channel.branding-settings.channel/unsubscribed-trailer
   :youtube.channel.branding-settings.channel/moderate-comments
   :youtube.channel.branding-settings.channel/country
   :youtube.channel.branding-settings.channel/show-related-channels
   :youtube.channel.branding-settings.channel/default-language
   :youtube.channel.branding-settings.channel/featured-channels-urls
   :youtube.channel.branding-settings.watch/text-color
   :youtube.channel.branding-settings.watch/background-color
   :youtube.channel.branding-settings.watch/featured-playlist-id
   :youtube.channel.branding-settings.image/banner-tablet-extra-hd-image-url
   :youtube.channel.branding-settings.image/banner-tv-medium-image-url
   :youtube.channel.branding-settings.image/banner-mobile-medium-hd-image-url
   :youtube.channel.branding-settings.image/banner-tablet-low-image-url
   :youtube.channel.branding-settings.image/banner-mobile-extra-hd-image-url
   :youtube.channel.branding-settings.image/banner-tablet-hd-image-url
   :youtube.channel.branding-settings.image/banner-mobile-low-image-url
   :youtube.channel.branding-settings.image/banner-image-url
   :youtube.channel.branding-settings.image/banner-external-url
   :youtube.channel.branding-settings.image/banner-tv-high-image-url
   :youtube.channel.branding-settings.image/banner-mobile-image-url
   :youtube.channel.branding-settings.image/banner-mobile-hd-image-url
   :youtube.channel.branding-settings.image/banner-tablet-image-url
   :youtube.channel.branding-settings.image/banner-tv-image-url
   :youtube.channel.branding-settings.image/watch-icon-image-url
   :youtube.channel.branding-settings.image/tracking-image-url
   :youtube.channel.branding-settings.image/banner-tv-low-image-url
   {:youtube.channel.branding-settings/hints [:youtube.channel.branding-settings.hints/property
                                              :youtube.channel.branding-settings.hints/value]}
   :youtube.channel.snippet/title
   :youtube.channel.snippet/description
   :youtube.channel.snippet/custom-url
   :youtube.channel.snippet/published-at
   {:youtube.channel.snippet.thumbnails/default [:youtube.thumbnail/url
                                                 :youtube.thumbnail/width
                                                 :youtube.thumbnail/height]}
   {:youtube.channel.snippet.thumbnails/high [:youtube.thumbnail/url
                                              :youtube.thumbnail/width
                                              :youtube.thumbnail/height]}
   {:youtube.channel.snippet.thumbnails/medium [:youtube.thumbnail/url
                                                :youtube.thumbnail/width
                                                :youtube.thumbnail/height]}
   :youtube.channel.snippet/default-language
   :youtube.channel.snippet.localized/title
   :youtube.channel.snippet.localized/description
   :youtube.channel.snippet/country
   :youtube.channel/etag
   :youtube.channel.invideo-promotion.default-timing/type
   :youtube.channel.invideo-promotion.default-timing/offset-ms
   :youtube.channel.invideo-promotion.default-timing/duration-ms
   :youtube.channel.invideo-promotion.position/type
   :youtube.channel.invideo-promotion.position/corner-position
   {:youtube.channel.invideo-promotion/items [:youtube.channel.invideo-promotion.items.id/type
                                              :youtube.channel.invideo-promotion.items.id/video-id
                                              :youtube.channel.invideo-promotion.items.id/website-url
                                              :youtube.channel.invideo-promotion.items.id/recently-uploaded-by
                                              :youtube.channel.invideo-promotion.items.timing/type
                                              :youtube.channel.invideo-promotion.items.timing/offset-ms
                                              :youtube.channel.invideo-promotion.items.timing/duration-ms
                                              :youtube.channel.invideo-promotion.items/custom-message
                                              :youtube.channel.invideo-promotion.items/promoted-by-content-owner]}
   :youtube.channel.invideo-promotion/use-smart-timing
   :youtube.channel.audit-details/overall-good-standing
   :youtube.channel.audit-details/community-guidelines-good-standing
   :youtube.channel.audit-details/copyright-strikes-good-standing
   :youtube.channel.audit-details/content-id-claims-good-standing
   :youtube.channel.statistics/view-count
   :youtube.channel.statistics/comment-count
   :youtube.channel.statistics/subscriber-count
   :youtube.channel.statistics/hidden-subscriber-count
   :youtube.channel.statistics/video-count
   :youtube.channel.status/privacy-status
   :youtube.channel.status/is-linked
   :youtube.channel.status/long-uploads-status
   :youtube.channel/id
   :youtube.channel.content-details.related-playlists/likes
   :youtube.channel.content-details.related-playlists/favorites
   :youtube.channel.content-details.related-playlists/uploads
   :youtube.channel.content-details.related-playlists/watch-history
   :youtube.channel.content-details.related-playlists/watch-later
   :youtube.channel.topic-details/topic-ids
   :youtube.channel.topic-details/topic-categories])

(defn adapt-channel [channel]
  (yth/adapt-recursive channel "youtube.channel"))

(def channel-by-id
  (pc/resolver `channel-by-id
    {::pc/input  #{:youtube.channel/id}
     ::pc/output channel-output
     ::pc/batch? true}
    (pc/batch-resolver
      (fn [env ids]
        (go-catch
          (->> (yth/youtube-api env "channels"
                 {:id   (str/join "," (map :youtube.channel/id ids))
                  :part (yth/request-parts env "youtube.channel")}) <?
               :items
               (mapv adapt-channel)
               (pc/batch-restore-sort {::pc/inputs ids
                                       ::pc/key    :youtube.channel/id})))))))

(pc/defresolver channel-by-username [env {:keys [youtube.user/username]}]
  {::pc/input  #{:youtube.user/username}
   ::pc/output channel-output}
  (go-catch
    (-> (yth/youtube-api env "channels"
          {:forUsername username
           :part        (yth/request-parts env "youtube.channel")}) <?
        :items
        first
        adapt-channel)))

(def resolvers [channel-by-id channel-by-username])
