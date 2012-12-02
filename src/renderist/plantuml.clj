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
(ns renderist.plantuml
  (:require [clojure.core.memoize :as m])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [net.sourceforge.plantuml FileFormat FileFormatOption SourceStringReader]))

(defn type-from-extension [extension]
  "plantuml FileFormat from file extension"
  (case extension
    "png" FileFormat/PNG
    "svg" FileFormat/SVG))

(defn supported?
  [extension]
  (or (= extension "png")
      (= extension "svg")))

(defn render [source extension]
  (let [reader (SourceStringReader. source)]
    (with-open [outputStream (ByteArrayOutputStream.)]
      (.generateImage reader outputStream (FileFormatOption. (type-from-extension extension)))
      (.toByteArray outputStream))))

(def render-cached (m/memo-lu render 400))
