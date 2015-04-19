

# Showtime

This library is very much in progress. Feel free to ping me with any thoughts / introductions to similiar ideas and implementations. Learning is cool.

## running the demos

- [install boot](https://github.com/boot-clj/boot#install)

- clone & cd to directory

- ```boot browser-example```  loads the inbrowser view that cycles through Chuck Norris Jokes ad infinitum localhost:3000

- ```boot quil-example wait``` fires up a quil sketch that cycles through pictures of kittens for 30 seconds

## Usage

Let's say you have five tremendous Chuck Norris Jokes and you want the user to only see one at a time, so that they can truly savor them. And you want the jokes to cycle indefinitely, because you are benevolent.

And just for the sake of the readme, let's say the display looks like this:
![CNJ are the past and future of comedy](http://i.imgur.com/Sib8ivv.gif)

Showtime lets you define how the joke would behave before, during, and after it is rendered via the IPerform, (or IAsyncPrep IPrep), and IStageTime protocols.

Let us look at the Chuck Norris Joke Performer

```
(defrecord CNJoke [text node container]
  IPerform
  (end-performance [this] ;; this method is call
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
```

You take the content of your jokes and a DOM node and construct a sequence of CNJoke performers, and hand them to the showtime function.

```
(let [jokes (map (fn [joke] (->CNJoke joke c)) jokes)
          [tick close] (showtime jokes)]
          (put! tick :go))
```

Showtime returns a tick and close chan. To begin the show, put a non-nil val on the tick chan. To end the show, put value on pause.

In the case of CNJoke, a simple yet profound performer, there is no need to do any prepwork before entering and leaving the DOM.

For a slightly more sophisticated example, look to the kitten-show,  which implements IAsyncPrep to lazy-load an image. If the request isn't completed by the allotted "entrance-slack" time value, it is **skipped**. Because show business is harsh.

## TODO

- think about animation hooks
- write chuck norris jokes



## License

Copyright © 2015 Johann Makram Salib Bestowrous

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
