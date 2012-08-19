(ns renderit.web
    (:use ring.adapter.jetty)
    (:use compojure.core) 
    (:require [compojure.route :as route])
    (:require [compojure.handler :as handler]
              [ring.util.response :as resp])
    (:use ring.util.json-response))

(defn handler [request]
    {:status 200
        :headers {"Content-Type" "text/html"}
        :body "Hello World"})

(defroutes api-routes
  (GET "/:id/:name.:format" [id name format] (json-response {:id id :name name :format format})))

(defroutes all-routes
  (context "/api" [] api-routes)
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
 ;; (GET "/" [] (resp/redirect "/index.html"))
  (route/resources "/")
  (route/not-found "Page not found"))

(def application-routes
  all-routes)

(defn -main [port]
  (run-jetty application-routes {:port (Integer. port)})
)
