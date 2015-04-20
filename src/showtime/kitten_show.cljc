(ns showtime.kitten-show
  #?(:clj (:import javax.imageio.ImageIO
                   [processing.core PImage PConstants]
                   [java.awt.image BufferedImage WritableRaster]))
  (:require [showtime.main :as show :refer (IPerform IPrep IAsyncPrep IStageTime
                                                     showtime)]
            #?@(:clj [[clojure.core.async :as async :refer [<! >! <!! >!! timeout chan alt! alts!! go go-loop alts!]]
                      [manifold.stream :as s]
                      [aleph.http :as http]
                      [quil.core :as q]])))

(defn bis->buffimage [bis]
  "takes a byteinputstream, returns a bufferedimage"
  (javax.imageio.ImageIO/read bis))

(defn buffimage->pimage [^BufferedImage buffimage]
  "converts buffered image objects to pimage objects (oo stinks)"
  (let [buff-image-width (.getWidth buffimage)
        buff-image-height (.getHeight buffimage)
        argb PConstants/ARGB
        pimg (PImage. buff-image-width buff-image-height argb)]
    (.getRGB buffimage 0 0 (.width pimg) (.height pimg) (.pixels pimg) 0 (.width pimg))
    (.updatePixels pimg)
    pimg))

(defn kitten-show [state]
  "starts quil sketch"
  (let [setup (fn []
                (q/smooth)
                (q/no-cursor))
        draw (fn []
               (when-let [kitten (:star @state)]
                 (q/image kitten 0 0))
               (when-let [message (:msg @state)]
                 (q/text (name message) 50 50)))]
    (q/sketch
     :renderer :p2d
     :setup setup
     :draw draw
     :size [300 500])))

(defn gen-kitten []
  "i'd like a place kitten, please'"
  (str "http://placekitten.com/g/"
       (+ 200 (rand-int 100)) "/"
       (+ 300 (rand-int 200))))

(defrecord Kitten [image-url state]
  IPerform
  (show/end-performance [this]
    (swap! state assoc :msg :end-performance)
    this)
  (show/start-performance [this]
    (let [pimg (some->> this ;;take the prepwork
                        :prepwork
                        :body
                        bis->buffimage ;; convert byte input stream to bufferedimage
                        buffimage->pimage ;; convert bufferedimage to a processing image
                        )]
      (swap! state assoc :star pimg) ;; swap it into the quil-sketch state
      (swap! state assoc :msg :a-star-is-born)
      (assoc this :pimg pimg)))
  IAsyncPrep
  (show/awill-enter-stage [this]
    (let [ready-chan (chan)
          reqstream (http/get image-url)] ;; request kitten image
      (s/connect reqstream ready-chan)
      ready-chan))
  (show/awill-leave-stage [this]
    (swap! state assoc :msg :will-leave-stage)
    this)
  IStageTime
  (show/perf-time [this]
    5000)
  (show/entrance-slack [this]
    1000))

(defn start [show-length]
  "a quil sketch that displays some kittens for a given show length"
  (let [state (atom nil)
        kitten-lazy-seq (repeatedly #(->Kitten (gen-kitten) state))
      [start-chan end-chan] (show/showtime (take 5 kitten-lazy-seq))]
  (kitten-show state)
  (async/put! start-chan "here we go")
  (go (<! (async/timeout show-length))
      (swap! state assoc :msg :show-is-over)
      (async/put! end-chan :end)
      (System/exit 0))))
