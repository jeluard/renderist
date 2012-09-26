(ns renderit.web
    (:use renderit.gist)
    (:use renderit.plantuml)
    (:use ring.adapter.jetty)
    (:use compojure.core)
    (:use net.cgrand.enlive-html)
    (:use [markdown :only (md-to-html-string)]
          [clj-time.format :only (parse unparse formatter with-locale)]
          [ring.util.codec :only (base64-encode)])
    (:require [compojure.route :as route]
              [clojure.java.io :as io]
              [ring.util.response :as resp])
    (:use ring.util.json-response)
    (:import java.util.Locale))

(deftemplate index "public/index.html" [string]
  [:div#content] (content string))

(defsnippet diagram-snippet "public/templates/diagram.html" [:section] [id {:keys [file source]} data]
  [:section#id] (set-attr :id file)
  [:a] (content file)
  [:a] (set-attr :href (str "https://gist.github.com/" id "#file_" (file-name-to-file-id file)))
  [:img] (set-attr :src (str "data:image/png;base64," data))
  [:pre.source] (content source)
  [:code] (content (str "&lt;img alt=\"" file "\" src=\"http://renderit.herokuapp.com/api/" id "/" file ".png")))

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
  (slurp (io/resource "public/404.html") :encoding "UTF-8"))

(defn gist-page [id]
  (if-let [gist (get-gist id)]
    (apply str(index (gist-snippet gist (extract-file gist "Readme.md") (:files gist))))
    page-404))

(defroutes api-routes
  (GET "/:id/:name.:extension" [id name extension :as {headers :headers}] 
;;    (println (get headers "cache-control")) 
    (if-not (or (= extension "png") (= extension "svg")) 
      (-> (resp/response (str "Unknow extension " extension))
          (resp/status 415))
      (let [gist (get-gist id)]
        (if (= (:status gist) 404) (resp/not-found (format "Non existing gist %s" id))
          (let [candidates (matching-files(list-files gist) name)]
            (if (empty? candidates) (resp/not-found (format "Cannot find matching file for %s" name))
              (if (not= (count candidates) 1) (resp/not-found (format "Too many candidates: %s" (apply str candidates)))
                (if-let [file (extract-file gist (first candidates))]
                  (->
                    (resp/response (render-stream file extension))
                    (resp/content-type (if (= extension "png") "image/png" "image/svg+xml")))
                  (resp/not-found (format "No file %s in gist %s" name id))
      )))))))))

(def latest [{:name "Name 1" :url "http://gists.github.com/12334"}
             {:name "Name 2" :url "http://gists.github.com/123345"}])

(defroutes all-routes
  (context "/api" [] api-routes)
  (GET "/" [] (root-html latest))
  (GET "/:id" [id] (gist-page id))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/404.html") :encoding "UTF-8")))

(defn -main [port]
  (System/setProperty "java.awt.headless" "true")
  (run-jetty all-routes {:port (Integer. port)})
)
