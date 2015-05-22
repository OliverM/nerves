(ns nerves.core-test
  (:require [clojure.test :refer :all]
            [nerves.core :refer :all]
            [clojure.zip :as z]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(defspec test-check-only
         100
         (prop/for-all [v (gen/not-empty (gen/vector gen/int))]
                       (= (apply min v)
                          (first (sort v)))))

;(deftest route-test
;  (let [zsc-6-6 (sc-zip [{:name     "A"                     ;; fig. 6.6 in Horrocks
;                          :default  true
;                          :actions  [["3" "B" (fn [] "Action 3 called")]]
;                          :children [
;                                     {:name    "C"
;                                      :actions [["2" "B" (fn [] "Action 2 called")]]}
;                                     {:name    "D"
;                                      :default true
;                                      :actions [["1" "C" (fn [] "Action 1 called")]]}]}
;                         {:name    "B"
;                          :actions [["4" "D" (fn [] "Action 4 called")]]}])
;        b-path (-> zsc-6-6 z/down z/right z/node)
;        c-path (-> zsc-6-6 z/down z/down z/node)]
;    (is (state=
;          (-> zsc-6-6 z/down z/node)
;          ())                                               ;; TODO: construct util fn for locating LCA?
;        "LCA should find lowest common ancestor")))

(deftest lca-test
  (let [test-zip (z/vector-zip [0 [1] [2 [3 [4] 5 [6 [7] [8]]]]])
        four (-> test-zip z/down z/right z/right z/down z/right z/down z/right z/down)
        seven (-> test-zip z/down z/right z/right z/down z/right z/down z/right z/right z/right z/down z/right z/down)]
    (is (= (z/node four) 4))
    (is (= (z/node seven) 7))
    (is (= (lca four seven) (-> four z/up z/up)))))