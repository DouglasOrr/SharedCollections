package com.dorr.persistent;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import scala.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** Abstracts away the details of map implementations, for fair-ish comparison. */
public abstract class MapTester<K,V> extends Tester {
    public abstract V get(K key);
    public abstract void put(K key, V value);

    public static class PersistentMapTester<K,V> extends MapTester<K,V> {
        private final PersistentMap<K,V> mEmpty;
        private PersistentMap<K,V> mMap;
        public PersistentMapTester(PersistentMap<K,V> empty) {
            mEmpty = empty;
            mMap = empty;
        }
        @Override
        public V get(K key) {
            return mMap.get(key);
        }
        @Override
        public void put(K key, V value) {
            mMap = mMap.with(key, value);
        }
        @Override
        public void reset() {
            mMap = mEmpty;
        }
        @Override
        public String toString() {
            return "Doug.HashTrieMap";
        }
    }

    public static abstract class JavaMapTester<K,V> extends MapTester<K,V> {
        protected Map<K,V> mMap;
        { reset(); }
        @Override
        public V get(K key) {
            return mMap.get(key);
        }
        @Override
        public void put(K key, V value) {
            mMap.put(key, value);
        }
    }

    public static class JavaHashMapTester<K,V> extends JavaMapTester<K,V> {
        @Override
        public void reset() {
            mMap = new HashMap<K, V>();
        }
        @Override
        public String toString() {
            return "Java.HashMap";
        }
    }

    public static class JavaTreeMapTester<K,V> extends JavaMapTester<K,V> {
        @Override
        public void reset() {
            mMap = new TreeMap<K, V>();
        }
        @Override
        public String toString() {
            return "Java.TreeMap";
        }
    }

    public static class ClojureIPersistentMapTester<K,V> extends MapTester<K,V> {
        private final IPersistentMap mEmpty;
        private IPersistentMap mMap;

        public ClojureIPersistentMapTester(IPersistentMap empty) {
            mEmpty = empty;
            mMap = mEmpty;
        }

        @Override @SuppressWarnings("unchecked")
        public V get(K key) {
            return (V) mMap.valAt(key);
        }
        @Override
        public void put(K key, V value) {
            mMap = mMap.assoc(key, value);
        }
        @Override
        public void reset() {
            mMap = mEmpty;
        }
        @Override
        public String toString() {
            return "Clojure.HashMap";
        }
    }

    public static class ScalaImmutableMapTester<K,V> extends MapTester<K,V> {
        private final scala.collection.immutable.Map<K,V> mEmpty;
        private scala.collection.immutable.Map<K,V> mMap;
        public ScalaImmutableMapTester(scala.collection.immutable.Map<K,V> empty) {
            mEmpty = empty;
            mMap = empty;
        }
        @Override
        public V get(K key) {
            Option<V> opt = mMap.get(key);
            return opt.isDefined() ? opt.get() : null;
        }
        @Override
        public void put(K key, V value) {
            mMap = mMap.updated(key, value);
        }
        @Override
        public void reset() {
            mMap = mEmpty;
        }
        @Override
        public String toString() {
            return "Scala.HashMap";
        }
    }
}
