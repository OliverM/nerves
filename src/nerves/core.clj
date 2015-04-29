(ns nerves.core
  (:require [clojure.walk :refer [walk]]
            [clojure.set :refer [map-invert]]
            [clojure.zip :as z]
            [clojure.core.match :refer [match]]
            [aprint.core :refer [aprint ap]]
            [nerves.samples :as ns]))

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


(defn statechart->eat
  "Convert a statechart specification into a functioning event-action table"
  [statechart]
  (let [state-counter (atom 0)
        id-dict (atom {})]
    (->> (map (fn [state]                                    ;; state-level manipulations
               (let [id (swap! state-counter inc)]
                 (swap! id-dict assoc (:name state) @state-counter)
                 (assoc state :id id)))
             statechart)
        (reduce (fn [eat state]
                  (into eat
                        (for [action (:actions state)]
                          [[[(:id state)] (nth action 0)]     ;; statechart status and action
                           [(nth action 2) [(@id-dict (nth action 1))]]
                           ])))
                {}))))


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
      (printf "Warning: state action combination %s %s not found in event-action table." state action) ;; TODO: proper logging
      state)))

;; sample invocation
(run-action ns/basic-sc-eat [3] "blark")                         ;; gives [1]

(defn sc-zip
  "Create a zipper to navigate & manipulate statecharts. Wraps top level state collection in a root state."
  [root]
  (z/zipper
    identity
    (fn [state] (:children state))
    (fn [state children]
      (assoc state :children children))
    {:children root}))

(defn- sc->state-index-length
  [statechart]
  "Determine the length of the active state vector for indexing into the event-action table.
  Increments by one for each state with children, by the count of the child states for states with
  concurrent children, or is unchanged for states without children."
  (loop [loc (sc-zip statechart)
         req-indices 0]
    (if (z/end? loc)
      req-indices
      (recur (z/next loc)
             (let [this-node (z/node loc)]
               (if (:concurrent-children this-node)
                 (+ (count (:children this-node)) req-indices)
                 (if (:children this-node)
                   (+ 1 req-indices)
                   req-indices)))))))

(defn sc-visitor
  [statechart]
  (loop [loc (sc-zip statechart)
         counter 1
         id-dict {}]
    (if (z/end? loc)
      [(z/root loc) id-dict]
      (recur (z/next (z/edit loc assoc :id counter))
             (inc counter)
             (assoc id-dict (or (:name (z/node loc)) (str counter)) counter)))))