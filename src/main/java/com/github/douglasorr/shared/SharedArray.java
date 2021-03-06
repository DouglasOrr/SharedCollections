package com.github.douglasorr.shared;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An array that supports shared updates in place of mutable ones.
 * Apart from this, it behaves as a normal Java immutable List.
 * <p>Instead of using <code>List.add</code>, use {@link #append(Object)},
 * instead of using <code>List.remove(n-1)</code>, use {@link #remend()},
 * and instead of using <code>List.set</code>, use {@link #update(int, Object)}.</p>
 * <p>Best suited to sequential containers where random reads and insertion
 * at the end is required.
 * Implementations offer efficient insertion at end, updates, and indexing,
 * as well as an efficient operation for selecting a prefix sub-list.</p>
 * <p>See {@link com.github.douglasorr.shared.SharedArrays} for some less efficient,
 * but potentially useful operations, based on this API.</p>
 */
public interface SharedArray<T> extends List<T> {
    /**
     * Returns a new array with an updated value at <code>index</code>.
     * (The original array is unmodified.)
     * @param index the index in the array to update
     * @param value new value to set
     * @return a new array, with <code>index</code> updated to contain <code>value</code>
     */
    SharedArray<T> update(int index, T value) throws IndexOutOfBoundsException;

    /**
     * Returns a new array, with <code>value</code> added on to the end.
     * (The original array is unmodified.)
     * <p>If you want to append multiple values, consider using {@link #appendAll(java.util.Collection)},
     * as this can be more efficient.</p>
     * @param value the value at the end of the returned array.
     * @return a new array, with <code>value</code> at the end
     */
    SharedArray<T> append(T value);

    /**
     * Returns a new array, with all elements of <code>values</code> added on to the
     * end, in iteration order.
     * (The original array is unmodified.)
     * @param values the values at the end of the returned array
     * @return a new array with <code>values</code> at the end
     */
    SharedArray<T> appendAll(Collection<T> values);

    /**
     * Returns a new array, without the last value.
     * (The original array is unmodified.)
     * <p>If you want to remove multiple values, consider using {@link #take(int)},
     * as this can be more efficient.</p>
     * @return a new array, without the last value.
     */
    SharedArray<T> remend() throws NoSuchElementException;

    /**
     * Returns a new array containing the first <code>n</code> elements of this array.
     * @param n the number of leading elements to return from this array.
     * @return a new array containing the first <code>n</code> elements
     */
    SharedArray<T> take(int n) throws IndexOutOfBoundsException;
}
