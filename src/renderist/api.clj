;; Copyright 2012 Julien Eluard
;;
;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;;
(ns renderist.api
  (:require [clojure.java.io :as io]
            [compojure.core :as c]
            [renderist.gist :as g]
            [renderist.plantuml :as p]
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
