(ns com.wsscode.pathom.connect.youtube
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.youtube.activities :as yt.activities]
            [com.wsscode.pathom.connect.youtube.channels :as yt.channels]
            [com.wsscode.pathom.connect.youtube.search :as yt.search]
            [com.wsscode.pathom.connect.youtube.videos :as yt.videos]))

(defn youtube-plugin []
  {::pc/resolvers [yt.activities/resolvers
                   yt.channels/resolvers
                   yt.search/resolvers
                   yt.videos/resolvers]})
