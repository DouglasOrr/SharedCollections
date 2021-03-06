package com.github.douglasorr.shared;

import clojure.lang.IPersistentMap;
import scala.Option;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/** Abstracts away the details of map implementations, for fair-ish comparison. */
public abstract class MapTester<K,V> extends Tester {
    public abstract V get(K key);
    public abstract void put(K key, V value);
    public abstract Iterator<Map.Entry<K,V>> iterator();

    public static class SharedMapTester<K,V> extends MapTester<K,V> {
        private final SharedMap<K,V> mEmpty;
        private SharedMap<K,V> mMap;
        public SharedMapTester(SharedMap<K, V> empty) {
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
        public Iterator<Map.Entry<K, V>> iterator() {
            return mMap.entrySet().iterator();
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
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return mMap.entrySet().iterator();
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
        public Iterator<Map.Entry<K, V>> iterator() {
            return mMap.iterator();
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
        public Iterator<Map.Entry<K, V>> iterator() {
            throw new UnsupportedOperationException();
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
