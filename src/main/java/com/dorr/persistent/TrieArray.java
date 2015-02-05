package com.dorr.persistent;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * An implementation of the persistent trie array.
 */
public class TrieArray<T> extends AbstractList<T> implements PersistentArray<T> {
    private static final int NBITS = 5;
    private static final int BLOCK_SIZE = (1 << NBITS);
    private static final int MASK = BLOCK_SIZE - 1;

    private final Object[] mRoot;
    private final Object mEnd;
    private final int mSize;

    private TrieArray(Object[] root, Object end, int size) {
        mRoot = root;
        mEnd = end;
        mSize = size;
    }

    // *** Factories ***

    public TrieArray() {
        this(null, null, 0);
    }
    public TrieArray(Collection<T> c) {
        TrieArray<T> a = empty();
        for (T element : c) {
            a = a.append(element);
        }
        mRoot = a.mRoot;
        mEnd = a.mEnd;
        mSize = a.mSize;
    }

    public static final TrieArray EMPTY = new TrieArray();
    public static <T> TrieArray<T> empty() {
        return EMPTY;
    }
    public static <T> TrieArray<T> singleton(T value) {
        return new TrieArray<T>(null, value, 1);
    }
    public static <T> TrieArray<T> of(T... values) {
        return new TrieArray<T>(Arrays.asList(values));
    }

    // *** AbstractList ***

    @Override
    public int size() {
        return mSize;
    }

    // *** PersistentArray ***

    private static int rootSize(int size) {
        return size <= 1 ? 0 : size - (1 + (size - 1) % BLOCK_SIZE);
    }
    private static int height(int size) {
        int rootSize = rootSize(size);
        return rootSize <= 1
                ? 0
                : 1 + (31 - Integer.numberOfLeadingZeros(rootSize - 1)) / NBITS;
    }

    /**
     * Find the node (non-terminal, or terminal) at <code>index</code> in level
     * <code>targetLevel</code>.
     * @param root node to start from
     * @param rootLevel level to start from
     * @param targetIndex the index to search for
     * @param targetLevel what level in the tree to return (i.e. for a terminal, pass targetLevel=0)
     * @return the terminal (if targetLevel == 0) or nonterminal (if targetLevel != 0)
     */
    private static Object findNode(Object root, int rootLevel, int targetIndex, int targetLevel) {
        Object current = root;
        for (int level = rootLevel; targetLevel <= level; --level) {
            int childIndex = (targetIndex >>> (NBITS * level)) & MASK;
            current = ((Object[]) current)[childIndex];
        }
        return current;
    }

    private static Object shrinkEnd(Object[] end, int n) {
        if (n == 1) {
            return end[0];
        } else if (n == end.length) {
            return end;
        } else {
            return Arrays.copyOf(end, n);
        }
    }

    @Override
    public T get(int index) {
        if (index < 0 || mSize <= index) {
            throw new IndexOutOfBoundsException(index + " (size " + mSize + ")");
        }

        int rootSize = rootSize(mSize);
        if (index < rootSize) {
            return (T) findNode(mRoot, height(mSize) - 1, index, 0);

        } else if (mSize - rootSize == 1) {
            return (T) mEnd;

        } else {
            return (T) ((Object[]) mEnd)[index - rootSize];
        }
    }

    @Override
    public TrieArray<T> update(int index, T value) throws IndexOutOfBoundsException {
        if (index < 0 || mSize <= index) {
            throw new IndexOutOfBoundsException(index + " (size " + mSize + ")");
        }

        int rootSize = rootSize(mSize);
        if (index < rootSize) {
            Object[] newRoot = null;
            Object[] parent = null;
            int parentIndex = -1;
            Object[] current = mRoot;

            // build a new slice of the tree, copying from the old
            for (int level = height(mSize) - 1; 0 <= level; --level) {
                // patch up the new trie
                final Object[] copy = Arrays.copyOf(current, current.length);
                if (newRoot == null) { newRoot = copy; }
                else { parent[parentIndex] = copy; }

                final int childIndex = (index >>> (NBITS * level)) & MASK;
                if (level == 0) {
                    // add our value at the bottom of the trie
                    copy[childIndex] = value;

                } else {
                    // keep on going
                    parent = copy;
                    parentIndex = childIndex;
                    current = (Object[]) current[childIndex];
                }
            }
            return new TrieArray<T>(newRoot, mEnd, mSize);

        } else if (index - rootSize == 1) {
            return new TrieArray<T>(mRoot, value, mSize);

        } else {
            Object[] endArray = (Object[]) mEnd;
            Object[] newEnd = Arrays.copyOf(endArray, endArray.length);
            newEnd[index - rootSize] = value;
            return new TrieArray<T>(mRoot, newEnd, mSize);
        }
    }

    @Override
    public TrieArray<T> append(T value) {
        final int rootSize = rootSize(mSize);

        if (mSize == 0) {
            // create a singleton
            return singleton(value);

        } else if (mSize == BLOCK_SIZE) {
            // create a 1-element 'end'
            return new TrieArray<T>((Object[]) mEnd, value, mSize + 1);

        } else if (mSize - rootSize == 1) {
            // split a 1-element 'end'
            return new TrieArray<T>(mRoot, new Object[]{mEnd, value}, mSize + 1);

        } else if (mSize - rootSize < BLOCK_SIZE) {
            // extend the 'end' array
            Object[] endArray = (Object[]) mEnd;
            Object[] end = Arrays.copyOf(endArray, endArray.length + 1);
            end[end.length - 1] = value;
            return new TrieArray<T>(mRoot, end, mSize + 1);

        } else {
            // insert 'end'

            Object[] newRoot = null;
            Object[] parent = null;
            int parentIndex = -1;
            Object[] current = mRoot;
            if (height(mSize) < height(mSize + 1)) {
                newRoot = parent = new Object[] { mRoot, null };
                parentIndex = 1;
                current = null;
            }

            // build a new slice of tree, copying from the old
            for (int level = height(mSize) - 1; 1 <= level; --level) {
                final int childIndex = (rootSize >>> (NBITS * level)) & MASK;

                // patch up the new trie
                final Object[] copy = (current == null)
                        ? new Object[] { null }
                        : Arrays.copyOf(current, childIndex + 1);
                if (newRoot == null) { newRoot = copy; }
                else { parent[parentIndex] = copy; }
                parent = copy;
                parentIndex = childIndex;

                if (level != 1) {
                    current = (current != null && childIndex < current.length)
                            ? (Object[]) current[childIndex]
                            : null;
                }
            }
            parent[parentIndex] = mEnd;
            return new TrieArray<T>(newRoot, value, mSize + 1);
        }
    }

    @Override
    public TrieArray<T> remend() {
        return take(mSize - 1);
    }

    @Override
    public TrieArray<T> take(int n) throws IndexOutOfBoundsException {
        final int rootSize = rootSize(mSize);

        if (!(0 <= n && n <= mSize)) {
            throw new IndexOutOfBoundsException(String.format("Trying to take(%d) from a %d-element array", n, mSize));

        } else if (n == mSize) {
            return this;

        } else if (n == 0) {
            return empty();

        } else if (1 <= n - rootSize) {
            // just shrink the 'end' array
            return new TrieArray<T>(mRoot, shrinkEnd((Object[]) mEnd, n - rootSize), n);

        } else {
            // we have run out of capacity in mEnd, so we need to get a new root, and end

            final int oldHeight = height(mSize);
            final int newRootSize = rootSize(n);
            final int newHeight = height(n);

            // build a new slice of tree, copying from the old
            Object[] newRoot = null;
            if (newRootSize != 0) {
                Object[] parent = null;
                int parentIndex = -1;
                Object[] current = mRoot;
                for (int level = oldHeight - 1; 0 <= level; --level) {
                    final int childIndex = ((newRootSize - 1) >>> (NBITS * level)) & MASK;
                    if (level <= newHeight - 1) {
                        // patch up the new trie
                        final Object[] copy = Arrays.copyOf(current, childIndex + 1);
                        if (newRoot == null) {
                            newRoot = copy;
                        } else {
                            parent[parentIndex] = copy;
                        }
                        parent = copy;
                        parentIndex = childIndex;
                    }
                    if (0 < level) {
                        current = (Object[]) current[childIndex];
                    }
                }
            }

            // find the new end node, and resize it as necessary
            Object newEnd = shrinkEnd((Object[]) findNode(mRoot, oldHeight - 1, newRootSize, 1), n - newRootSize);

            return new TrieArray<T>(newRoot, newEnd, n);
        }
    }
}
