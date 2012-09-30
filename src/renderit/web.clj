(ns renderit.web
    (:use [renderit.gist]
          [renderit.plantuml]
          [ring.server.standalone]
          [ring.util.codec :only (base64-encode)]
          [ring.util.response :only (content-type not-found response status)]
          [compojure.core]
          [net.cgrand.enlive-html]
          [clojure.java.io :only (resource)]
          [markdown :only (md-to-html-string)]
          [clj-time.format :only (parse unparse formatter with-locale)])
    (:require [compojure.route :as route])
    (:import java.util.Locale))

(deftemplate index "public/index.html" [string]
  [:div#content] (content string))

(def format (with-locale (formatter "MMM dd, yyyy") Locale/US));default to UTC time zone

(defsnippet gist-snippet "public/templates/diagram.html" [:body :> any-node] [{:keys [id description] date :created_at {author :login} :user} readme files]
  [:span (nth-child 1)] (set-attr :href (str "/" author))
  [:span (nth-child 1)] (content author)
  [:span (nth-child 2)] (set-attr :href (str "https://gist.github.com/" id))
  [:span (nth-child 2)] (content (str "#" id))
  [:h1] (content description)
  [:aside#date] (content (unparse format (parse date)))
  [:pre#readme] (html-content (md-to-html-string readme))
  [:section] (clone-for [file files]
               [:section#id] (set-attr :id (:filename (val file)))
               [:h2 :a] (content (:filename (val file)))
               [:h2 :a] (set-attr :href (str "https://gist.github.com/" id "#file_" (file-name-to-file-id (:filename (val file)))))
               [:div.diagram :img] (set-attr :src (str "data:image/png;base64," (base64-encode (render (:content (val file)) "png"))))
               [:pre.source] (content (:content (val file)))
               [:code] (content (str "&lt;img alt=\"" (:filename (val file)) "\" src=\"http://renderit.herokuapp.com/api/" id "/" (:filename (val file)) ".png")))
               )

(defn root-html [latest]
  (apply str(index latest)))

(def page-404
  (slurp (resource "public/404.html") :encoding "UTF-8"))

(def page-author
  (slurp (resource "public/author.html") :encoding "UTF-8"))

(defn gist-page [id]
  (if-let [gist (get-gist id)]
    (apply str(index (gist-snippet gist (extract-file gist "Readme.md") (:files gist))))
    page-404))

(defroutes api-routes
  (GET "/:id/:name.:extension" [id name extension :as {headers :headers}] 
    (if-not (or (= extension "png") (= extension "svg")) 
      (-> (response (str "Unknow extension " extension))
          (status 415))
      (let [gist (get-gist id)]
        (if (= (:status gist) 404) (not-found (format "Non existing gist %s" id))
          (let [candidates (matching-files(list-files gist) name)]
            (if (empty? candidates) (not-found (format "Cannot find matching file for %s" name))
              (if (not= (count candidates) 1) (not-found (format "Too many candidates: %s" (apply str candidates)))
                (if-let [file (extract-file gist (first candidates))]
                  (->
                    (response (render-stream file extension))
                    (content-type (if (= extension "png") "image/png" "image/svg+xml")))
                  (not-found (format "No file %s in gist %s" name id))
      )))))))))

(def latest [{:name "Name 1" :url "http://gists.github.com/12334"}
             {:name "Name 2" :url "http://gists.github.com/123345"}])

(defroutes all-routes
  (context "/api" [] api-routes)
  (GET "/" [] (root-html latest))
  (GET "/:id" [id] (gist-page id))
  (route/resources "/")
  (route/not-found page-404))

(defn -main [port]
  (System/setProperty "java.awt.headless" "true")
  (serve all-routes {:port (Integer. port)})
)
