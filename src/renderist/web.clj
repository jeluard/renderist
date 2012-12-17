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
(ns renderist.web
  (:require [clj-time.format :as f]
            [clojure.java.io :as io]
            [compojure.core :as c]
            [compojure.route :as r]
            [markdown :as md]
            [net.cgrand.enlive-html :as h]
            [renderist.api :as a]
            [renderist.gist :as g]
            [renderist.plantuml :as p]
            [ring.server.standalone :as s])
  (:import java.util.Locale))

(def formatter (f/with-locale (f/formatter "MMM dd, yyyy") Locale/US)) ;default to UTC time zone

(h/deftemplate blank "public/blank.html" [snippet]
  [:div#content] (h/content snippet))

(h/defsnippet file-snippet "public/diagram.html" [:section :> h/any-node] [id filename filename-no-extension filecontent]
  [:h2 :a] (h/content filename-no-extension)
  [:h2 :a] (h/set-attr :href (str "https://gist.github.com/" id "#" (g/file-name-to-file-id filename)))
  [:div.diagram :img] (h/set-attr :src (str id "/" filename-no-extension ".png"))
  [:pre.source] (h/content filecontent)
  [:code] (h/content (str "<img alt=\"" filename "\" src=\"http://renderist.herokuapp.com/" id "/" filename-no-extension ".png\" />")))

(def description-file "Diagrams.md")

(defn valid? [filename]
  (not= filename description-file))

(h/defsnippet author-snippet "public/diagram.html" [:span#author] [author]
  [:a] (h/do->
         (h/set-attr :href (str "https://gist.github.com/" author))
         (h/content author)))

(h/defsnippet gist-snippet "public/diagram.html" [:#content :> h/any-node] [{:keys [id description] date :created_at {author :login} :user} readme files]
  [:span#author] (when author (h/content (author-snippet author)))
  [:span#id :a] (h/do->
                  (h/set-attr :href (str "https://gist.github.com/" id))
                  (h/content (str "#" id)))
  [:h1] (h/content description)
  [:aside#date] (h/content (f/unparse formatter (f/parse date)))
  [:pre#readme] (h/html-content (md/md-to-html-string readme))
  [:section] (h/clone-for [file files
                           :let [filename (:filename (val file))
                                 filename-no-extension (g/chop-extension filename)
                                 filecontent (:content (val file))]
                           :when (valid? filename)]
               (h/do->
                 (h/set-attr :id filename-no-extension)
                 (h/content (file-snippet id filename filename-no-extension filecontent)))))

(defn html-resource-snippet [source selector]
  (h/select (h/html-resource source) selector))

(def snippet-index (html-resource-snippet "public/index.html" [:body :> h/any-node]))
(def snippet-404 (html-resource-snippet "public/404.html" [:body :> h/any-node]))

(def page-index (blank snippet-index))
(def page-404 (blank snippet-404))
(def robots (slurp (io/resource "public/robots.txt") :encoding "UTF-8"))

(defn page-gist [id]
  (if-let [gist (g/get-gist-cached id)]
    (blank (gist-snippet gist (g/extract-file gist description-file) (:files gist)))
    page-404))

(c/defroutes all-routes
  (c/GET "/" [] page-index)
  (c/GET "/robots.txt" [] robots)
  (c/GET "/:id/:name.:extension" [id name extension :as {headers :headers}] (a/render id name extension headers))
  (c/GET "/:id" [id] (page-gist id))
  (r/resources "/resources")
  (r/not-found page-404))

(defn -main []
  (System/setProperty "java.awt.headless" "true")
  (s/serve all-routes {:open-browser? false}))
