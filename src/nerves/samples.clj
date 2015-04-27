(ns nerves.samples)

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
  {[[1] "frob"]  [(fn [] (println "Frob action called")) [2]]
   [[1] "blork"] [(fn [] (println "Blork action called")) [3]]
   [[2] "freb"]  [(fn [] (println "Freb action called")) [1]]
   [[2] "blerk"] [(fn [] (println "Blerk action called")) [3]]
   [[3] "frab"]  [(fn [] (println "Frab action called")) [2]]
   [[3] "blark"] [(fn [] (println "Blark action called")) [1]]})

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