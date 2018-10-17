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
      "snippet")))

(defn youtube-api [{::keys [access-token] :as env} path data]
  (let [data (assoc data :access_token access-token
                         :part (request-parts env))]
    (go-catch
      (-> (http/request env
            (str "https://www.googleapis.com/youtube/v3/" path "?" (query-string data))
            {::http/accept ::http/json}) <?
          ::http/body))))

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

(defn youtube-plugin []
  {::pc/resolvers [channel-by-id channel-by-username]})
