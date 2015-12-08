(ns tea-time.app
  (:require [tea-time.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
