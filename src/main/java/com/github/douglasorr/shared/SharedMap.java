package com.github.douglasorr.shared;

import java.util.Map;

/**
 * A shared map that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface SharedMap<K,V> extends Map<K, V> {
    /**
     * Return a new map, with the given key-&gt;value mapping.
     * If there is already a value for the given key, it is replaced.
     * @param key the key to insert
     * @param value the value to add, or replace
     * @return a new map with the key-&gt;value mapping (the existing map is unchanged),
     * (so <code>map.get(key) == value</code>).
     */
    SharedMap<K, V> with(K key, V value);

    /**
     * Return a new map, without the given key.
     * @param key the key to remove
     * @return a new map without the given key (so <code>map.get(key) == null</code>)
     */
    SharedMap<K,V> without(K key);
}
