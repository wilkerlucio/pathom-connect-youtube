(ns com.wsscode.pathom.connect.youtube
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.activities :as yt.activities]
            [com.wsscode.pathom.connect.youtube.channels :as yt.channels]
            [com.wsscode.pathom.connect.youtube.playlists :as yt.playlists]
            [com.wsscode.pathom.connect.youtube.search :as yt.search]
            [com.wsscode.pathom.connect.youtube.videos :as yt.videos]))

(defn youtube-plugin []
  {::pc/register [yt.activities/resolvers
                  yt.channels/resolvers
                  yt.playlists/resolvers
                  yt.search/resolvers
                  yt.videos/resolvers]})

