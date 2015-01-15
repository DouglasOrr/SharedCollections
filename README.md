# com.dorr.persistent
[![Build Status](https://travis-ci.org/DouglasOrr/Persistent.svg?branch=master)](https://travis-ci.org/DouglasOrr/Persistent)

A very small persistent data structures library (based on Clojure's map, vector, list & set), for use in plain old Java.

## Usage



## Reference

 - [Phil Bagwell's original paper](http://lampwww.epfl.ch/papers/idealhashtrees.pdf) describing the Hash Array Mapped Trie

## Development

The aim of the project is to be usable in any Java project, with minimum fuss, to make it easier to develop safe, simple, programs for various purposes.
As such, the library seeks to:

 - provide well-tested correct & performant implementations
 - keep APIs relatively stable
 - have no extra dependencies

 If you have improvements that are in line with these goals, or don't conflict, I'd be very happy to receive a pull request.

## Background

I was developing an application on Android with a asynchronous architecture (as Android apps generally should), making heavy use of RX Observables to pass data between the model being updated in the background and the UI thread. This architecture really benefits from immutable events, in which case, cross-thread modifications are not a concern, and it is easy for the model to publish 'snapshots' to the UI. Unfortunately achieving persistent snapshots in Java is a bit tricky, and can easily lead to performance problems as you are forced to create copies of mutable data structures (even Guava's excellent collections do not support the persistent data structure feature of updating to create a new collection, without modifying the original, except for some collection 'views'). So I felt the need for the sort of Vector, List and (most importantly Map/Set) that you find in Clojure/Scala, which make persistent snapshots efficient, however I don't want to have to pull in the rest of either of these universes when I'm writing my plain old Java code! Hence this library.
