(ns showtime.bad-jokes
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.dom.classes :as class]
            [showtime.main :as show :refer (IPerform IPrep IAsyncPrep IStageTime
                                                     showtime
                                                     end-performance
                                                     start-performance
                                                     awill-enter-stage
                                                     awill-leave-stage
                                                     will-enter-stage
                                                     will-leave-stage
                                                     perf-time
                                                     entrance-slack)]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! alts! put!]]))

(enable-console-print!)

(defrecord CNJoke [text node container]
  IPerform
  (end-performance [this]
    (aset node "innerHTML" (str "<i>" (:joke text) "</i>"))
    (.. container (appendChild node)))
  (start-performance [this]
    (aset node "innerHTML" (str "<div id='joke'>" (:joke text) "</div>"))
    (aset node "style" "fontSize" (str (+ 15 (rand-int 10))  "px"))
    (.. container (appendChild node)))
  IPrep
  (will-enter-stage [this])
  (will-leave-stage [this])
  IStageTime
  (perf-time [this]
    (rand-int 5000)))

(defn prepare-dom [joke-node container tick close]
  "create and hook events into buttons and append it to container"
  (let [continue-button (.. js/document (createElement "BUTTON"))
        pause-button (.. js/document (createElement "BUTTON"))]
    (class/add joke-node "joke")
    (aset container "innerHTML" (str "loaded " 5 " out of " 5 " jokes"))
    (aset pause-button "innerHTML" (str "<b>" "Stop the Bad Jokes" "</b>"))
    (aset continue-button "innerHTML" (str "<i>" "Continue Bad Jokes" "</i>"))
    (aset continue-button "onclick" (fn [e]
                                      (.. e preventDefault)
                                      (put! tick :continue)))
    (aset pause-button "onclick" (fn [e]
                                   (.. e preventDefault)
                                   (put! close :pause)))
    (.. container (appendChild pause-button))
    (.. container  (appendChild continue-button))))

(defn start-show [container jokes]
  (let [joke-node (.. js/document (createElement "DIV"))
        jokes (map (fn [joke]
                     (->CNJoke joke joke-node container)) jokes)
        [tick close] (showtime jokes)]
     (prepare-dom joke-node container tick close)
     (put! tick :go)))

(defn get-jokes []
  "requests a single joke every second up till five"
  (go-loop [jokes []]
    (<! (async/timeout 1000))
    (let [endpoint "http://api.icndb.com/jokes/random?limitTo=[nerdy]&exclude=[explicit]"
          container (.. js/document (getElementById "joke-container"))]
        (if (< (count jokes) 5)
          (let [resp (<! (http/get endpoint
                                   {:with-credentials? false}))
                joke (get-in resp [:body :value])]
            (aset container "innerHTML"
                  (str "loading " (inc (count jokes)) " out of " 5 " jokes"))
            (recur (conj jokes joke)))
          (start-show container jokes)))))

(defn main []
  (get-jokes))
