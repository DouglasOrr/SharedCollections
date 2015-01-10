package com.dorr.persistent;

import java.util.Set;

/**
 * A persistent set that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentSet<T> extends Iterable<T> {
    /**
     * Does this set contain <code>value</code>?
     * @param value the value to test membership
     */
    boolean contains(T value);

    /**
     * Return a new map, with the given key->value mapping.
     * If there is already a value for the given key, it is replaced.
     * @param value the value to add, or replace
     * @return a new map with the key->value mapping (the existing map is unchanged),
     * (so <code>map.get(key) == value</code>).
     */
    PersistentSet<T> add(T value);

    /**
     * Return a new set, without the given key.
     * @param value the value to remove
     * @return a new set without the given key (so <code>!set.contains(key)</code>)
     */
    PersistentSet<T> remove(T value);

    /** How many unique keys are present in the set. */
    int size();

    /** Returns an unmodifiable set 'view' of this set. */
    Set<T> asSet();
}
