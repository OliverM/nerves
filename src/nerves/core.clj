(ns nerves.core
  (:require [clojure.core.async :as a]
            [clojure.walk :refer [walk]]
            [clojure.set :refer [map-invert]]
            [clojure.zip :as z]
            [zip.visit :as v]
            [puget.printer :refer [cprint]]
            [nerves.USF :as usf]))


(def sample-state
  {:name                "test-state"
   :type                :concurrent                         ;; :concurrent | :start | :final | nil
   :history             :state                              ;; :state | :deep | nil
   :children            []                                  ;; children states
   ;; quadruplets of event name, target state (matching :name), action to perform, and guard on that action (if any)
   :events             [["event-name" "destination-state" (fn [] nil) nil]]})

(defrecord StatechartData [active-states event-handlers timed-events])
(defrecord State [type name transitions children history])
(defrecord Transition [target-state event action guard])


(defn sc-zip
  "Convert a statechart to a zipper"
  [root-state]
  (z/zipper
    (fn [in] (instance? State in))                          ;; can we have children
    (fn [state] (:children state))                          ;; get state children
    (fn [state children] (assoc state :children children))  ;; new node with supplied children
    root-state))

(defn ->USF-statechart
  "Create a USF statechart from the nerves statechart spec"
  ([statechart] ->USF-statechart [statechart "Unnamed"])
  ([statechart name]
    (let [zsc (sc-zip statechart)
          usf (usf/statechart name)]
      )))













































(defn state=
  "Check if states are equal by comparing :name"
  [left right]
  (= (:name left) (:name right)))

(defn lca
  "Given two locations from the same zipper, find their lowest common ancestor."
  [start end]
  (let [start-depth (count (z/path start))
        end-depth (count (z/path end))]
    (cond
      (state= (z/node start) (z/node end)) start
      (< start-depth end-depth) (recur start (z/up end))
      (> start-depth end-depth) (recur (z/up start) end))))

  (defn lca-path [start-loc end-loc]
  "Traverse the zipper between the supplied two zipper paths via their lowest common ancestor."
  (if (identical? start-loc end-loc)
    []
    (let [sczip (z/root start-loc)
          start-path (z/path start-loc)
          start-node (z/node start-loc)
          end-path (z/path end-loc)
          end-node (z/node end-loc)
          lca-path (filter (set start-path) end-path)
          lca-node [(last lca-path)]
          lca-to-start (conj (vec (drop (count lca-path) start-path)) start-node)
          lca-to-end (conj (vec (drop (count lca-path) end-path)) end-node)
          ]
      (cprint {:start lca-to-start
               :lca   lca-node
               :end   lca-to-end})

      (concat (reverse lca-to-start) lca-node lca-to-end)))
  )

(defmacro action
  "Generate an action for a state."
  [anon-fn]
  (let [fn-name (gensym "n-")]
    `(def ~fn-name ~anon-fn)))




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


(defn- sc->state-index-length
  "Determine the length of the active state vector for indexing into the event-action table.
  Increments by one for each state with children, by the count of the child states for states with
  concurrent children, or is unchanged for states without children."
  [statechart]
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

(defn sc->id-sc
  "Assign each state a unique id and return a pair of the id-annotated statechart and the dictionary
  translating ids to names. Assigns the id as the key to the dictionary in cases of states without
  names (e.g. anonymous nesting states) and the implicit root state."
  [statechart]
  (loop [loc (sc-zip statechart)
         counter 0
         id-dict {}]
    (if (z/end? loc)
      [(z/root loc) id-dict]
      (recur (z/next (z/edit loc assoc :id counter))
             (inc counter)
             (assoc id-dict (or (:name (z/node loc)) (str counter)) counter)))))

(defn merge-eats
  "Merge a child eat with a parent eat"
  [parent child]
  (into {} (for [parent-transition (seq parent)
                 child-transition (seq child)]
             (let [[[parent-start-state parent-event] [parent-action parent-end-state]] parent-transition
                   [[child-start-state child-event] [child-action child-end-state]] child-transition
                   merged-start-state (into parent-start-state child-start-state)]
               {[merged-start-state parent-event] [parent-action parent-end-state]
                [merged-start-state child-event]  [child-action child-end-state]}
               )))
  )

