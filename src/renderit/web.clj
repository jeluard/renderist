(ns renderit.web
    (:use renderit.gist)
    (:use renderit.plantuml)
    (:use ring.adapter.jetty)
    (:use compojure.core) 
    (:use net.cgrand.enlive-html) 
    (:require [compojure.route :as route]
              [clojure.java.io :as io]
              [ring.util.response :as resp])
    (:use ring.util.json-response))

(comment (deftemplate index "public/index.html"
  [latest]
  [:div#content] (content "Enlive!!!")
  [:ul#items :li] (clone-for [item latest]
    (content item))))

(deftemplate index "public/index.html" [string]
  [:div#content] (content string))

(defsnippet diagram "public/templates/diagram.html" [:body :> any-node] [id name]
  [:a (attr-has :href "url")] (content id)
  [:div.author :span] (content name)
  [:img] (content (str "data:image/png;base64," name)))

(defn root-html [latest]
  (apply str(index latest)))

(defn diagram-html [id]
  (apply str(index (diagram id "jeluard"))))

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
                    (resp/response (render file extension))
                    (resp/content-type (if (= extension "png") "image/png" "image/svg+xml")))
                  (resp/not-found (format "No file %s in gist %s" name id))
      )))))))))

(def latest [{:name "Name 1" :url "http://gists.github.com/12334"}
             {:name "Name 2" :url "http://gists.github.com/123345"}])

(defroutes all-routes
  (context "/api" [] api-routes)
  (GET "/" [] (root-html latest))
  (GET "/:id" [id] (diagram-html id))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/404.html") :encoding "UTF-8")))

(defn -main [port]
  (System/setProperty "java.awt.headless" "true")
  (run-jetty all-routes {:port (Integer. port)})
)
