(ns nerves.USF-test
  (:require [clojure.test :refer :all]
           [nerves.USF :refer :all]))

(deftest SCData-mintest
  (let [full (scdata "Test")]
    (is (= (.data full) "Test") "USF metadata properly set")
    (.setData full "Test2")
    (is (= (.data full) "Test2") "USF Metadata properly reset")
    ))

(deftest basic-USF
  (let [mydata (scdata)
        statechart (statechart "basic")
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


(deftest USF-docs
  (let [metadata (scdata {:value 1})
        statechart (statechart "USF-docs")

        ;; actions
        setValue (action (fn [data parameter] (.setData data {:value 10})))
        decValue (action (fn [data parameter] (let [value (:value (.data data))]
                                                (.setData data {:value (dec value)})))) ;; I seem to be working hard to avoid using atoms properly here
        printValue (action (fn [data parameter] (println (str "Current data value is: " (.data data)))))

        ;; guards
        valueEquals (guard (fn [data parameter] (= (.data data) 0)))

        ;; states
        start-state (state :start "begin" statechart)
        end-state (state :final "end" statechart)
        h-state-a (state :hierarchical "A" statechart setValue nil nil)
        state-b (state :leaf "B" h-state-a)
        link-state (state :junction "B<->A-final" h-state-a)
        a-end-state (state :final "A-final" h-state-a)
        c-state-c (state :concurrent "C" h-state-a)
        h-state-c-1 (state :hierarchical "C region 1" c-state-c)
        c1-start-state (state :start "C region 1 start" h-state-c-1)
        state-d (state :leaf "D" h-state-c-1
                       (action (fn [_ _] (println "Concurrent state D activated")))
                       nil
                       (action (fn [_ _] (println "Concurrent state D deactivated"))))
        h-state-c-2 (state :hierarchical "C region 2" c-state-c)
        c2-start-state (state :start "C region 2 start" h-state-c-2)
        state-e (state :leaf "E" h-state-c-2 (action (fn [_ _] (println "Start timeout"))) nil nil)
        state-f (state :leaf "F" h-state-c-2 decValue nil nil)]
    (connect-transitions
      [[start-state state-b]
       [state-b c-state-c]
       [c1-start-state state-d]
       [c2-start-state state-e]
       [state-e state-f (timeout-event 1000) nil (action (fn [_ _] (println "Timeout event fired.")))]
       [state-f state-e (event "Another event")]
       [c-state-c link-state (event "anEvent")]
       [link-state state-b nil valueEquals nil]
       [link-state a-end-state]
       [h-state-a end-state]])
    (is (= (.start statechart metadata) true) "Statechart successfully initialised")
    (is (= (.isActive metadata (.getStateByName statechart "E")) true) "Statechart moved to state E automatically")
    (Thread/sleep 1500)
    (is (= (.isActive metadata (.getStateByName statechart "F")) true) "Statechart moved to state F after timeout event")
    (is (= (.dispatch statechart metadata (event "anEvent")) true) "State F responded to event \"anEvent\"")
    (is (= (.dispatch statechart metadata (event "anEvent")) false) "Sending second anEvent ignored")
    (is (= (.getData metadata (.getStateByName statechart "A")) nil) "State A deactivated after failing guard test caused transition to final state")))