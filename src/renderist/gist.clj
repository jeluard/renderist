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
(ns renderist.gist
  (:require [clojure.core.memoize :as m]
            [clojure.string :as str]
            [environ.core :as e]
            [tentacles.gists :as t]
            [clojure.tools.logging :as l]))

;;Github uses lowercase/hyphen string as file id. HTML5 is less restrictive: http://www.w3.org/TR/html5/global-attributes.html#the-id-attribute
(defn file-name-to-file-id [name]
  (str "file_" name))

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

(defn get-credentials []
  {:auth (str (e/env :github-user false) ":" (e/env :github-password false))})

(defn get-gist [id]
  ""
  (let [gist (t/specific-gist id (get-credentials))]
    (if-not (contains? gist :status)
      gist
      (do
        (l/warn "Got status <" (:status gist)  "> while accessing gist <" id ">")
        nil))))

(def get-gist-cached (m/memo-lu get-gist 400))

(defn extract-file [gist name]
  ""
  (get-in gist [:files (keyword name) :content]))
