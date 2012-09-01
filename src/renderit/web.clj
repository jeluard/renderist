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

(deftemplate t1 "public/index.html" []
  [:p]   (content "Enlive!!!"))

(defn enlive-template []
    (apply str(t1)))

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

(defroutes all-routes
  (context "/api" [] api-routes)
  (GET "/" [] (enlive-template))
  (route/resources "/")
  (route/not-found (slurp (io/resource "public/404.html") :encoding "UTF-8")))

(defn -main [port]
  (System/setProperty "java.awt.headless" "true")
  (run-jetty all-routes {:port (Integer. port)})
)
