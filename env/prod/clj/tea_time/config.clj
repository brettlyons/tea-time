(ns tea-time.config
  (:require [taoensso.timbre :as timbre]))

(def defaults
  {:init
   (fn []
     (timbre/info "\n-=[tea-time started successfully]=-"))
   :middleware identity})
