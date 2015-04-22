(ns nerves.core)

(defrecord Statechart [])

(defrecord State [])

(def sample-state
  {:history :state                                         ;; :state | :deep
   :concurrent-children true
   :children []
   :name "test-state"

   }
  )

(def sample-statechart
  [{}
   {}
   {}])
