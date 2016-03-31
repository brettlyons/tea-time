(ns tea-time.routes.home
  (:require [tea-time.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [tea-time.db.core :as db]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]))

(defn home-page [& param]
  ;(println db/get-teas)
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
  (GET "/api/teas/:name/delete" [name]
       :summary "Deletes the named tea"
       (db/delete-tea! {:name name})
       (ok (str name " Tea Deleted")))
  (GET "/api/teas/:id/update/:newname" [id newname]
       ;; (println id newname)
       :summary "Updates the named tea"
       (db/update-tea! {:id (Integer/parseInt id) :newname newname})
       (ok (str newname " modified")))
  (GET "/api/teas" []
        :return :json 
        :query-params []
        :summary "The list of teas from the db"
        (ok (db/get-teas-name)))
  (POST "/api/newtea" [] (fn [req]
                           (db/create-tea! {:tea (get (:params req) :new-tea)})
                           ;; (println (get (:params req) :new-tea))
                           (ok "Tea Added")))

  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))

