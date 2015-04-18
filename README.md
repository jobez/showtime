# Showtime

It turn your values into captivating, glamorous performers on the clj(s) platform.

## Usage

Let's say you have five tremendous Chuck Norris Jokes and you want the user to only see one at a time, so that they can truly savor them. And you want the jokes to cycle indefinitely, because you are benevolent.

Showtime lets you define how the joke would behave before, during, and after it is rendered via the IPerform, (or IAsyncPrep IPrep), and IStageTime protocols.

Let us look at the Chuck Norris Joke Performer

```
(defrecord CNJoke [text c]
  IPerform
  (end-performance [this] 
    (aset c "innerHTML" (str "<i>" (:joke text) "</i>"))
    (.. js/document (getElementById "joke-container") (appendChild c)))
  (start-performance [this]
    (aset c "innerHTML" (str "<div id='joke'>" (:joke text) "</div>"))
    (aset c "style" "fontSize" (str (+ 15 (rand-int 10))  "px"))
    (.. js/document (getElementById "joke-container") (appendChild c)))
  IPrep
  (will-enter-stage [this])
  (will-leave-stage [this])
  IStageTime
  (perf-time [this] ;; defines the joke's stage time
    (rand-int 5000)))
    ```

You take the content of your jokes and a DOM node and construct a sequence of CNJoke performers, and hand them to the showtime function.

```
(let [jokes (map (fn [joke]
                             (->CNJoke joke c)) jokes)
                [tick close] (showtime jokes)]
            (put! tick :go))
```

Showtime returns a tick and close chan. To begin the show, put a non-nil val on the tick chan. To end the show, put value on pause.

In the case of CNJoke, a simple yet profound performer, there is no need to do any prepwork before entering and leaving the DOM.

For a slightly more sophisticated example, look to the kitten-show,  which implements IAsyncPrep to download an image. If the request isn't completed by the allotted "entrance-slack" time value, it is **skipped**. Because show business is harsh.

## TODO

- think about animation hooks
- write chuck norris jokes



## License

Copyright © 2015 Johann Makram Salib Bestowrous

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
