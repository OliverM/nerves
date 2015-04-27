(ns nerves.core
  (:require [clojure.walk :refer [walk]]
            [clojure.set :refer [map-invert]]
            [clojure.core.match :refer [match]]
            [aprint.core :refer [aprint ap]]))

(defrecord Statechart [])

(defrecord State [])

(def sample-state
  {:default             true                                            ;; if sibling states exist, this flag indiciates which is the default start state
   :history             :state                                         ;; :state | :deep
   :concurrent-children true
   :children            []
   :name                "test-state"
   :actions             [;; triples of action name, target state (matching :name) and action to perform
                         ["action-name" "destination-state" (fn [] nil)]
                         ]
   })

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

(def nested-statechart                                      ;; fig. 6.6 in Horrocks
  [{:name "A"
    :default true
    :actions [["3" "B" (fn [] "Action 3 called")]]
    :children [
               {:name "C"
                :actions [["2" "B" (fn [] "Action 2 called")]]}
               {:name "D"
                :default true}]}
   {:name "B"
    :actions [["4" "D" (fn [] "Action 4 called")]]}]
  )

(defn statechart->eat
  "Convert a statechart specification into a functioning event-action table"
  [statechart]
  (let [state-counter (atom 0)
        id-dict (atom {})]
    (->> (map (fn [state]                                    ;; state-level manipulations
               (let [id (swap! state-counter inc)]
                 (swap! id-dict assoc (:name state) @state-counter)
                 (assoc state :id id)))
             statechart)
        (reduce (fn [eat state]
                  (into eat
                        (for [action (:actions state)]
                          [[[(:id state)] (nth action 0)]     ;; statechart status and action
                           [(nth action 2) [(@id-dict (nth action 1))]]
                           ])))
                {}))))

(def sample-eat                                             ;; event action table
  {[[1] "frob"]  [(fn [] (println "Frob action called")) [2]]
   [[1] "blork"] [(fn [] (println "Blork action called")) [3]]
   [[2] "freb"]  [(fn [] (println "Freb action called")) [1]]
   [[2] "blerk"] [(fn [] (println "Blerk action called")) [3]]
   [[3] "frab"]  [(fn [] (println "Frab action called")) [2]]
   [[3] "blark"] [(fn [] (println "Blark action called")) [1]]})


(defn run-action
  "Applies the supplied state and action to the event action table, returning the resulting state.
  If the state-action combination is not found, a warning is printed to the REPL and the originally
  supplied state is returned."
  [eat state action]
  (if-let [[action-fn dest-state] (eat [state action])]
    (do
      (action-fn)
      dest-state)
    (do
      (printf "Warning: state action combination %s %s not found in event-action table." state action) ;; TODO: proper logging
      state)))

;; sample invocation
(run-action sample-eat [3] "blark")                         ;; gives [1]