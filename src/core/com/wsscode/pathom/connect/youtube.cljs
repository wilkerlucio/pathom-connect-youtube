(ns com.wsscode.pathom.connect.youtube
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.diplomat.http :as http]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [goog.string :as gstr]
            [com.wsscode.pathom.connect.adapt :as adapt]
            [clojure.string :as str]))

(defn query-string [params]
  (str/join "&" (map (fn [[k v]] (str (name k) "=" v)) params)))

(defn youtube-query-part [keyword]
  (-> keyword namespace (str/split ".") (->> (drop 2)) first))

(defn request-parts [env]
  (let [parts
        (->> env ::p/parent-query p/query->ast :children
             (into #{} (comp (keep (comp youtube-query-part :key))
                             (map gstr/toCamelCase))))]
    (if (seq parts)
      (str/join "," parts)
      "id")))

(defn youtube-api [{::keys [access-token] :as env} path data]
  (if access-token
    (let [data (merge {:part (request-parts env)} data)]
      (go-catch
        (-> (http/request env
              (str "https://www.googleapis.com/youtube/v3/" path "?" (query-string data))
              {::http/accept  ::http/json
               ::http/headers {"Authorization" (str "Bearer " access-token)}}) <?
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

(defn batch-restore-sort [{::keys [input key default]} items]
  (let [index   (group-by key items)
        default (or default #(hash-map key (get % key)))]
    (into [] (map (fn [input]
                    (or (first (get index (get input key)))
                        (default input)))) input)))

(defn adapt-thumbnail [thumbnail]
  (adapt/namespaced-keys thumbnail "youtube.thumbnail"))

(defn adapt-channel [channel]
  (adapt-recursive channel "youtube.channel"))

(def channel-by-id
  (pc/resolver `channel-by-id
    {::pc/input  #{:youtube.channel/id}
     ::pc/output channel-output
     ::pc/batch? true}
    (pc/batch-resolver
      (fn [env ids]
        (go-catch
          (->> (youtube-api env "channels"
                 {:id (str/join "," (map :youtube.channel/id ids))}) <?
               :items
               (mapv adapt-channel)
               (batch-restore-sort {::input ids
                                    ::key   :youtube.channel/id})))))))

(def channel-by-username
  (pc/resolver `channel-by-username
    {::pc/input  #{:youtube.user/username}
     ::pc/output channel-output}
    (fn [env {:keys [youtube.user/username]}]
      (go-catch
        (-> (youtube-api env "channels"
              {:forUsername username}) <?
            :items
            first
            adapt-channel)))))

(def video-output
  [:youtube.video.localizations.key/title
   :youtube.video.localizations.key/description
   :youtube.video.snippet/description
   :youtube.video.snippet/tags
   :youtube.video.snippet/published-at
   :youtube.video.snippet/channel-id
   :youtube.video.snippet/category-id
   :youtube.video.snippet.thumbnails.-k-e-y/url
   :youtube.video.snippet.thumbnails.-k-e-y/width
   :youtube.video.snippet.thumbnails.-k-e-y/height
   :youtube.video.snippet/title
   :youtube.video.snippet/default-audio-language
   :youtube.video.snippet/live-broadcast-content
   :youtube.video.snippet.localized/title
   :youtube.video.snippet.localized/description
   :youtube.video.snippet/channel-title
   :youtube.video.snippet/default-language
   :youtube.video.file-details/creation-time
   :youtube.video.file-details/file-size
   :youtube.video.file-details/file-type
   :youtube.video.file-details/file-name
   :youtube.video.file-details/bitrate-bps
   {:youtube.video.file-details/video-streams [:youtube.video.file-details.video-streams/width-pixels
                                               :youtube.video.file-details.video-streams/height-pixels
                                               :youtube.video.file-details.video-streams/frame-rate-fps
                                               :youtube.video.file-details.video-streams/aspect-ratio
                                               :youtube.video.file-details.video-streams/codec
                                               :youtube.video.file-details.video-streams/bitrate-bps
                                               :youtube.video.file-details.video-streams/rotation
                                               :youtube.video.file-details.video-streams/vendor]}
   :youtube.video.file-details/container
   :youtube.video.file-details/duration-ms
   {:youtube.video.file-details/audio-streams [:youtube.video.file-details.audio-streams/channel-count
                                               :youtube.video.file-details.audio-streams/codec
                                               :youtube.video.file-details.audio-streams/bitrate-bps
                                               :youtube.video.file-details.audio-streams/vendor]}
   :youtube.video/etag
   :youtube.video.recording-details/recording-date
   :youtube.video.statistics/view-count
   :youtube.video.statistics/like-count
   :youtube.video.statistics/dislike-count
   :youtube.video.statistics/favorite-count
   :youtube.video.statistics/comment-count
   :youtube.video.status/upload-status
   :youtube.video.status/failure-reason
   :youtube.video.status/rejection-reason
   :youtube.video.status/privacy-status
   :youtube.video.status/publish-at
   :youtube.video.status/license
   :youtube.video.status/embeddable
   :youtube.video.status/public-stats-viewable
   :youtube.video.processing-details/processing-status
   :youtube.video.processing-details.processing-progress/parts-total
   :youtube.video.processing-details.processing-progress/parts-processed
   :youtube.video.processing-details.processing-progress/time-left-ms
   :youtube.video.processing-details/processing-failure-reason
   :youtube.video.processing-details/file-details-availability
   :youtube.video.processing-details/processing-issues-availability
   :youtube.video.processing-details/tag-suggestions-availability
   :youtube.video.processing-details/editor-suggestions-availability
   :youtube.video.processing-details/thumbnails-availability
   :youtube.video/id
   :youtube.video.live-streaming-details/actual-start-time
   :youtube.video.live-streaming-details/actual-end-time
   :youtube.video.live-streaming-details/scheduled-start-time
   :youtube.video.live-streaming-details/scheduled-end-time
   :youtube.video.live-streaming-details/concurrent-viewers
   :youtube.video.live-streaming-details/active-live-chat-id
   :youtube.video.suggestions/processing-errors
   :youtube.video.suggestions/processing-warnings
   :youtube.video.suggestions/processing-hints
   {:youtube.video.suggestions/tag-suggestions [:youtube.video.suggestions.tag-suggestions/tag
                                                :youtube.video.suggestions.tag-suggestions/category-restricts]}
   :youtube.video.suggestions/editor-suggestions
   :youtube.video.content-details/caption
   :youtube.video.content-details/definition
   :youtube.video.content-details/licensed-content
   :youtube.video.content-details/duration
   :youtube.video.content-details.content-rating/mccyp-rating
   :youtube.video.content-details.content-rating/ilfilm-rating
   :youtube.video.content-details.content-rating/lsf-rating
   :youtube.video.content-details.content-rating/mccaa-rating
   :youtube.video.content-details.content-rating/fsk-rating
   :youtube.video.content-details.content-rating/mtrcb-rating
   :youtube.video.content-details.content-rating/grfilm-rating
   :youtube.video.content-details.content-rating/kijkwijzer-rating
   :youtube.video.content-details.content-rating/czfilm-rating
   :youtube.video.content-details.content-rating/incaa-rating
   :youtube.video.content-details.content-rating/resorteviolencia-rating
   :youtube.video.content-details.content-rating/eefilm-rating
   :youtube.video.content-details.content-rating/cna-rating
   :youtube.video.content-details.content-rating/moctw-rating
   :youtube.video.content-details.content-rating/tvpg-rating
   :youtube.video.content-details.content-rating/bfvc-rating
   :youtube.video.content-details.content-rating/bmukk-rating
   :youtube.video.content-details.content-rating/cce-rating
   :youtube.video.content-details.content-rating/cnc-rating
   :youtube.video.content-details.content-rating/fco-rating
   :youtube.video.content-details.content-rating/ifco-rating
   :youtube.video.content-details.content-rating/oflc-rating
   :youtube.video.content-details.content-rating/chvrs-rating
   :youtube.video.content-details.content-rating/cbfc-rating
   :youtube.video.content-details.content-rating/csa-rating
   :youtube.video.content-details.content-rating/icaa-rating
   :youtube.video.content-details.content-rating/djctq-rating-reasons
   :youtube.video.content-details.content-rating/fcbm-rating
   :youtube.video.content-details.content-rating/pefilm-rating
   :youtube.video.content-details.content-rating/yt-rating
   :youtube.video.content-details.content-rating/nfvcb-rating
   :youtube.video.content-details.content-rating/cscf-rating
   :youtube.video.content-details.content-rating/djctq-rating
   :youtube.video.content-details.content-rating/kmrb-rating
   :youtube.video.content-details.content-rating/smais-rating
   :youtube.video.content-details.content-rating/bbfc-rating
   :youtube.video.content-details.content-rating/skfilm-rating
   :youtube.video.content-details.content-rating/nbc-rating
   :youtube.video.content-details.content-rating/eirin-rating
   :youtube.video.content-details.content-rating/nkclv-rating
   :youtube.video.content-details.content-rating/moc-rating
   :youtube.video.content-details.content-rating/smsa-rating
   :youtube.video.content-details.content-rating/medietilsynet-rating
   :youtube.video.content-details.content-rating/mpaa-rating
   :youtube.video.content-details.content-rating/meku-rating
   :youtube.video.content-details.content-rating/ecbmct-rating
   :youtube.video.content-details.content-rating/rte-rating
   :youtube.video.content-details.content-rating/mibac-rating
   :youtube.video.content-details.content-rating/mda-rating
   :youtube.video.content-details.content-rating/chfilm-rating
   :youtube.video.content-details.content-rating/rcnof-rating
   :youtube.video.content-details.content-rating/egfilm-rating
   :youtube.video.content-details.content-rating/anatel-rating
   :youtube.video.content-details.content-rating/catvfr-rating
   :youtube.video.content-details.content-rating/acb-rating
   :youtube.video.content-details.content-rating/rtc-rating
   :youtube.video.content-details.content-rating/russia-rating
   :youtube.video.content-details.content-rating/ccc-rating
   :youtube.video.content-details.content-rating/agcom-rating
   :youtube.video.content-details.content-rating/catv-rating
   :youtube.video.content-details.content-rating/kfcb-rating
   :youtube.video.content-details.content-rating/cicf-rating
   :youtube.video.content-details.content-rating/mcst-rating
   :youtube.video.content-details.content-rating/nbcpl-rating
   :youtube.video.content-details.content-rating/nfrc-rating
   :youtube.video.content-details.content-rating/fpb-rating
   :youtube.video.content-details.content-rating/fpb-rating-reasons
   :youtube.video.content-details.content-rating/fmoc-rating
   :youtube.video.content-details.content-rating/mpaat-rating
   :youtube.video.content-details.region-restriction/allowed
   :youtube.video.content-details.region-restriction/blocked
   :youtube.video.content-details/dimension
   :youtube.video.content-details/projection
   :youtube.video.content-details/has-custom-thumbnail
   :youtube.video.player/embed-html
   :youtube.video.player/embed-height
   :youtube.video.player/embed-width
   :youtube.video.topic-details/topic-ids
   :youtube.video.topic-details/relevant-topic-ids
   :youtube.video.topic-details/topic-categories])

(defn adapt-video [video]
  (adapt-recursive video "youtube.video"))

(def search-video-output
  [:youtube.video/id
   :youtube.video.snippet/published-at
   :youtube.video.snippet/channel-id
   :youtube.video.snippet/title
   :youtube.video.snippet/description
   ;:youtube.video.snippet.thumbnails.key/url
   ;:youtube.video.snippet.thumbnails.key/width
   ;:youtube.video.snippet.thumbnails.key/height
   :youtube.video.snippet/channel-title
   :youtube.video.snippet/live-broadcast-content])

(defn adapt-search-video [video]
  (-> (update video :id :videoId)
      adapt-video))

(def channel-latest-videos
  (pc/resolver `channel-latest-videos
    {::pc/input  #{:youtube.channel/id}
     ::pc/output [{:youtube.channel/latest-videos search-video-output}]}
    (fn [env {:keys [youtube.channel/id]}]
      (go-catch
        {:youtube.channel/latest-videos
         (->> (youtube-api env "search"
                {:channelId id
                 :order     "date"
                 :type      "video"
                 :part      "snippet"}) <?
              :items
              (mapv adapt-search-video))}))))

(def video-by-id
  (pc/resolver `video-by-id
    {::pc/input  #{:youtube.video/id}
     ::pc/output video-output
     ::pc/batch? true}
    (pc/batch-resolver
      (fn [env ids]
        (go-catch
          (->> (youtube-api env "videos"
                 {:id (str/join "," (map :youtube.video/id ids))}) <?
               :items
               (mapv adapt-video)
               (batch-restore-sort {::input ids
                                    ::key   :youtube.video/id})))))))

(defn youtube-plugin []
  {::pc/resolvers [channel-by-id channel-by-username channel-latest-videos
                   video-by-id]})

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

(comment
  (youtube->output "youtube.video" search-result-schema))

(def video-schema
  {:localizations
                     {:KEY
                      {:title "string", :description "string"}},
   :snippet
                     {:description          "string",
                      :tags                 ["string"],
                      :publishedAt          "datetime",
                      :channelId            "string",
                      :categoryId           "string",
                      :thumbnails
                                            {:KEY
                                             {:url    "string",
                                              :width  "unsigned integer",
                                              :height "unsigned integer"}},
                      :title                "string",
                      :defaultAudioLanguage "string",
                      :liveBroadcastContent "string",
                      :localized
                                            {:title "string", :description "string"},
                      :channelTitle         "string",
                      :defaultLanguage      "string"},
   :fileDetails
                     {:creationTime "string",
                      :fileSize     "unsigned long",
                      :fileType     "string",
                      :fileName     "string",
                      :bitrateBps   "unsigned long",
                      :videoStreams
                                    [{:widthPixels  "unsigned integer",
                                      :heightPixels "unsigned integer",
                                      :frameRateFps "double",
                                      :aspectRatio  "double",
                                      :codec        "string",
                                      :bitrateBps   "unsigned long",
                                      :rotation     "string",
                                      :vendor       "string"}],
                      :container    "string",
                      :durationMs   "unsigned long",
                      :audioStreams
                                    [{:channelCount "unsigned integer",
                                      :codec        "string",
                                      :bitrateBps   "unsigned long",
                                      :vendor       "string"}]},
   :etag             "etag",
   :recordingDetails {:recordingDate "datetime"},
   :statistics
                     {:viewCount     "unsigned long",
                      :likeCount     "unsigned long",
                      :dislikeCount  "unsigned long",
                      :favoriteCount "unsigned long",
                      :commentCount  "unsigned long"},
   :status
                     {:uploadStatus        "string",
                      :failureReason       "string",
                      :rejectionReason     "string",
                      :privacyStatus       "string",
                      :publishAt           "datetime",
                      :license             "string",
                      :embeddable          "boolean",
                      :publicStatsViewable "boolean"},
   :processingDetails
                     {:processingStatus              "string",
                      :processingProgress
                                                     {:partsTotal     "unsigned long",
                                                      :partsProcessed "unsigned long",
                                                      :timeLeftMs     "unsigned long"},
                      :processingFailureReason       "string",
                      :fileDetailsAvailability       "string",
                      :processingIssuesAvailability  "string",
                      :tagSuggestionsAvailability    "string",
                      :editorSuggestionsAvailability "string",
                      :thumbnailsAvailability        "string"},
   :id               "string",
   :liveStreamingDetails
                     {:actualStartTime    "datetime",
                      :actualEndTime      "datetime",
                      :scheduledStartTime "datetime",
                      :scheduledEndTime   "datetime",
                      :concurrentViewers  "unsigned long",
                      :activeLiveChatId   "string"},
   :suggestions
                     {:processingErrors   ["string"],
                      :processingWarnings ["string"],
                      :processingHints    ["string"],
                      :tagSuggestions
                                          [{:tag               "string",
                                            :categoryRestricts ["string"]}],
                      :editorSuggestions  ["string"]},
   :contentDetails
                     {:caption            "string",
                      :definition         "string",
                      :licensedContent    "boolean",
                      :duration           "string",
                      :contentRating
                                          {:mccypRating            "string",
                                           :ilfilmRating           "string",
                                           :lsfRating              "string",
                                           :mccaaRating            "string",
                                           :fskRating              "string",
                                           :mtrcbRating            "string",
                                           :grfilmRating           "string",
                                           :kijkwijzerRating       "string",
                                           :czfilmRating           "string",
                                           :incaaRating            "string",
                                           :resorteviolenciaRating "string",
                                           :eefilmRating           "string",
                                           :cnaRating              "string",
                                           :moctwRating            "string",
                                           :tvpgRating             "string",
                                           :bfvcRating             "string",
                                           :bmukkRating            "string",
                                           :cceRating              "string",
                                           :cncRating              "string",
                                           :fcoRating              "string",
                                           :ifcoRating             "string",
                                           :oflcRating             "string",
                                           :chvrsRating            "string",
                                           :cbfcRating             "string",
                                           :csaRating              "string",
                                           :icaaRating             "string",
                                           :djctqRatingReasons     ["string"],
                                           :fcbmRating             "string",
                                           :pefilmRating           "string",
                                           :ytRating               "string",
                                           :nfvcbRating            "string",
                                           :cscfRating             "string",
                                           :djctqRating            "string",
                                           :kmrbRating             "string",
                                           :smaisRating            "string",
                                           :bbfcRating             "string",
                                           :skfilmRating           "string",
                                           :nbcRating              "string",
                                           :eirinRating            "string",
                                           :nkclvRating            "string",
                                           :mocRating              "string",
                                           :smsaRating             "string",
                                           :medietilsynetRating    "string",
                                           :mpaaRating             "string",
                                           :mekuRating             "string",
                                           :ecbmctRating           "string",
                                           :rteRating              "string",
                                           :mibacRating            "string",
                                           :mdaRating              "string",
                                           :chfilmRating           "string",
                                           :rcnofRating            "string",
                                           :egfilmRating           "string",
                                           :anatelRating           "string",
                                           :catvfrRating           "string",
                                           :acbRating              "string",
                                           :rtcRating              "string",
                                           :russiaRating           "string",
                                           :cccRating              "string",
                                           :agcomRating            "string",
                                           :catvRating             "string",
                                           :kfcbRating             "string",
                                           :cicfRating             "string",
                                           :mcstRating             "string",
                                           :nbcplRating            "string",
                                           :nfrcRating             "string",
                                           :fpbRating              "string",
                                           :fpbRatingReasons       ["string"],
                                           :fmocRating             "string",
                                           :mpaatRating            "string"},
                      :regionRestriction
                                          {:allowed ["string"], :blocked ["string"]},
                      :dimension          "string",
                      :projection         "string",
                      :hasCustomThumbnail "boolean"},
   :player
                     {:embedHtml   "string",
                      :embedHeight "long",
                      :embedWidth  "long"},
   :topicDetails
                     {:topicIds         ["string"],
                      :relevantTopicIds ["string"],
                      :topicCategories  ["string"]}})
