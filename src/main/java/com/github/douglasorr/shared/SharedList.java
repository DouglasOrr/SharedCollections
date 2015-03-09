package com.github.douglasorr.shared;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A list that supports shared updates in place of mutable ones.
 * Apart from this, it behaves as a normal Java immutable List.
 * <p>Instead of using <code>LinkedList.addFirst</code>, use {@link #prepend(Object)},
 * and instead of using <code>LinkedList.removeFirst</code>, use {@link #tail()}.
 * The accessor {@link #head()} is provided for convenience.</p>
 */
public interface SharedList<T> extends List<T> {
    /**
     * The first element from the list.
     * Equivalent to <code>list.get(0)</code>.
     * @throws java.util.NoSuchElementException if the list is empty.
     * @return the first element of the list
     */
    T head() throws NoSuchElementException;

    /**
     * The tail of the list - every element after the first.
     * @throws java.util.NoSuchElementException if the list is empty.
     * @return a new list containing every element in this list after the first
     * (the original list is unmodified)
     */
    SharedList<T> tail() throws NoSuchElementException;

    /**
     * Returns a new list, with <code>head</code> at the front of the list.
     * (The original list is unmodified.)
     * @param head the head of the returned list
     * @return a new list with <code>head</code> at the front, and the current list
     * following (the original list is unmodified), so <code>list.head() == head</code>.
     */
    SharedList<T> prepend(T head);
}
