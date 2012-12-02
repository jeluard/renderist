(ns renderist.gist-test
  (:use clojure.test renderist.gist))

(deftest file-id
  (is (= "file_name.txt" (file-name-to-file-id "FILEName.txt")))
  (is (= "file_name.txt" (file-name-to-file-id "file-name.txt")))
  (is (= "filename.txt" (file-name-to-file-id "Filename.txt")))
  (is (= "filename.txt" (file-name-to-file-id "filename.txt"))))
