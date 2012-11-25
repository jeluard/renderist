(ns renderit.web
    (:use [renderit.gist]
          [renderit.plantuml]
          [ring.server.standalone]
          [ring.util.codec :only (base64-encode)]
          [ring.util.response :only (content-type not-found response status)]
          [compojure.core]
          [net.cgrand.enlive-html])
    (:require [clj-time.format :as f]
              [clojure.java.io :as io]
              [compojure.route :as route]
              [markdown :as md]
              [renderit.api :as api])
    (:import java.util.Locale))

(deftemplate index "public/index.html" [string]
  [:div#content] (content string))

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

(defsnippet gist-snippet "public/templates/diagram.html" 
  [:body :> any-node] [{:keys [id description] date :created_at {author :login} :user} readme files]
  [:span (nth-child 1)] (set-attr :href (str "/" author))
  [:span (nth-child 1)] (content author)
  [:span (nth-child 2)] (set-attr :href (str "https://gist.github.com/" id))
  [:span (nth-child 2)] (content (str "#" id))
  [:h1] (content description)
  [:aside#date] (content (f/unparse formatter (f/parse date)))
  [:pre#readme] (html-content (md/md-to-html-string readme))
  [:section] (clone-for [file files]
               [:section#id] (set-attr :id (:filename (val file)))
               [:h2 :a] (content (:filename (val file)))
               [:h2 :a] (set-attr :href (str "https://gist.github.com/" id "#file_" (file-name-to-file-id (:filename (val file)))))
               [:div.diagram :img] (set-attr :src (str "data:image/png;base64," (base64-encode (render (:content (val file)) "png"))))
               [:pre.source] (content (:content (val file)))
               [:code] (content (str "&lt;img alt=\"" (:filename (val file)) "\" src=\"http://renderit.herokuapp.com/api/" id "/" (:filename (val file)) ".png")))
               )

(defn load-page
  [page]
  (slurp (io/resource (str "public/" page)) :encoding "UTF-8"))

(def page-index (load-page "index.html"))

(def page-author (load-page "author.html"))

(def page-404 (load-page "404.html"))

(defn page-gist [id]
  (if-let [gist (get-gist id)]
    (apply str(index (gist-snippet gist (extract-file gist "Readme.md") (:files gist))))
    page-404))

(defroutes all-routes
  (context "/api" [] api/routes)
  (GET "/" [] page-index)
  (GET "/:id" [id] (page-gist id))
  (route/resources "/")
  (route/not-found page-404))

(defn -main [port]
  (System/setProperty "java.awt.headless" "true")
  (serve all-routes {:port (Integer. port)}))
