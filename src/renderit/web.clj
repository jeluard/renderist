(ns renderit.web
    (:use [ring.server.standalone])
    (:require [clj-time.format :as f]
              [clojure.java.io :as io]
              [compojure.core :as c]
              [compojure.route :as r]
              [markdown :as md]
              [net.cgrand.enlive-html :as e]
              [renderit.api :as a]
              [renderit.gist :as g]
              [renderit.plantuml :as p]
              [ring.util.codec :as codec])
    (:import java.util.Locale))

(e/deftemplate index "public/index.html" [string]
  [:div#content] (e/content string))

(def formatter (f/with-locale (f/formatter "MMM dd, yyyy") Locale/US)) ;default to UTC time zone

(comment defsnippet gist "public/templates/diagram.html" [:section]
  []
  [:#id] (set-attr :id (:filename (val file))))

(comment defsnippet footer "footer.html" [:.footer]
  [message]
  [:.footer] (content message))
;;(footer "hello") => ({:tag :div, :attrs {:class "footer"}, :content ("hello")})

(comment deftemplate friends-list "friends.html"
  [username friends]
  [:.usrname] (content username)
  [:ul.friends :li] (clone-for [f friends]
                      (do-> (content f))
                            (add-class "test"))
  [:body] (append (footer (str "Goodbye, " username))))
;;use directly as ring :body

(e/defsnippet gist-snippet "public/templates/diagram.html" 
  [:body :> e/any-node] [{:keys [id description] date :created_at {author :login} :user} readme files]
  [:span (e/nth-child 1)] (e/set-attr :href (str "/" author))
  [:span (e/nth-child 1)] (e/content author)
  [:span (e/nth-child 2)] (e/set-attr :href (str "https://gist.github.com/" id))
  [:span (e/nth-child 2)] (e/content (str "#" id))
  [:h1] (e/content description)
  [:aside#date] (e/content (f/unparse formatter (f/parse date)))
  [:pre#readme] (e/html-content (md/md-to-html-string readme))
  [:section] (e/clone-for [file files]
               [:section#id] (e/set-attr :id (:filename (val file)))
               [:h2 :a] (e/content (:filename (val file)))
               [:h2 :a] (e/set-attr :href (str "https://gist.github.com/" id "#file_" (g/file-name-to-file-id (:filename (val file)))))
               [:div.diagram :img] (e/set-attr :src (str "data:image/png;base64," (codec/base64-encode (p/render (:content (val file)) "png"))))
               [:pre.source] (e/content (:content (val file)))
               [:code] (e/content (str "&lt;img alt=\"" (:filename (val file)) "\" src=\"http://renderit.herokuapp.com/api/" id "/" (:filename (val file)) ".png"))))

(defn load-page [page]
  (slurp (io/resource (str "public/" page)) :encoding "UTF-8"))

(def page-index (load-page "index.html"))

(def page-author (load-page "author.html"))

(def page-404 (load-page "404.html"))

(defn page-gist [id]
  (if-let [gist (g/get-gist id)]
    (apply str (index (gist-snippet gist (g/extract-file gist "Readme.md") (:files gist))))
    page-404))

(c/defroutes all-routes
  (c/context "/api" [] a/routes)
  (c/GET "/" [] page-index)
  (c/GET "/:id" [id] (page-gist id))
  (r/resources "/")
  (r/not-found page-404))

(defn -main []
  (System/setProperty "java.awt.headless" "true")
  (serve all-routes {:open-browser? false}))
