(defproject nerves "0.1.0-SNAPSHOT"
  :description "A Clojure Statechart implementation"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[io.aviso/pretty "0.1.21"]
            ]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [mvxcvi/puget "1.0.0"]
                 [io.aviso/pretty "0.1.21"]
                 [org.clojure/test.check "0.9.0"]
                 [com.velisco/herbert "0.7.0-alpha2"]

                 [com.github.klangfarbe/statechart "1.0"]
                 ]
  )
