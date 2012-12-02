(ns renderit.api
  (:require [clojure.java.io :as io]
            [compojure.core :as c]
            [renderit.gist :as g]
            [renderit.plantuml :as p]
            [ring.util.response :as r])
  (:import java.util.Locale))

(defn mime-from-extension
  [extension]
  (if (= extension "png") "image/png" "image/svg+xml"))

(defn image-response
  [file extension]
  (-> (r/response (io/input-stream (p/render-cached file extension)))
      (r/content-type (mime-from-extension extension))))

(defn render [id name extension headers]
  (if-not (p/supported? extension)
    (-> (r/response (str "Unsupported extension " extension))
        (r/status 415))
    (let [gist (g/get-gist-cached id)]
      (if (= (:status gist) 404)
        (r/not-found (format "Non existing gist %s" id))
        (let [candidates (g/matching-files(g/list-files gist) name)]
          (if (empty? candidates)
            (r/not-found (format "Cannot find matching file for %s" name))
            (if (not= (count candidates) 1)
              (r/not-found (format "Too many candidates: %s" (apply str candidates)))
              (if-let [file (g/extract-file gist (first candidates))]
                (image-response file extension)
                (r/not-found (format "No file %s in gist %s" name id))))))))))
