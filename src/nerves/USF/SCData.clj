(ns nerves.USF.SCData
  "Extend the USF Metadata class to allow for adding state in a more idiomatic Clojure manner. Attaches an atom to each
  instantiated USF statechart."
  (:gen-class
    :extends com.github.klangfarbe.statechart.Metadata
    :state state
    :prefix "scd-"
    :init init
    :constructors {[] []}
    :methods [[setData [Object] void]
              [data [] Object]]))

(defn scd-init [] [[] (atom nil)])
(defn scd-setData [this data] (reset! (.state this) data))
(defn scd-data [this] @(.state this))
