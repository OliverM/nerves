(ns nerves.USF
  (:require [clojure.core.match :refer [match]])
  (:import (com.github.klangfarbe.statechart
             Metadata Action Guard Statechart
             Event TimeoutEvent
             State PseudoState FinalState HierarchicalState ConcurrentState
             Transition)))

(defn statechart
  "Translates the java USF Statechart constructor into Clojure"
  ([name] (statechart name 2 false))                        ;; default to two non-daemon threads (the minimum allowed)
  ([name threads daemon] (Statechart. name threads daemon)))

(defn state
  "Gathers the various java USF state types into a single factory function keyed by :type"
  ([type name parent]
    (state type name parent nil nil nil))
  ([type name parent on-enter after-enter on-exit]
  (match type
         :start (PseudoState. name parent PseudoState/pseudostate_start)
         :hierarchical (HierarchicalState. name parent on-enter after-enter on-exit)
         :concurrent (ConcurrentState. name parent on-enter after-enter on-exit)
         :leaf (State. name parent on-enter after-enter on-exit)
         :final (FinalState. name parent))))

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

(defn basic-statechart-USF []
  (let [statechart (statechart "basic-statechart")
        start-state (state :start "begin" statechart)
        main-state (state :hierarchical "main" statechart)
        a-state (state :leaf "A" main-state)
        b-state (state :leaf "B" main-state)
        c-state (state :leaf "C" main-state)]
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