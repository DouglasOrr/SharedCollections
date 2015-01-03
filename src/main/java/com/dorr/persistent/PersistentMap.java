package com.dorr.persistent;

import java.util.*;

/**
 * A persistent map class that does not support in-place updates,
 * but supports updates with structural sharing.
 */
public interface PersistentMap<K,V> extends Iterable<Map.Entry<K, V>> {
    @Deprecated
    class Entry<K, V> implements Map.Entry<K,V> {
        private final K mKey;
        private final V mValue;

        public Entry(K key, V value) {
            this.mKey = key;
            this.mValue = value;
        }
        @Override
        public K getKey() { return mKey; }
        @Override
        public V getValue() { return mValue; }
        @Override
        public V setValue(V object) {
            throw new UnsupportedOperationException("setValue() mutator not possible for an immutable PersistentMap.Entry");
        }
        @Override
        public String toString() {
            return mKey + "=" + mValue;
        }
    }

    /**
     * Retreive the value associated with the given key.
     * @param key the key to look up
     */
    V get(K key);

    /**
     * Return a new map, with the given key->value mapping.
     * If there is already a value for the given key, it is replaced.
     * @param key the key to insert
     * @param value the value to add, or replace
     * @return a new map with the key->value mapping (the existing map is unchanged),
     * (so <code>map.get(key) == value</code>).
     */
    PersistentMap<K,V> put(K key, V value);

    /**
     * Return a new map, without the given key.
     * @param key the key to remove
     * @return a new map without the given key (so <code>map.get(key) == null</code>)
     */
    PersistentMap<K,V> remove(K key);

    /** How many key-value pairings are present in the map. */
    int size();

    /** Returns an unmodifiable map 'view' of this map. */
    Map<K, V> asMap();
}
