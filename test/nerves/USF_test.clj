(ns nerves.USF-test
  (:require [clojure.test :refer :all]
           [nerves.USF :refer :all])
  (:import (nerves.USF SCData)))

(deftest basic-USF-statechart
  (let [mydata (SCData.)
        statechart (statechart "basic-statechart")
        start-state (state :start "begin" statechart)
        main-state (state :hierarchical "main" statechart)
        a-state (state :leaf "A" main-state)
        b-state (state :leaf "B" main-state)
        c-state (state :leaf "C" main-state)]
    (connect-transitions
      [[start-state a-state]
       [a-state b-state (event "frob") nil nil]
       [a-state c-state (event "blork") nil nil]
       [b-state a-state (event "freb") nil nil]
       [b-state c-state (event "blerk") nil nil]
       [c-state a-state (event "frab") nil nil]
       [c-state b-state (event "blark") nil nil]])

    (is (= (.start statechart mydata) true) "Statechart successfully initialised")
    (is (= (.isActive mydata (.getStateByName statechart "A")) true) "Statechart moved to state A automatically")
    (is (= (.dispatch statechart mydata (event "frob")) true) "State A responded to event \"frob\"")
    (is (= (.isActive mydata (.getStateByName statechart "B")) true) "State B now active after previous event")))


#_(deftest USF-docs-statechart
  (let [mymetadata (scdata)
        ]))