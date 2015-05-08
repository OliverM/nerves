(ns nerves.core
  (:require [clojure.walk :refer [walk]]
            [clojure.set :refer [map-invert]]
            [clojure.zip :as z]
            [zip.visit :as v]
            [aprint.core :refer [aprint ap]]))

;; Utility zipper functions to navigate a zipper via a path, based on version by Meikel Brandmeyer
;; via google group discussion here: https://groups.google.com/forum/#!topic/clojure/v9ZTnsNnqVs
(defn walk-along-and-do
"Follow along the path from the given loc. When the path is contained
invoke the found-action. Otherwise invoke the not-found-action with
the loc of the last contained node as well as the not contained path
components. pred is used to identify the nodes."
[loc pred p found-action not-found-action]
; Get rid of special case: the empty path. This makes pc in
; the loop always non-nil.
(if-let [p (seq p)]
  (loop [loc loc
         [pc & pcs :as p] p]
    (let [is-equal (pred (z/node loc) pc)]
      (cond
        ; No match. Try the next node.
        (and (not is-equal)
             (z/right loc)) (recur (z/right loc) p)

        ; No match. Report the parent and the original path
        (not is-equal) (not-found-action (z/up loc) p)

        ; From here on the node matches!
        ; In case we have nothing left in the path, we
        ; found the target node.
        (nil? pcs) (found-action loc)

        ; For branch go on for the children and the rest of
        ; the path's components.
        (and (z/branch? loc)
             (z/down loc)) (recur (z/down loc) pcs)

        ; In any other case the path is not contained in the tree.
        :else (not-found-action loc pcs))))
  (found-action loc)))

(defn gen-path
  "Generate a series of actions that navigate to a location on a zipper"
  [loc compare-fn  ])

(defn walk-along
  "Follow along the path from the given loc. In case the path is not
  contained in the zipper, an exception is thrown. The empty path is
  always contained and leaves the loc as is. pred is used to identify
  the nodes."
  [loc pred p]
  (walk-along-and-do loc pred p identity
                     (fn [_ _] (throw (new Exception "path not in tree")))))

(defn try-walk-along
  "Try to follow along the path from the given loc. In case the path
  is not contained in the zipper, the last contained location and the
  rest of the path are returned in a vector. The empty path is always
  contained and leaves the loc as is. pred is used to identify the
  nodes."
  [loc pred p]
  (walk-along-and-do loc pred p (fn [loc] [loc nil])
                     (fn [loc rpath] [loc rpath])))

(defn lca-path
  "Traverse the zipper between the supplied two zipper paths via their lowest common ancestor."
  [sczip start-path end-path]
  (let [path-filter (set start-path)
        lca-loc (walk-along sczip identical? (filter path-filter end-path))
        dofn (fn [loc] (println "At node: " (z/node loc)))]

    ;; descend to start point from lca
    (walk-along-and-do lca-loc identical?
                       (drop (count lca-loc) start-path)
                       dofn
                       (fn [last-contained-loc divergent-path]
                         (throw (new Exception "LCA to start path not in tree"))))

    ;; descend to end point from lca
    (walk-along-and-do lca-loc identical?
                       (drop (count lca-loc) end-path)
                       dofn
                       (fn [last-contained-loc divergent-path]
                         (throw (new Exception "LCA to end path not in tree"))))))

(defn lca-path [start-loc end-loc]
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
    (aprint {:start lca-to-start
             :lca lca-node
             :end lca-to-end})

    (concat (reverse lca-to-start) lca-node lca-to-end))
  )

(defmacro action
  "Generate an action for a state."
  [anon-fn]
  (let [fn-name (gensym "n-")]
    `(def ~fn-name ~anon-fn)))


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


(defn sc-zip
  "Create a zipper to navigate & manipulate statecharts. Wraps top level state collection in a root state."
  [root]
  (z/zipper
    map?
    (fn [state] (:children state))
    (fn [state children]
      (assoc state :children children))
    {:children root}))


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

