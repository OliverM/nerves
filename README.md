# nerves
> It is impossible to say just what I mean!   
> But as if a magic lantern threw the nerves in patterns on a screen...  
>  
> -- T. S. Eliot, [Lovesong of J. Alfred Prufrock](http://genius.com/Ts-eliot-the-love-song-of-j-alfred-prufrock-annotated/)

A Clojure Statechart implementation, based on David Harel's [Statecharts](http://www.inf.ed.ac.uk/teaching/courses/seoc/2005_2006/resources/statecharts.pdf) formalism, as discussed in Ian Horrocks' _Constructing the User Interface with Statecharts_.

Currently the implementation consists entirely of a clojure wrapper of a Java statechart implementation, [UML Statechart Framwork](https://github.com/klangfarbe/UML-Statechart-Framework-for-Java). It can be used to specify statecharts usable on the JVM. Almost the entirety of USF has been covered, except for History and internal transitions (at the time of writing). A clojure representation of the example on the USF homepage is available in the USF test namespace.

Ultimately the goal is to produce a clojure-native implementation that can be run both on the JVM and compiled to JS via Clojurescript.

## Usage

Currently the only examples are in the USF-test namespace. The library has been tested in production.

## License

Copyright © 2016 Oliver Mooney

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
