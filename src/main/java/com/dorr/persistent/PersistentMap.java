package com.dorr.persistent;

import java.util.Map;

/**
 * A persistent map class that does not support in-place updates,
 * but does support updates with structural sharing.
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

    PersistentMap<K,V> put(K key, V value);

    V get(K key);

    int size();

    Map<K, V> asMap();
}
