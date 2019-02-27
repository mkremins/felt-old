(defproject mkremins/felt "0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [datascript "0.18.1"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.4.0"]]
  :plugins [[lein-cljsbuild "1.1.4"]]
  :cljsbuild {:builds [{:id "app"
                        :source-paths ["src"]
                        :compiler {:main felt.app
                                   :optimizations :advanced
                                   :output-dir "docs/js/app"
                                   :output-to "docs/js/app.js"
                                   :source-map "docs/js/app.js.map"}}]})
