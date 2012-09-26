(ns renderit.gist
  (:use [tentacles.gists :only [specific-gist]]
        [clojure.string :only [split]]))
;;http://developer.github.com/v3/gists/
;;http://developer.github.com/v3/#rate-limiting
;;http://bl.ocks.org/1353700
;;https://gist.github.com/748421
;;https://github.com/dakrone/cheshire
;;https://github.com/mmcgrana/clj-json

;;Github uses lowercase/hyphen string as file id. HTML5 is less restrictive: http://www.w3.org/TR/html5/global-attributes.html#the-id-attribute
(defn file-name-to-file-id [name]
  (str name))

(defn list-files [gist]
  "List files part of this gist."
  (map #(name %) (keys (:files gist))))

(defn chop-extension [file]
  ""
  (first (split file #"\.")))

(defn matches? [file candidate]
  ""
  (= (chop-extension file) candidate))

(defn matching-files [files prefix]
  ""
  (filter #(matches? % prefix) files))

(defn get-gist [id]
  ""
  (let [gist (specific-gist id)]
    (if (not= 404 (:status gist))
      gist nil)))

(defn extract-file [gist name]
  ""
  (get-in gist [:files (keyword name) :content]))
