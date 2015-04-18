(set-env!
 :source-paths #{"src" "test"}
 :resource-paths #{"html"}
 :dependencies '[[adzerk/boot-cljs "0.0-2814-3" :scope "test"]
                 [adzerk/boot-reload "0.2.6"]
                 [adzerk/boot-test "1.0.4" :scope "test"]
                 [adzerk/bootlaces "0.1.11" :scope "test"]
                 [aleph "0.4.0-beta3"]
                 [manifold "0.1.0"]
                 [cljs-http "0.1.30"]
                 [http-kit "2.1.16"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/plumbing "0.4.2"]
                 [pandeiro/boot-http  "0.6.3-SNAPSHOT"]
                 [org.clojure/clojure "1.7.0-beta1" :scope "provided"]
                 [quil "2.2.5"]])

(require '[adzerk.bootlaces :refer [bootlaces! build-jar push-release]]
         '[adzerk.boot-cljs      :refer [cljs]]
         '[pandeiro.boot-http :refer :all]
         '[adzerk.boot-reload    :refer [reload]]
         '[adzerk.boot-test :refer [test]]
         '[showtime.kitten-show :as kitten])

(def +version+ "0.0.1")

(bootlaces! +version+)

(task-options!
 pom  {:project     'johann/showtime
       :version     +version+
       :description ""
       :url         ""
       :scm         {:url ""}
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask run-cljs-test
  "Run cljs tests"
  []
  (fn middleware [next-handler]
    (fn handler [fileset]
      (sh "node" "target/script.js")
      (-> fileset next-handler))))

(deftask dev
  []
  (comp (watch)
        (serve)
        (speak)
        (reload :on-jsload 'showtime.bad-jokes/main)
        (cljs :source-map true :optimizations :none)
        (test)))

(deftask release []
  (comp (build-jar)
        (push-release)))

(deftask kitten-show
  []
  "runs a quil sketch that cycles through kittens for 30secs"
  (fn middleware [next-handler]
    (fn handler [fileset]
      (kitten/start 30000)
      (-> fileset next-handler))))

(deftask bad-jokes []
  "serves demo app at localhost:3000"
  (comp (watch)
        (serve)
        (cljs :source-map true :optimizations :none)))
