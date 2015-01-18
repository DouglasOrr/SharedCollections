package com.dorr.persistent;

import java.util.List;

/**
 * A persistent array that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentArray<T> extends List<T> {
    /**
     * Return a new array with an updated value at a single index.
     * (The original array is unmodified.)
     * @param index the index in the array to update
     * @param value new value to set
     * @return a new array, with
     */
    PersistentArray<T> update(int index, T value) throws IndexOutOfBoundsException;

    /**
     * Returns a new array, with <code>value</code> added on to the end.
     * (The original array is unmodified.)
     * @param value the value at the end of the returned array.
     * @return a new array, with <code>value</code> at the end
     */
    PersistentArray<T> append(T value);
}
