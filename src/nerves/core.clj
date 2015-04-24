(ns nerves.core)

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
    :actions [["frob"] ["StateB"] (fn [] "Frob action called")
              ["blork"] ["StateC"] (fn [] "Blork action called")
              ]}
   {:name    "StateB"
    :actions [["freb"] ["StateA"] (fn [] "Freb action called")
              ["blerk"] ["StateC"] (fn [] "Blerk action called")
              ]}
   {:name    "StateC"
    :actions [["frab"] ["StateB"] (fn [] "Frab action called")
              ["blark"] ["StateA"] (fn [] "Blark action called")
              ]}])

(defn statechart->eat
  "Convert a statechart specification into a functioning event-action table"
  [])

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
      (println "Warning: state action combination not found in event-action table.") ;; TODO: proper logging
      state)))

;; sample invocation
(run-action sample-eat [3] "blark")                         ;; gives [1]