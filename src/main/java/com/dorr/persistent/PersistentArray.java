package com.dorr.persistent;

import java.util.List;

/**
 * A persistent array that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentArray<T> extends List<T> {
    /**
     * Return a new array with an updated value at <code>index</code>.
     * (The original array is unmodified.)
     * @param index the index in the array to update
     * @param value new value to set
     * @return a new array, with <code>index</code> updated to contain <code>value</code>
     */
    PersistentArray<T> update(int index, T value) throws IndexOutOfBoundsException;

    /**
     * Returns a new array, with <code>value</code> added on to the end.
     * (The original array is unmodified.)
     * @param value the value at the end of the returned array.
     * @return a new array, with <code>value</code> at the end
     */
    PersistentArray<T> append(T value);

    /**
     * Returns a new array, with <code>value</code> at <code>index</code>.
     * Elements after <code>index</code> are shifted forward by one place to make room.
     * (The original array is unmodified.)
     * @param index the location to insert the new value
     * @param value value to place at index
     * @return a new array, with <code>value</code> at <code>index</code>
     */
    PersistentArray<T> insert(int index, T value) throws IndexOutOfBoundsException;

    /**
     * Erase the element at <code>index</code>.
     * Elements after <code>index</code> are shifted backward by one place to make room.
     * @param index the location to erase a value
     * @return a new array, with the previous value of <code>index</code> removed.
     * @throws IndexOutOfBoundsException
     */
    PersistentArray<T> erase(int index) throws IndexOutOfBoundsException;
}
