(ns com.wsscode.pathom.connect.youtube.videos
  (:require [clojure.string :as str]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.helpers :as yth]))

(def video-output
  [:youtube.video.localizations.key/title
   :youtube.video.localizations.key/description
   :youtube.video.snippet/description
   :youtube.video.snippet/tags
   :youtube.video.snippet/published-at
   :youtube.video.snippet/channel-id
   :youtube.video.snippet/category-id
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
  (yth/adapt-recursive video "youtube.video"))

(def video-by-id
  (pc/resolver `video-by-id
    {::pc/input  #{:youtube.video/id}
     ::pc/output video-output
     ::pc/batch? true}
    (pc/batch-resolver
      (fn [env ids]
        (go-catch
          (->> (yth/youtube-api env "videos"
                 {:id   (str/join "," (map :youtube.video/id ids))
                  :part (yth/request-parts env "youtube.video")}) <?
               :items
               (mapv adapt-video)
               (pc/batch-restore-sort {::pc/inputs ids
                                       ::pc/key    :youtube.video/id})))))))

(def resolvers [video-by-id])

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
