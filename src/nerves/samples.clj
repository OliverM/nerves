(ns nerves.samples
  (:require [nerves.core :as n]
            [clojure.zip :as z]
            [aprint.core :refer [aprint ap]]))

;; Testing basic action, all states siblings
(def basic-statechart
  [{:name    "StateA"
    :default true
    :actions [
              ["frob" "StateB" (fn [] "Frob action called")]
              ["blork" "StateC" (fn [] "Blork action called")]
              ]}
   {:name    "StateB"
    :actions [
              ["freb" "StateA" (fn [] "Freb action called")]
              ["blerk" "StateC" (fn [] "Blerk action called")]
              ]}
   {:name    "StateC"
    :actions [
              ["frab" "StateB" (fn [] "Frab action called")]
              ["blark" "StateA" (fn [] "Blark action called")]
              ]}])

;; TODO: default state, default entry & exit actions, user-specified entry & exit actions
(def basic-sc-eat                                             ;; event action table
  {[[1] "frob"]  [(n/action (fn [] (println "Frob action called"))) [2]]
   [[1] "blork"] [(n/action (fn [] (println "Blork action called"))) [3]]
   [[2] "freb"]  [(n/action (fn [] (println "Freb action called"))) [1]]
   [[2] "blerk"] [(n/action (fn [] (println "Blerk action called"))) [3]]
   [[3] "frab"]  [(n/action (fn [] (println "Frab action called"))) [2]]
   [[3] "blark"] [(n/action (fn [] (println "Blark action called"))) [1]]})

;; sample invocation
(n/run-action basic-sc-eat [3] "blark")                         ;; gives [1]


;; Testing refinement, tracking state across multiple levels

(def nested-statechart                                      ;; fig. 6.6 in Horrocks
  [{:name "A"
    :default true
    :actions [["3" "B" (fn [] "Action 3 called")]]
    :children [
               {:name "C"
                :actions [["2" "B" (fn [] "Action 2 called")]]}
               {:name "D"
                :default true
                :actions [["1" "C" (fn [] "Action 1 called")]]}]}
   {:name "B"
    :actions [["4" "D" (fn [] "Action 4 called")]]}]
  )

;; TODO: default state, default entry & exit actions, user-specified entry & exit actions
(def nested-sc-eat                                          ;; this is breadth-first in name->id
  {[[1 4] "3"] [(fn [] "Action 3 called") [2 0]]
   [[1 4] "1"] [(fn [] "Action 1 called") [1 3]]
   [[1 3] "3"] [(fn [] "Action 3 called") [2 0]]
   [[1 3] "2"] [(fn [] "Action 2 called") [2 0]]
   [[2 0] "4"] [(fn [] "Action 4 called") [1 4]]})

(def parent-eat
  {[[1] "3"] [(fn [] "Action 3 called") [2]]
   [[2] "4"] [(fn [] "Action 4 called") [4]]})

(def child-eat
  {[[3] "2"] [(fn [] "Action 2 called") [2]]
   [[4] "1"] [(fn [] "Action 1 called") [3]]})



;; current test strategy for walk-along fns...
;(def t-lca-loc (n/walk-along test-zip identical? (filter (set four-path) seven-path)))
;(n/walk-along t-lca-loc identical? (drop (count t-lca-loc) seven-path))
;(n/walk-along t-lca-loc identical? (drop (count t-lca-loc) four-path))



;; testing LCA routing for statecharts...
(def zipped-nsc (n/sc-zip nested-statechart))
(def c-posn (-> zipped-nsc
                z/down
                z/down
                z/path))
(def b-posn (-> zipped-nsc
                z/down
                z/right
                z/path))
(n/walk-along zipped-nsc identical? (filter (set c-posn) b-posn))