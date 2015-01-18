package com.dorr.persistent;

import java.util.*;

/**
 * Very simple implementation of a persistent singly-linked list.
 */
public class LinkedList<T> extends AbstractSequentialList<T> implements PersistentList<T> {
    private final T mHead;
    private final LinkedList<T> mTail;
    private final int mSize;

    private LinkedList(T head, LinkedList<T> tail) {
        mHead = head;
        mTail = tail;
        mSize = (head == null ? 0 : 1 + tail.size());
    }

    // *** Factories ***

    public LinkedList() {
        this(null, null);
    }
    public LinkedList(Collection<? extends T> c) {
        LinkedList<T> linkedList;
        if (c instanceof LinkedList) {
            // O(1) copy - we can just view the same data
            linkedList = (LinkedList<T>) c;
        } else {
            // O(n) copy, also requiring working memory (otherwise the 'obvious' implementation
            // would reverse the elements of c)
            linkedList = LinkedList.of((T[]) c.toArray());
        }
        mHead = linkedList.mHead;
        mTail = linkedList.mTail;
        mSize = linkedList.mSize;
    }
    public static final LinkedList<?> EMPTY = new LinkedList();
    public static <T> LinkedList<T> empty() { return (LinkedList<T>) EMPTY; }
    public static <T> LinkedList<T> of(T... elements) {
        LinkedList<T> head = empty();
        for (int i = elements.length - 1; 0 <= i; --i) {
            head = head.prepend(elements[i]);
        }
        return head;
    }

    // *** PersistentList ***

    @Override
    public LinkedList<T> prepend(T head) {
        return new LinkedList<T>(head, this);
    }

    @Override
    public T head() throws NoSuchElementException {
        if (mHead == null) {
            throw new NoSuchElementException("Head of an empty list");
        } else {
            return mHead;
        }
    }

    @Override
    public LinkedList<T> tail() {
        if (mTail == null) {
            throw new NoSuchElementException("Tail of an empty list");
        } else {
            return mTail;
        }
    }

    // *** AbstractList ***

    @Override
    public ListIterator<T> listIterator(final int startIndex) {
        if (!(0 <= startIndex && startIndex <= size())) {
            throw new IndexOutOfBoundsException("ListIterator starts out of the bounds of the list");
        }
        return new ListIterator<T>() {
            private int index = 0;
            private LinkedList<T> it = LinkedList.this;
            {
                for (int i = 0; i < startIndex; ++i) {
                    next();
                }
            }

            @Override
            public boolean hasNext() {
                return it.size() != 0;
            }
            @Override
            public int nextIndex() {
                return index;
            }
            @Override
            public T next() {
                T element = it.head();
                it = it.tail();
                ++index;
                return element;
            }

            // Backwards iteration - not implemented (pretend we're always at the beginning of the list)
            @Override
            public boolean hasPrevious() {
                return false;
            }
            @Override
            public int previousIndex() {
                return -1;
            }
            @Override
            public T previous() {
                throw new UnsupportedOperationException("Cannot iterate backwards through a singly-linked list");
            }

            // Mutable operations - not implemented
            @Override
            public void add(T t) {
                throw new UnsupportedOperationException("Mutable method called on immutable list");
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Mutable method called on immutable list");
            }
            @Override
            public void set(T t) {
                throw new UnsupportedOperationException("Mutable method called on immutable list");
            }
        };
    }

    @Override
    public int size() {
        return mSize;
    }
}
