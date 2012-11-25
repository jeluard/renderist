(ns renderit.gist
  (:require [clojure.string :as str]
            [tentacles.gists :as t]))
;;http://developer.github.com/v3/gists/
;;http://developer.github.com/v3/#rate-limiting
;;http://bl.ocks.org/1353700
;;https://gist.github.com/748421
;;https://github.com/dakrone/cheshire
;;https://github.com/mmcgrana/clj-json

;;Github uses lowercase/hyphen string as file id. HTML5 is less restrictive: http://www.w3.org/TR/html5/global-attributes.html#the-id-attribute
(defn file-name-to-file-id [name]
  (str name))

(defn camel-case-to-hyphen [string]
  (str/replace string #"[A-Z]" #(str \- (str/lower-case %))))

(defn list-files [gist]
  "List files part of this gist."
  (map #(name %) (keys (:files gist))))

(defn chop-extension [file]
  ""
  (first (str/split file #"\.")))

(defn matches? [file candidate]
  ""
  (= (chop-extension file) candidate))

(defn matching-files [files prefix]
  ""
  (filter #(matches? % prefix) files))

(defn get-gist [id]
  ""
  (let [gist (t/specific-gist id)]
    (if (not= 404 (:status gist))
      gist nil)))

(defn extract-file [gist name]
  ""
  (get-in gist [:files (keyword name) :content]))
