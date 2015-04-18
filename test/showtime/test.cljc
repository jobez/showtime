(ns showtime.test
  (:require #?(:cljs [cljs.test    :as test :refer-macros [deftest is]]
               :clj  [clojure.test :as test :refer        [deftest is]])))

(deftest seven
  (is (= 7 7)))

(deftest eight
  (is (= 8 8)))
