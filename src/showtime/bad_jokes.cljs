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

(defn get-jokes [node b a]
  (go-loop [jokes []]
    (<! (async/timeout 1000))
    (let [container (.. js/document (getElementById "joke-container"))]
        (if (< (count jokes) 5)
          (let [resp (<! (http/get "http://api.icndb.com/jokes/random?limitTo=[nerdy]&exclude=[explicit]"
                                   {:with-credentials? false}))
                joke (get-in resp [:body :value])]
            (aset container "innerHTML" (str "loading " (inc (count jokes)) " out of " 5 " jokes"))
            (recur (conj jokes joke)))
          (let [jokes (map (fn [joke]
                             (->CNJoke joke node container)) jokes)
                [tick close] (showtime jokes)]
            (aset container "innerHTML" (str "loaded " (count jokes) " out of " 5 " jokes"))
            (aset b "innerHTML" (str "<b>" "Stop the Bad Jokes" "</b>"))
            (aset a "innerHTML" (str "<i>" "Continue Bad Jokes" "</i>"))
            (aset a "onclick" (fn [e]
                                (.. e preventDefault)
                                (put! tick :continue)))
            (aset b "onclick" (fn [e]
                                (.. e preventDefault)
                                (put! close :pause)))
            (.. container (appendChild a))
            (.. container  (appendChild b))
            (put! tick :go))))))

(defn main []
  (let [c (.. js/document (createElement "DIV"))
        b (.. js/document (createElement "BUTTON"))
        a (.. js/document (createElement "BUTTON"))]
    (class/add c "joke")
    (get-jokes c b a)))
