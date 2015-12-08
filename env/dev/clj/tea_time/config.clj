(ns tea-time.config
  (:require [selmer.parser :as parser]
            [taoensso.timbre :as timbre]
            [tea-time.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (timbre/info "\n-=[tea-time started successfully using the development profile]=-"))
   :middleware wrap-dev})
