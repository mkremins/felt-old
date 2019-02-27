(ns felt.app
  (:require [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom]))

(enable-console-print!)

(defonce app-state
  (atom {}))

(defcomponent app [data owner]
  (render [_]
    (dom/div {}
      )))

(om/root app app-state {:target (js/document.getElementById "app")})
