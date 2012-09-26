;;http://plantuml.sourceforge.net/jdot/jdot.html
;;http://plantuml.sourceforge.net/salt.html
;;http://ditaa.org/ditaa/
;;http://ditaa.sourceforge.net/#download
;;http://plantuml.sourceforge.net/ditaa.html
(ns renderit.plantuml
  (:import net.sourceforge.plantuml.SourceStringReader)
  (:import net.sourceforge.plantuml.FileFormat)
  (:import net.sourceforge.plantuml.FileFormatOption)
  (:import java.io.ByteArrayOutputStream))

(defn type-from-extension [extension]
  ""
  (case extension
    "png" FileFormat/PNG
    "svg" FileFormat/SVG))

(defn render [source extension]
  (let [reader (SourceStringReader. source)
        outputStream (ByteArrayOutputStream.)]
    (.generateImage reader outputStream (FileFormatOption. (type-from-extension extension)))
    (.toByteArray outputStream)))

(defn render-stream [source extension]
  (java.io.ByteArrayInputStream. (render source extension)))
