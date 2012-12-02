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
