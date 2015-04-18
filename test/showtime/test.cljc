(ns showtime.test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [alt! go go-loop]]))
  (:require [showtime.main :as show :refer (IPerform IPrep IAsyncPrep IStageTime
                                                      showtime
                                                      end-performance
                                                      start-performance
                                                      awill-enter-stage
                                                      awill-leave-stage
                                                      will-enter-stage
                                                      will-leave-stage
                                                      perf-time
                                                      entrance-slack)]
            #?@(:cljs [[cljs.test    :as test :refer-macros [deftest is]]
                       [cljs.core.async :as async :refer [timeout <! chan close! put!]]]
                :clj  [[clojure.test :as test :refer        [deftest is]]
                             [clojure.core.async :as async :refer [put! <! >! <!! >!! timeout chan alt! alts!! go go-loop alts!]]
                             ])))

(defrecord HurriedPerformer [cycle-tracker]
  IPrep
  (will-enter-stage [this]
    (swap! cycle-tracker conj :will-enter-stage))
  (will-leave-stage [this]
    (swap! cycle-tracker conj :will-leave-stage))
  IPerform
  (start-performance [this]
    (swap! cycle-tracker conj :start-performance))
  (end-performance [this]
    (swap! cycle-tracker conj :end-performance))
  IStageTime
  (perf-time [this]
    1000))

(let [rushed (take 1 (repeatedly #(->HurriedPerformer (atom []))))
      [start end] (showtime rushed)]
  (put! start :ok)
  (go (<! (async/timeout 1050))
      (put! end :ok)
      (deftest proper-order
        (is (= [:will-enter-stage :start-performance :will-leave-stage :end-performance]
               (vec (take 4 @(:cycle-tracker (first rushed)))))))))
