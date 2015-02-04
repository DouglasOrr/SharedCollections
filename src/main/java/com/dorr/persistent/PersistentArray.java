package com.dorr.persistent;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A persistent array that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentArray<T> extends List<T> {
    /**
     * Returns a new array with an updated value at <code>index</code>.
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
     * Returns a new array, without the last value.
     * (The original array is unmodified.)
     * @return a new array, without the last value.
     */
    PersistentArray<T> remend() throws NoSuchElementException;

    /**
     * Returns a new array containing the first <code>n</code> elements of this array.
     * @param n the number of leading elements to return from this array.
     * @return a new array containing the first <code>n</code> elements
     */
    PersistentArray<T> take(int n) throws IndexOutOfBoundsException;
}
