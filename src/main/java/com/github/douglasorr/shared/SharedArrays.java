package com.github.douglasorr.shared;

/**
 * Utility methods for working with {@link SharedArray} values.
 * These algorithms are provided here with an efficiency warning - while all the direct
 * SharedArray operations are log(N) or better, these methods can have O(N log(N))
 * worst case computational complexity.
 */
public final class SharedArrays {
    private SharedArrays() { }
    /**
     * Returns a new array, with <code>value</code> at <code>index</code>.
     * Elements after <code>index</code> are shifted forward by one place to make room.
     * (The original array is unmodified.)
     * @param array the source array
     * @param index the location to insert the new value
     * @param value value to place at index
     * @return a new array, with <code>value</code> at <code>index</code>
     */
    public static <T> SharedArray<T> insert(SharedArray<T> array, int index, T value) {
        return array.take(index)
                    .append(value)
                    .appendAll(array.subList(index, array.size()));
    }

    /**
     * Returns a new array, without the element that was at <code>index</code>.
     * Elements after <code>index</code> are shifted backward by one place to make room.
     * (The original array is unmodified.)
     * @param array the source array
     * @param index the location to remove a value
     * @return a new array, with the previous value of <code>index</code> removed.
     */
    public static <T> SharedArray<T> remove(SharedArray<T> array, int index) {
        return array.take(index)
                    .appendAll(array.subList(index + 1, array.size()));
    }
}
