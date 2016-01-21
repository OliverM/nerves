(ns nerves.USF
  (:import (com.github.klangfarbe.statechart
             Metadata Action Guard Statechart
             Event TimeoutEvent
             State PseudoState FinalState HierarchicalState ConcurrentState
             Transition)))

(defn mydata
  "Try to hang some state on the statechart's Metadata. See http://kotka.de/blog/2010/03/proxy_gen-class_little_brother.html
  for reasons not to do this this way..."
  [data]
  (let [state (atom data)]
    (proxy [Metadata clojure.lang.IDeref] []
      (toString [] @state)
      (deref [] state))))

(defn event
  "Instantiate a USF event object with the supplied name."
  [name]
  (proxy [Event] [name]))

(defn action
  "Instantiate a USF action object. Takes a function that should accept a USF Metadata analogue and a USF Parameter
  analogue. Parameters are supplied by event dispatchers to provide data useful in executing the action. To test at the
  REPL, try (.execute (action (fn [metadata parameter] (println \"Test action\"))) nil nil)"
  [action-fn]
  (reify Action
    (execute [this metadata parameter] (action-fn metadata parameter))))

(defn guard
  "Instantiate a USF guard object. Takes a function that should return true or false. The function should accept a USF
  Metadata analogue and a USF Parameter analogue. Parameters are supplied by event dispatchers to provide data useful in
  executing the action."
  [guard-fn]
  (reify Guard
    (check [this metadata parameter] (guard-fn metadata parameter))))

(defn start-state [name parent] (PseudoState. name parent PseudoState/pseudostate_start))
(defn end-state [name parent] (FinalState. name parent))

(defn basic-statechart-USF []
  (let [statechart (Statechart. "basic-statechart" 2 false)
        start-state (start-state "begin" statechart)
        main-state (HierarchicalState. "main" statechart nil nil nil)
        a-state (State. "A" main-state nil nil nil)
        b-state (State. "B" main-state nil nil nil)
        c-state (State. "C" main-state nil nil nil)]
    (do
      ; Transitions have a source state, destination state, event, guard and action. Implement as fn accepting a map of options?
      (Transition. start-state a-state)                     ;; two arity form just specifies start & destination states
      (Transition. a-state b-state (event "frob") nil nil)
      (Transition. a-state c-state (event "blork") nil nil)
      (Transition. b-state a-state (event "freb") nil nil)
      (Transition. b-state c-state (event "blerk") nil nil)
      (Transition. c-state a-state (event "frab") nil nil)
      (Transition. c-state b-state (event "blark") nil nil)
      statechart)
    ))

(comment
  ;; repl session testing basic functionality
  (def scdata (mydata "Test"))
  (def basic-sc (basic-statechart-USF))
  (.start basic-sc scdata)
  (.dispatch basic-sc scdata (event "frob"))
  ;; following returns true, indicating statechart is in state b after the "frob" event
  (.isActive scdata (.getStateByName basic-sc "B"))
  )