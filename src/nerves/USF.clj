(ns nerves.USF
  (:require [clojure.core.match :refer [match]])
  (:import (com.github.klangfarbe.statechart
             ;; Metadata implemented as SCData via gen-class in nerves.USF.SCData
             Action Guard Statechart
             Event TimeoutEvent
             State PseudoState FinalState HierarchicalState ConcurrentState
             Transition)
           (nerves.USF SCData)
           ))

(defn statechart
  "Translates the java USF Statechart constructor into Clojure"
  ([name] (statechart name 2 false))                        ;; default to two non-daemon threads (the minimum allowed)
  ([name threads daemon] (Statechart. name threads daemon)))

(defn scdata
  "Factory function for SCData objects extending the USF Metadata class"
  ([] (SCData.))
  ([data] (let [obj (SCData.)]
            (.setData obj data)
            obj)))

(defn state
  "Gathers the various java USF state types into a single factory function keyed by :type"
  ([type name parent]
    (state type name parent nil nil nil))
  ([type name parent on-enter after-enter on-exit]
  (match type
         :start (PseudoState. name parent PseudoState/pseudostate_start)
         :hierarchical (HierarchicalState. name parent on-enter after-enter on-exit)
         :concurrent (ConcurrentState. name parent on-enter after-enter on-exit)
         :junction (PseudoState. name parent PseudoState/pseudostate_junction)
         :join (PseudoState. name parent PseudoState/pseudostate_join)
         :fork (PseudoState. name parent PseudoState/pseudostate_fork)
         :leaf (State. name parent on-enter after-enter on-exit)
         :final (FinalState. name parent))))

(defn transition
  "Translates the USF Transition constructor into Clojure."
  ([source destination] (Transition. source destination))
  ([source destination event] (Transition. ^State source ^State destination ^Event event))
  ([source destination event guard action] (Transition. source destination event guard action)))

;; TODO: handle internal transitions. These have no start & end states but must have an action and an event (guards are optional). They're used when the currently active state can handle the event and need not transfer to another state (and so the entry, exit actions etc aren't fired)

(defn connect-transitions
  "Connects the supplied vector of transitions to the set of states. States must already have been instantiated."
  [transitions]
  (doall (map (partial apply transition) transitions)))

(defn event
  "Instantiate a USF event object with the supplied name."
  [name]
  (proxy [Event] [name]))

(defn timeout-event
  "Instantiate a USF timeout object. These have their names hard-coded to TimeoutEvent by USF, and the transition action
   is fired on the timeout expiring. Timeout parameter is a long expressed in milliseconds."
  [timeout]
  (proxy [TimeoutEvent] [timeout]))

(defn action
  "Instantiate a USF action object. Takes a function that should accept a USF Metadata derivative and a USF Parameter
  derivative. Parameters are supplied by event dispatchers to provide data useful in executing the action."
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
