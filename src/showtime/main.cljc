(ns showtime.main
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]]))
  (:require
   #?@(:cljs [[plumbing.core :refer-macros [fnk]]
              [cljs.core.async :as async :refer [timeout <! chan close! put!]]]

             :clj [[plumbing.core :refer :all :exclude [update]]

                   [clojure.core.async :as async :refer [<! >! <!! >!! timeout chan alt! alts!! go go-loop alts!]]])))

(defprotocol IPerform
  "Entering and Leaving the senses"
  (end-performance [this])
  (start-performance [this]))

(defprotocol IPrep
  "rigid prepwork for entrances and exits"
  (will-enter-stage [this])
  (will-leave-stage [this]))

(defprotocol IAsyncPrep
  "it's done when it's done prepwork for entrances and exits"
  (awill-enter-stage [this])
  (awill-leave-stage [this]))

(defprotocol IStageTime
  "the amount of time given to performer's presence"
  (entrance-slack [this])
  (exit-slack [this])
  (perf-time [this]))

(defn- showtime-process [stage tick]
  (go-loop [performer (<! stage)]
    (if (satisfies? IAsyncPrep performer)
      ;; if the performer has time sensitive prepwork needs...
      (alt! ;; the user gives performer a predefined deadline to get on stage
        (timeout (entrance-slack performer)) ([_]
                                              (println "prepwork failed")
                                              (>! tick :go))
        ;; performer tries to get their prepwork done, if not, they are skipped!
        (awill-enter-stage performer) ([prepwork]
                                       (start-performance (assoc performer :prepwork prepwork))
                                       (<! (async/timeout (perf-time performer)))
                                       (awill-leave-stage performer)
                                       (end-performance performer)
                                       (>! tick :go)))
      (do ;; otherwise prepwork is handled syncronously
        (will-enter-stage performer)
        (start-performance performer)
        (<! (async/timeout (perf-time performer)))
        (will-leave-stage performer)
        (end-performance performer)
        (>! tick :go)))

    (recur (<! stage))))

(defn showtime [item-sequence]
  {:pre [(every? #(and (satisfies? IStageTime %) ;; we need to make sure every item
                       (satisfies? IPerform %)
                       (or (satisfies? IPrep %)
                           (satisfies? IAsyncPrep %)))   ;; satisfies the protocols
                 item-sequence)]} ;; or else there will be madness
  (let [tick (chan)
        close (chan)
        stage (chan)]
    (showtime-process stage tick) ;; ready stage
    (go-loop [performers item-sequence performed []] ;; coordinate performers
      (alt!
        tick ([_] (if-let [performer (first performers)]
                    (do
                      #?(:clj (>! stage performer)
                              :cljs (put! stage performer))
                      (recur (rest performers) (conj performed performer)))
                    (do
                      (println "cycling through performers")
                      #?(:clj (>! stage (first performed))
                              :cljs (put! stage (first performed)))
                      (recur (rest performed) (conj [] (first performed))))
                    ))
        close ([_] (alt! ;; if, after sending a val to the close chan there is
                         ;; no other signals on the tick chan, the show is over
                       (timeout 5000) ([_] (println "show's over"))
                       tick ([_] (recur performers performed))))))
    [tick close]))
