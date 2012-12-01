(ns renderit.web
  (:require [clj-time.format :as f]
            [compojure.core :as c]
            [compojure.route :as r]
            [markdown :as md]
            [net.cgrand.enlive-html :as h]
            [renderit.api :as a]
            [renderit.gist :as g]
            [renderit.plantuml :as p]
            [ring.server.standalone :as s])
  (:import java.util.Locale))

(def formatter (f/with-locale (f/formatter "MMM dd, yyyy") Locale/US)) ;default to UTC time zone

(h/deftemplate blank "public/blank.html" [snippet]
  [:div#content] (h/content snippet))

(h/defsnippet file-snippet "public/diagram.html" [:section :> h/any-node] [id filename filecontent]
  [:#id] (h/set-attr :id filename)
  [:h2 :a] (h/content filename)
  [:h2 :a] (h/set-attr :href (str "https://gist.github.com/" id "#" (g/file-name-to-file-id filename)))
  [:div.diagram :img] (h/set-attr :src (str "/api/" id "/" (g/chop-extension filename) ".png"))
  [:pre.source] (h/content filecontent)
  [:code] (h/content (str "<img alt=\"" filename "\" src=\"http://renderit.herokuapp.com/api/" id "/" (g/chop-extension filename) ".png\" />")))

(def description-file "Diagrams.md")

(defn valid? [filename]
  (not= filename description-file))

(h/defsnippet gist-snippet "public/diagram.html" [:#content :> h/any-node] [{:keys [id description] date :created_at {author :login} :user} readme files]
  [:span (h/nth-child 1)] (h/do->
                            (h/set-attr :href (str "https://gist.github.com/" author))
                            (h/content author))
  [:span (h/nth-child 2)] (h/do->
                            (h/set-attr :href (str "https://gist.github.com/" id))
                            (h/content (str "#" id)))
  [:h1] (h/content description)
  [:aside#date] (h/content (f/unparse formatter (f/parse date)))
  [:pre#readme] (h/html-content (md/md-to-html-string readme))
  [:section] (h/clone-for [file files
                           :let [filename (:filename (val file))
                                 filecontent (:content (val file))]
                           :when (valid? filename)]
               (h/content (file-snippet id filename filecontent))))

(defn html-resource-snippet [source selector]
  (h/select (h/html-resource source) selector))

(def snippet-index (html-resource-snippet "public/index.html" [:body :> h/any-node]))
(def snippet-404 (html-resource-snippet "public/404.html" [:body :> h/any-node]))

(def page-index (blank snippet-index))
(def page-404 (blank snippet-404))

(defn page-gist [id]
  (if-let [gist (g/get-gist-cached id)]
    (blank (gist-snippet gist (g/extract-file gist description-file) (:files gist)))
    page-404))

(c/defroutes all-routes
  (c/context "/api" [] a/routes)
  (c/GET "/" [] page-index)
  (c/GET "/:id" [id] (page-gist id))
  (r/resources "/")
  (r/not-found page-404))

(defn -main []
  (System/setProperty "java.awt.headless" "true")
  (s/serve all-routes {:open-browser? false}))
