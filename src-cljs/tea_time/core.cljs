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
          [nav-link "#/special-page" "Special Page" :special-page collapsed?]
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

(defn tealister
  "The page component for listing tea"
  []
  (let [get-teas (fn [] (ajax/GET "/api/teas" :handler (fn [response]
                                                         (reset! teas-list (:body response)
                                                                 (println teas-list)))))]
    (get-teas)
    (fn []
      [:ul
       (for [tea teas-list]
         ^{:key tea} [:li "Tea: " (:name tea)])])))

(defn handler [response]
  response)

(defn get-teas []
  (ajax/GET "/api/teas"
            {:headers {"Accept" "application/json"}
             :handler handler}))

;; (defn post-to-teas-db
;;   "Sends an Ajax Request to the API route to post the tea from the form to the database"
;;   [tea-name & other-params]
;;   (println "post-to-teas-db hit, param: " (:params tea-name))
;;   ajax/POST "/newtea" {:params {:new-tea tea-name} :format :json :handler handler})


(defn special-page []
  (let [tea-value (reagent/atom "")]
    [:div.container
     [:div.row
      [:div.col-md-12
       "Special page holds things"]]
     [:div.row
      [tealister]
      [:input {:type "button" :value "Retreive Tea List" :on-click get-teas}]
      ]]))

   ;; [:div.row
   ;;  [:div.col-md-12
   ;;   [:input.form-control {:field :text :id :tea}
   ;; [:input {:type "button" :value "Add Tea" :on-click #(post-to-teas-db :tea)}]]]]]) )

(def pages
  {:home #'home-page
   :about #'about-page
   :special-page #'special-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/special-page" []
  (session/put! :page :special-page))
 

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
