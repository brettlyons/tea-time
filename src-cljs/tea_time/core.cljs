(ns tea-time.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [ajax.core :as ajax :refer [GET POST]])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  [:li {:class (when (= page (session/get :page)) "active")}
   [:a {:href uri
        :on-click #(reset! collapsed? true)}
    title]])

(defn navbar []
  (let [collapsed? (atom true)]
    (fn []
      [:nav.navbar.navbar-inverse.navbar-fixed-top
       [:div.container
        [:div.navbar-header
         [:button.navbar-toggle
          {:class         (when-not @collapsed? "collapsed")
           :data-toggle   "collapse"
           :aria-expanded @collapsed?
           :aria-controls "navbar"
           :on-click      #(swap! collapsed? not)}
          [:span.sr-only "Toggle Navigation"]
          [:span.icon-bar]
          [:span.icon-bar]
          [:span.icon-bar]]
         [:a.navbar-brand {:href "#/"} "Tea Time"]]
        [:div.navbar-collapse.collapse
         (when-not @collapsed? {:class "in"})
         [:ul.nav.navbar-nav
          [nav-link "#/" "Home" :home collapsed?]
          [nav-link "#/list-page" "List Of Teas Page" :special-page collapsed?]
          [nav-link "#/about" "About" :about collapsed?]]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "Tea Time is a simple web page to display tea, and tea ingredients"]]])

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to Tea Time"]
    ;; [:p "Time to start building your site!"]
    ;; [:p [:a.btn.btn-primary.btn-lg {:href "http://luminusweb.net"} "Learn more »"]]
    ]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to Tea Time"]]]
   (when-let [docs (session/get :docs)]
     [:div.row
      [:div.col-md-12
       [:div {:dangerouslySetInnerHTML
              {:__html (md->html docs)}}]]])])

(def teas-list (atom nil))
(def new-tea (atom nil))
(defn get-teas [] (ajax/GET "/api/teas" :handler (fn [response] (reset! teas-list response))))


(defn show-edit-form 
  "show/hide the show-edit-form"
  [& eventObj id]
  (println "show-edit-form" eventObj "||" (first id) "||" (.value id)))

(defn tealister
  "The page component for listing tea"
  []
  (get-teas)
  (fn []
    [:div.row
     [:table.table.table-striped
      [:thead
       [:tr
        [:th "Tea #"]
        [:th "Tea Name"]
        [:th "Edit"]]]
      [:tbody
       (for [tea @teas-list]
         ^{:key tea}
         [:tr [:td (first tea)] [:td (second tea)] [:td [:input {:type "button" :value "Edit" :on-click show-edit-form}]]])]]]))

(defn post-to-teas-db
  "Sends an Ajax Request to the API route to post the tea from the form to the database"
  [tea-name]
  ;; (println "post-to-teas-db hit, params: " tea-name)
  (ajax/POST "/api/newtea" {:params {:new-tea tea-name} :format :json
                           :handler (fn [] (println "New Tea Posted") (get-teas))
                           :error-handler (fn [err] (println "New Tea Failed To Post" err))}))

(defn tea-adder
  "A component for the tea-adder form"
  []
  [:div.row
   [:div.col-md-12
    [:form {:post "/api/newtea"}
     [:input.form-control {:field :text :id :in-tea :value @new-tea :on-change #(reset! new-tea (-> % .-target .-value))}
      [:input {:type "submit" :value "Add Tea" :on-click #(post-to-teas-db @new-tea)}]]]]])

;; this passes the value of the new tea atom into the post-to-teas-db function

(defn list-page []
  (let [tea-value (reagent/atom "")]
    [:div.container 
     [:div.row
      [:div.col-md-12
       "Here is a list of teas"]]
     [:div.row {:style {:display "flex" :align-items "flex-end"}}
      [:div.col-md-6
       [tealister]]
      [:div.col-md-6 
       [tea-adder]]]]))


(def pages
  {:home #'home-page
   :about #'about-page
   :list-page #'list-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix nil)

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/list-page" []
  (session/put! :page :list-page))
 

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          EventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (reagent/render [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
