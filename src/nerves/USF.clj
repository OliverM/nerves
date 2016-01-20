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
  analogue. Parameters are supplied by event dispatchers to provide data useful in executing the action."
  [action]
  (proxy [Action] []
    (execute [data parameter]
      (action data parameter))))

