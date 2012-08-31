(ns renderit.gist
  (:use [tentacles.gists :only [specific-gist]]
        [clojure.string :only [split]]))
;;http://developer.github.com/v3/gists/
;;http://developer.github.com/v3/#rate-limiting
;;http://bl.ocks.org/1353700
;;https://gist.github.com/748421
;;https://github.com/dakrone/cheshire
;;https://github.com/mmcgrana/clj-json

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
  (specific-gist id))

(defn extract-file [gist name] 
  ""
  (:content ((keyword name) (:files gist))))
