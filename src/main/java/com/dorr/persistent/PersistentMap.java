package com.dorr.persistent;

import java.util.Map;

/**
 * A persistent map class that does not support in-place updates,
 * but supports updates with structural sharing.
 */
public interface PersistentMap<K,V> extends Iterable<Map.Entry<K, V>> {
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
    }

    V get(K key);

    PersistentMap<K,V> put(K key, V value);

    /** How many key-value pairings are present in the map. */
    int size();

    /** Returns an unmodifiable map 'view' of this map. */
    Map<K, V> asMap();
}
