package com.github.douglasorr.shared;

import java.util.Map;

/**
 * A map that supports shared updates in place of mutable ones.
 * Apart from this, it behaves as a normal Java immutable Map.
 * <p>Instead of using <code>Map.put</code>, use {@link #with(Object,Object)}, and
 * instead of using <code>Map.remove</code>, use {@link #without(Object)}.</p>
 */
public interface SharedMap<K,V> extends Map<K, V> {
    /**
     * Return a new map, with the given key-&gt;value mapping.
     * <p>If there is already a value for the given key, it is replaced.</p>
     * @param key the key to insert (must not be <code>null</code>)
     * @param value the value to add, or replace (must not be <code>null</code>)
     * @return a new map with the key-&gt;value mapping (the original map is unchanged),
     * so <code>map.get(key) == value</code>.
     */
    SharedMap<K, V> with(K key, V value);

    /**
     * Return a new map, without the given key (and the value that it previously
     * mapped to, if any).
     * @param key the key to remove
     * @return a new map without the given key (the original map is unchanged),
     * so <code>map.get(key) == null</code>.
     */
    SharedMap<K,V> without(K key);
}
