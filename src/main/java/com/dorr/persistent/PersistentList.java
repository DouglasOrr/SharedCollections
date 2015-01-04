package com.dorr.persistent;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A persistent list that does not support in-place updates,
 * but supports 'copy' updates with structural sharing.
 */
public interface PersistentList<T> extends List<T> {
    /** The first element from the list. */
    T head() throws NoSuchElementException;

    /** The tail of the list - every element after the first. */
    PersistentList<T> tail() throws NoSuchElementException;

    /** A new list, with <code>head</code> at the front of the list. */
    PersistentList<T> prepend(T head);
}
