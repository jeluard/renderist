(ns renderist.web-test
  (:use clojure.test renderist.web ring.mock.request))

(deftest page-not-found
   (is (= (:status (all-routes (request :get "/blurb/dsfsd")))
     404)))
