(ns renderit.web-test
  (:use clojure.test renderit.web ring.mock.request))

(deftest page-not-found
   (is (= (:status (all-routes (request :get "/blurb/dsfsd")))
     404)))
