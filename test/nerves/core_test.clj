(ns nerves.core-test
  (:require [clojure.test :refer :all]
            [nerves.core :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(defspec test-check-only
         100
         (prop/for-all [v (gen/not-empty (gen/vector gen/int))]
                       (= (apply min v)
                          (first (sort v)))))

(deftest sample-test
  (is (= 5 (+ 2 2)) "Bad sum test"))

(deftest run-action-test
  (let []))
