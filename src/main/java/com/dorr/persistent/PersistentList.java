package com.dorr.persistent;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A persistent list that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentList<T> extends List<T> {
    /**
     * The first element from the list.
     * Equivalent to <code>list.get(0)</code>.
     * @throws java.util.NoSuchElementException if the list is empty.
     */
    T head() throws NoSuchElementException;

    /**
     * The tail of the list - every element after the first.
     * @throws java.util.NoSuchElementException if the list is empty.
     */
    PersistentList<T> tail() throws NoSuchElementException;

    /**
     * Returns a new list, with <code>head</code> at the front of the list.
     * (The original list is unmodified.)
     * @param head the head of the returned list.
     */
    PersistentList<T> prepend(T head);
}
