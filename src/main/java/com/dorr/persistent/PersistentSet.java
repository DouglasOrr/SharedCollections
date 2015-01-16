package com.dorr.persistent;

import java.util.Set;

/**
 * A persistent set that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentSet<T> extends Set<T> {
    /**
     * Return a new map, with the given key->value mapping.
     * If there is already a value for the given key, it is replaced.
     * @param value the value to add, or replace
     * @return a new map with the key->value mapping (the existing map is unchanged),
     * (so <code>map.get(key) == value</code>).
     */
    PersistentSet<T> with(T value);

    /**
     * Return a new set, without the given key.
     * @param value the value to remove
     * @return a new set without the given key (so <code>!set.contains(key)</code>)
     */
    PersistentSet<T> without(T value);
}
