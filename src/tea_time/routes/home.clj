(ns tea-time.routes.home
  (:require [tea-time.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [tea-time.db.core :as db]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]))

(defn home-page [& param]
  (println db/get-teas)
  (layout/render "home.html"))

;; (defn tea-page []
;;   (layout/render
;;    "tea-list.html"
;;    (merge {:teas (db/get-teas)}
;;           (select-keys flash [:name :tea :errors]))))

;; (defn validate-message [params]
;;   (first
;;    (b/validate
;;     params
;;     :name v/required
;;     :tea [v/required [v/min-count 4]])))

;; (defn save-message! [{:keys [params]}]
;;   (if-let [errors (validate-message params)]
;;     (-> (redirect "/")
;;         (assoc :flash (assoc params :errors errors)))
;;     (do
;;       (db/save-message!
;;        (assoc params :timestamp (java.util.Date.)))
;;             (redirect "/"))))

;; (defn new-tea-page [] )

(defroutes home-routes
  (GET "/" req (home-page req))
  (GET "/api/teas" []
        :return map
        :query-params []
        :summary "The list of teas from the db"
        (ok (db/get-teas)))
  ;(POST "/newtea" (save-tea! request))
  (POST "/newtea" req (println req))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))

