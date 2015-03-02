# dorr.shared
[![Build Status](https://travis-ci.org/DouglasOrr/SharedCollections.svg?branch=master)](https://travis-ci.org/DouglasOrr/SharedCollections)

A very small immutable (structural sharing) data structures library (based on Clojure & Scala's associative and sequential data structures), for use in plain old Java.

## Usage



## Development

The aim of the project is to be usable in any Java project, with minimum fuss, to make it easier to develop safe, simple, programs for various purposes.
As such, the library seeks to:

 - provide well-tested correct & high-performing implementations
 - keep APIs relatively stable
 - have no additional dependencies

If you have improvements that are in line with these goals, or don't conflict, I'd be very happy to receive a pull request.

### Releasing

Currently ([@DouglasOrr](https://github.com/DouglasOrr)) has the signing keys, which are required to release official versions of the library. However, here is the process.

 - we use [semantic versioning](http://semver.org/) - `MAJOR.MINOR.PATCH`, make sure the version number obeys these rules before releasing!
 - to release the current version, run `./gradlew release`
 - increment the version number in `build.gradle` as required

## Background

I was developing an application on Android with a asynchronous architecture (as Android apps generally should), making heavy use of RX Observables to pass data between the model being updated in the background and the UI thread. This architecture really benefits from immutable events, in which case, cross-thread modifications are not a concern, and it is easy for the model to publish 'snapshots' to the UI. Unfortunately achieving immutable snapshots in Java is a bit tricky, and can easily lead to performance problems as you are forced to create copies of mutable data structures (even Guava's excellent collections do not support the 'shared update' feature of updating to create a new collection, without modifying the original, except for some collection 'views').

So I felt the need for the sort of Vector, List and (most importantly) Map/Set that you find in Clojure/Scala, which make immutable snapshots efficient, however I don't want to have to pull in the rest of either of these universes when I'm writing my plain old Java code! Hence this library.

### Reference

 - [Phil Bagwell's original paper](http://lampwww.epfl.ch/papers/idealhashtrees.pdf) describing the Hash Array Mapped Trie
 - [Marek's helpful blog](https://idea.popcount.org/2012-07-25-introduction-to-hamt/) - an accessible introduction to the idea
