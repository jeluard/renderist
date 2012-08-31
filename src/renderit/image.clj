(ns renderit.image
  (require minemap.core)
    (import java.io.File)
    (import java.awt.Color)
    (import java.awt.image.BufferedImage)
    (import javax.imageio.ImageIO))

(defn draw-png
  "Take width, height, and the map of mines. Save to a file.
    Supposed to take a generate-random-map{,-perc} mapping."
  [width height minemap file]
  (let [block 5 ;block size
       bi (BufferedImage. (* block width) (* block height) BufferedImage/TYPE_INT_ARGB)
       g (.createGraphics bi)]
    (do
      (.setColor g (*colors* :background))
      (.fillRect g 0 0 (* block width) (* block height))
      (doseq [[[x y] high] minemap]
        (.setColor g (*colors* high))
        (.fillRect g (* block x) (* block y) block block))
      (ImageIO/write bi "png" (File. file)))))
