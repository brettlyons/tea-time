(ns tea-time.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [ajax.core :as ajax :refer [GET POST]]
            [re-frame.core :as re-frame])
  (:require-macros [reagent.ratom :refer [reaction]])
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
    [:h1 "Welcome to Tea Time"]]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to Tea Time"]]]
   (when-let [docs (session/get :docs)]
     [:div.row
      [:div.col-md-12
       [:div {:dangerouslySetInnerHTML
              {:__html (md->html docs)}}]]])])

(re-frame/register-handler
  :process-teas-response
  (fn [app-db [_ response]]
    (assoc-in app-db [:teas-list] response)))

(re-frame/register-handler
  :process-teas-bad-response
  (fn [app-db [_ response]]
    (println "ERROR: " response)
    app-db))

(re-frame/register-handler
  :load-teas
  (fn [app-db _]
    (ajax/GET "/api/teas"
              {:handler #(re-frame/dispatch [:process-teas-response %1])
               :error-handler #(re-frame/dispatch [:process-teas-bad-response %1])
               :response-format :json
               :keywords? true})
    app-db))

(re-frame/register-handler
  :delete-tea
  (fn [db [_ tea-name]]
    (ajax/GET (str "/api/teas/" tea-name "/delete")
              :error-handler (fn [response]
                               (println "DELETE-TEA ERROR" response))
              :handler (fn [response]
                         (re-frame/dispatch [:load-teas])))))

(re-frame/register-handler
  :delete-tea
  (fn [app-db [_ id]]
    (ajax/GET (str "/api/teas/" id "/delete")
              :error-handler (fn [response] (println "DELETE-TEA ERROR " response))
              :handler #(re-frame/dispatch [:process-delete]))
    app-db))

; -- SAVE FOR TESTING ASYNC DOM UPDATE --
;(re-frame/register-handler
;  :update-tea
;  (fn [app-db [_ id new-name]]
;    (.setTimeout js/window #(ajax/GET (str "/api/teas/" id "/update/" new-name)
;                                      :error-handler (fn [response]
;                                                       (println "UPDATE-TEA ERROR" response))
;                                      :handler (fn [response] (println response))) 4000)
;    app-db)

(re-frame/register-handler
  :update-tea
  (fn [app-db [_ id new-name]]
    (ajax/GET (str "/api/teas/" id "/update/" new-name)
              :error-handler (fn [response]
                               (println "UPDATE-TEA ERROR " response))
              :handler (fn [response] (re-frame/dispatch [:load-teas])))
    app-db))


(re-frame/register-handler
  :process-delete
  (fn [app-db [_ response]]
    (re-frame/dispatch [:load-teas])
    app-db))

(re-frame/register-handler
  :initialize-db
  (fn [_ _]
    {:teas-list []}))

(re-frame/register-handler
  :replace-tea
  (fn [app-state [_ replaced-by edit-tea-input]]
    (re-frame/dispatch [:update-tea (:id replaced-by) edit-tea-input])
    (assoc-in app-state [:teas-list] (replace {replaced-by
                                               {:id (:id replaced-by)
                                                :name edit-tea-input}}
                                              (:teas-list app-state)))))

(re-frame/register-sub
  :teas
  (fn [db]
    (reaction (:teas-list @db))))

(re-frame/register-sub
  :new-tea
  (fn [db]
    (reaction (:new-tea @db))))

(re-frame/register-handler
  :tea-posted
  (fn [app-state]
    (re-frame/dispatch [:load-teas])))

(re-frame/register-handler
  :create-tea
  (fn [app-state [_ tea-name]]
    (ajax/POST "/api/newtea" {:params {:new-tea tea-name}
                              :format :json
                              :handler #(re-frame/dispatch [:tea-posted])
                              :error-handler (fn [err] (println "New Tea Failed To Post" err))})
    app-state))

(defn value-event
  [event]
  (-> event .-target .-value))

(defn tea-edit-toggler
  [tea]
  (let [edit? (reagent/atom false)
        tmp-edit (reagent/atom (:name tea))]
    (fn [tea]
      (if @edit?
        [:tr
         [:td
          [:input.form-control {:type "text"
                                :value @tmp-edit
                                :on-change #(reset! tmp-edit (value-event %))}]
          [:div.row {:style {:margin-bottom "15px"}}]
          [:button.btn.btn-success.pull-right
           {:on-click (fn [e]
                        (re-frame/dispatch
                          [:replace-tea tea @tmp-edit])
                        (swap! edit? not))}
           "Update"]
          [:button.btn.btn-danger.pull-right
           {:on-click (fn [e]
                        (re-frame/dispatch [:delete-tea (:name tea)]))
            :style {:margin-right "10px"}}
           "Delete"]]]
        [:tr {:on-click #(swap! edit? not)}
         [:td.pull-left (:name tea)]]))))

(defn tealister
  "The page component for listing tea"
  []
  (let [teas (re-frame/subscribe [:teas])]
    (fn []
      [:div.row
       [:table.table.table-striped
        [:thead
         [:tr
          [:th "Tea Name"]]]
        [:tbody
         (for [tea @teas]
           ^{:key tea}
           [tea-edit-toggler tea])]]])))

(defn tea-adder
  "A component for the tea-adder form"
  []
  (let [tmp-new-tea (reagent/atom "")]
    (fn []
      [:div.row
       [:div.col-md-12
        [:input.form-control {:type "text"
                              :placeholder "Name of New Tea"
                              :value @tmp-new-tea
                              :on-change #(reset! tmp-new-tea (value-event %))}]
        [:button.btn.btn-primary.pull-right {:style {:margin-top "10px"}
                                             :on-click #(re-frame/dispatch [:create-tea @tmp-new-tea]
                                                                           (reset! tmp-new-tea ""))} (str "Add " @tmp-new-tea)]]])))

(defn list-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "Here is a list of teas"]]
   [:div.row
    [:div.col-xs-6
     [tealister]
     [tea-adder]]
    [:div.col-xs-6]]])


(def pages
  {:home #'home-page
   :about #'about-page
   :list-page #'list-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :list-page))

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
;; (defn fetch-docs! []
;;   (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (reagent/render [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  ;; (fetch-docs!)
  (re-frame/dispatch [:initialize-db])
  (re-frame/dispatch [:load-teas])
  (hook-browser-navigation!)
  (mount-components))
