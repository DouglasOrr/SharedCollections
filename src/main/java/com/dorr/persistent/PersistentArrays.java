package com.dorr.persistent;

import java.util.ListIterator;

public class PersistentArrays {
    /**
     * Returns a new array, with <code>value</code> at <code>index</code>.
     * Elements after <code>index</code> are shifted forward by one place to make room.
     * (The original array is unmodified.)
     * @param array the source array
     * @param index the location to insert the new value
     * @param value value to place at index
     * @return a new array, with <code>value</code> at <code>index</code>
     */
    public static <T> PersistentArray<T> insert(PersistentArray<T> array, int index, T value) {
        // take the head
        PersistentArray<T> result = array.take(index);
        // append the value to be inserted
        result = result.append(value);
        // append any tail elements one-by-one
        ListIterator<T> tail = array.listIterator(index);
        while (tail.hasNext()) {
            result = result.append(tail.next());
        }
        return result;
    }

    /**
     * Returns a new array, without the element that was at <code>index</code>.
     * (The original array is unmodified.)
     * Elements after <code>index</code> are shifted backward by one place to make room.
     * @param array the source array
     * @param index the location to erase a value
     * @return a new array, with the previous value of <code>index</code> removed.
     */
    public static <T> PersistentArray<T> erase(PersistentArray<T> array, int index) {
        // take the head
        PersistentArray<T> result = array.take(index);
        // append any tail elements one-by-one
        ListIterator<T> tail = array.listIterator(index + 1);
        while (tail.hasNext()) {
            result = result.append(tail.next());
        }
        return result;
    }
}
