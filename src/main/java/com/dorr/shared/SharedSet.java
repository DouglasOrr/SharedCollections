package com.dorr.shared;

import java.util.Set;

/**
 * A shared set that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface SharedSet<T> extends Set<T> {
    /**
     * Return a new map, with the given key->value mapping.
     * If there is already a value for the given key, it is replaced.
     * @param value the value to add, or replace
     * @return a new map with the key->value mapping (the existing map is unchanged),
     * (so <code>map.get(key) == value</code>).
     */
    SharedSet<T> with(T value);

    /**
     * Return a new set, without the given key.
     * @param value the value to remove
     * @return a new set without the given key (so <code>!set.contains(key)</code>)
     */
    SharedSet<T> without(T value);
}
