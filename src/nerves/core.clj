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
