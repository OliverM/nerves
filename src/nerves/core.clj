(ns nerves.core
  (:require [clojure.walk :refer [walk]]
            [clojure.set :refer [map-invert]]
            [clojure.zip :as z]
            [clojure.core.match :refer [match]]
            [aprint.core :refer [aprint ap]]
            [nerves.samples :as ns]))

;; Utility zipper functions to navigate a zipper via a path, based on version by Meikel Brandmeyer
;; via google group discussion here: https://groups.google.com/forum/#!topic/clojure/v9ZTnsNnqVs
(defn- walk-along-and-do
"Follow along the path from the given loc. When the path is contained
invoke the found-action. Otherwise invoke the not-found-action with
the loc of the last contained node as well as the not contained path
components. pred is used to identify the nodes."
[loc pred p found-action not-found-action]
; Get rid of special case: the empty path. This makes pc in
; the loop always non-nil.
(if-let [p (seq p)]
          (loop [loc              loc
                 [pc & pcs :as p] p]
            (let [is-equal (pred (z/node loc) pc)]
              (cond
                ; No match? Try the next node.
                (and (not is-equal)
                     (z/right loc)) (recur (z/right loc) p)

                ; No match!
                ; XXX: Report the parent and the original path!
                (not is-equal)        (not-found-action (z/up loc) p)

                ; XXX: From here on the node matches!
                ; In case we have nothing left in the path, we
                ; found the target node.
                (nil? pcs)            (found-action loc)

                ; For branch go on for the children and the rest of
                ; the path's components.
                (and (z/branch? loc)
                     (z/down loc))  (recur (z/down loc) pcs)

                ; In any other case the path is not contained in the tree.
                :else                 (not-found-action loc pcs))))
          (found-action loc)))

(defn- walk-along
  "Follow along the path from the given loc. In case the path is not
  contained in the zipper, an exception is thrown. The empty path is
  always contained and leaves the loc as is. pred is used to identify
  the nodes."
  [loc pred p]
  (walk-along-and-do loc pred p identity
                     (fn [_ _] (throw (new Exception "path not in tree")))))

(defn- try-walk-along
  "Try to follow along the path from the given loc. In case the path
  is not contained in the zipper, the last contained location and the
  rest of the path are returned in a vector. The empty path is always
  contained and leaves the loc as is. pred is used to identify the
  nodes."
  [loc pred p]
  (walk-along-and-do loc pred p (fn [loc] [loc nil])
                     (fn [loc rpath] [loc rpath])))

(defn lca-path
  "Traverse the zipper between the supplied two locations via their lowest common ancestor."
  [sczip start-path end-path]
  (let [path-filter (set start-path)
        lca-path (filter path-filter end-path)
        dofn (fn [loc] (println "At node: " (z/node loc)))]
    ;; descend to lca
    (walk-along-and-do sczip identical? lca-path dofn (throw (new Exception "LCA path not in tree")))

    ;; descend to start point from lca
    (walk-along-and-do sczip identical? (filter (complement path-filter) end-path) dofn (throw (new Exception "Start path not in tree")))

    ;; descend to end point from lca
    (walk-along-and-do sczip identical? (filter (complement (set end-path)) start-path) dofn (throw (new Exception "End path not in tree")))
    ))

;; current test strategy for above...
(def test-zip [0 [1] [2 [3 [4] 5 [6 [7] [8]]]]])
(def four-path (-> (z/vector-zip test-zip)
                   z/down
                   z/right
                   z/right
                   z/down
                   z/right
                   z/down
                   z/right
                   z/down
                   z/path))
(def seven-path (-> (z/vector-zip test-zip)
                    z/down
                    z/right
                    z/right
                    z/down
                    z/right
                    z/down
                    z/right
                    z/right
                    z/right
                    z/down
                    z/right
                    z/down
                    z/path))
(walk-along (z/vector-zip test-zip) identical? (filter (set four-path) seven-path))
(walk-along-and-do (z/vector-zip test-zip) identical? (filter (set four-path) seven-path) z/node "Error")





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

;; testing LCA routing for statecharts...
(def c-posn (-> (sc-zip ns/nested-statechart)
                z/down
                z/down
                z/path))
(def b-posn (-> (sc-zip ns/nested-statechart)
                z/down
                z/right
                z/path))
(walk-along (sc-zip ns/nested-statechart) identical? (filter (set c-posn) b-posn))


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

(defn- sc->id-sc
  "Assign each state a unique id and return a pair of the id-annotated statechart and the dictionary
  translating ids to names. Assigns the id as the key to the dictionary in cases of states without
  names (e.g. anonymous nesting states) and the implicit root state."
  [statechart]
  (loop [loc (sc-zip statechart)
         counter 1
         id-dict {}]
    (if (z/end? loc)
      [(z/root loc) id-dict]
      (recur (z/next (z/edit loc assoc :id counter))
             (inc counter)
             (assoc id-dict (or (:name (z/node loc)) (str counter)) counter)))))

(defn- merge-eats
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

