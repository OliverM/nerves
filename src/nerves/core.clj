(ns nerves.core)

(defrecord Statechart [])

(defrecord State [])

(def sample-state
  {:history             :state                                         ;; :state | :deep
   :concurrent-children true
   :children            []
   :name                "test-state"
   :actions             [                                   ;; pairs of target state (matching :name) and action to perform
                         ["destination-state" (fn [] nil)]
                         ]

   }
  )

(def sample-statechart
  [{}
   {}
   {}])

(def sample-eat                                             ;; event action table
  {
   [[2 3 4] "test"] (fn [] "Test 1 fired")                  ;; requires a table-first invocation, e.g. (apply (sample-eat [[2 3 4] "test"]) nil)
   })