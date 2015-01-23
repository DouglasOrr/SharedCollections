package com.dorr.persistent;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.AbstractList;
import java.util.Arrays;

/**
 * An implementation of the persistent trie array.
 */
public class TrieArray<T> extends AbstractList<T> implements PersistentArray<T> {
    private final Object mRoot;
    private final int mSize;
    private static final int NBITS = 5;
    private static final int MASK = (1 << NBITS) - 1;

    private int height() {
        return mSize <= 1 ? 0 : 1 + (31 - Integer.numberOfLeadingZeros(mSize - 1)) / NBITS;
    }

    private TrieArray(Object root, int size) {
        mRoot = root;
        mSize = size;
    }

    // *** Factories ***

    public static final TrieArray EMPTY = new TrieArray(null, 0);

    public static <T> TrieArray<T> empty() {
        return EMPTY;
    }
    public static <T> TrieArray<T> singleton(T value) {
        return new TrieArray<T>(value, 1);
    }

    // *** AbstractList ***

    @Override
    public int size() {
        return mSize;
    }

    // *** PersistentArray ***

    @Override
    public T get(int index) {
        if (index < 0 || mSize <= index) {
            throw new IndexOutOfBoundsException(index + " (size " + mSize + ")");
        }
        Object current = mRoot;
        for (int level = height() - 1; 0 <= level; --level) {
            int childIndex = (index >>> (NBITS * level)) & MASK;
            current = ((Object[]) current)[childIndex];
        }
        return (T) current;
    }

    @Override
    public TrieArray<T> update(int index, T value) throws IndexOutOfBoundsException {
        if (index < 0 || mSize <= index) {
            throw new IndexOutOfBoundsException(index + " (size " + mSize + ")");
        }
        if (mSize == 1) {
            // Object is being stored in mRoot
            return singleton(value);
        }
        return new TrieArray<T>(assign(null, value, index), mSize);
    }

    @Override
    public TrieArray<T> append(T value) {
        if (mSize == 0) {
            // store the value in mRoot
            return singleton(value);
        }
        final int newSize = mSize + 1;

        // we may need to grow upwards
        if ((1 << (NBITS * height())) < newSize) {
            return new TrieArray<T>(assign(new Object[] { mRoot, null }, value, mSize), newSize);
        } else {
            return new TrieArray<T>(assign(null, value, mSize), newSize);
        }
    }

    @Override
    public PersistentArray<T> insert(int index, T value) throws IndexOutOfBoundsException {
        throw new NotImplementedException();
    }

    @Override
    public PersistentArray<T> erase(int index) throws IndexOutOfBoundsException {
        throw new NotImplementedException();
    }

    private Object assign(Object[] newRoot, T value, int index) {
        Object[] current;
        Object[] parent = newRoot;
        int parentIndex;

        if (newRoot == null) {
            // searching through the tree
            current = (Object[]) mRoot;
            parentIndex = -1;

        } else {
            // growing the tree upwards
            parentIndex = parent.length - 1;
            current = null;
        }

        // build a new slice of tree, copying from the old
        for (int level = height() - 1; 0 <= level; --level) {
            int childIndex = (index >>> (NBITS * level)) & MASK;
            Object[] copy;
            if (current == null) {
                copy = new Object[] { null };

            } else {
                copy = Arrays.copyOf(current, Math.max(current.length, childIndex + 1));
                current = (level != 0 && childIndex < current.length)
                        ? (Object[]) current[childIndex]
                        : null;
            }

            // patch up the new trie
            if (newRoot == null) { newRoot = copy; }
            else { parent[parentIndex] = copy; }
            parent = copy;
            parentIndex = childIndex;
        }

        // add our value at the bottom of the trie
        parent[parentIndex] = value;

        return newRoot;
    }
}
