package com.dorr.persistent;

import java.util.*;

/**
 * Very simple implementation of a persistent singly-linked list.
 */
public abstract class LinkedList<T> extends AbstractSequentialList<T> implements PersistentList<T> {
    /**
     * Implements the empty list (contains no elements, calls to head()/tail() disallowed).
     */
    public static final class Empty<T> extends LinkedList<T> {
        @Override
        public int size() {
            return 0;
        }

        // Not allowed to call these on the Empty list
        @Override
        public T head() {
            throw new NoSuchElementException("Head of an empty list");
        }
        @Override
        public LinkedList<T> tail() {
            throw new NoSuchElementException("Tail of an empty lsit");
        }
    }

    /**
     * Implements a prepended element onto the front of a LinkedList.
     */
    public static class Cons<T> extends LinkedList<T> {
        private final T mHead;
        private final LinkedList<T> mTail;
        private final int mSize;

        public Cons(T head, LinkedList<T> tail) {
            mHead = head;
            mTail = tail;
            mSize = tail.size() + 1;
        }

        @Override
        public int size() {
            return mSize;
        }

        @Override
        public T head() {
            return mHead;
        }

        @Override
        public LinkedList<T> tail() {
            return mTail;
        }
    }

    // Factories
    public static final LinkedList<?> EMPTY = new Empty();
    public static <T> LinkedList<T> empty() { return (LinkedList<T>) EMPTY; }
    public static <T> LinkedList<T> of(T... elements) {
        LinkedList<T> head = empty();
        for (int i = elements.length - 1; 0 <= i; --i) {
            head = head.after(elements[i]);
        }
        return head;
    }

    // PersistentList implementation

    @Override
    public LinkedList<T> after(T head) {
        return new Cons<T>(head, this);
    }

    @Override
    public abstract LinkedList<T> tail();

    // java.util.List implementation

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
}
