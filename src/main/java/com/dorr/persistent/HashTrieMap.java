package com.dorr.persistent;

import java.util.*;

/**
 * An implementation of the persistent hash trie map.
 */
public class HashTrieMap<K,V> implements PersistentMap<K,V> {

    // TODO: Clojure's implementation saves memory by putting keys & values adjacent in the children array
    // (not using map entry pairs) - could we do this?
    private static class Node {
        // child :: Node | PersistentMap$Entry | PersistentMap$Entry[]
        public final Object[] children;
        public final int hasChild;
        private Node(Object[] children, int hasChild) {
            this.children = children;
            this.hasChild = hasChild;
            assert children != null;
            assert Integer.bitCount(hasChild) == children.length;
        }
    }

    // root :: Node | PersistentMap$Entry | PersistentMap$Entry[] | Null
    private final Object mRoot;
    private final int mSize;
    private static final int HASH_MASK = 0x001F;
    private static final int HASH_SHIFT = 5;

    private HashTrieMap(Object root, int size) {
        mRoot = root;
        mSize = size;
    }

    // *** Factories ***
    public HashTrieMap() {
        this(null, 0);
    }
    public static final HashTrieMap EMPTY = new HashTrieMap(null, 0);
    public static <K,V> HashTrieMap<K,V> empty() {
        return EMPTY;
    }
    public static <K,V> HashTrieMap<K,V> singleton(K key, V value) {
        return new HashTrieMap<K,V>(new Entry<K,V>(key, value), 1);
    }

    @Override
    public V get(K key) {
        if (key == null || mRoot == null) {
            return null;
        }

        final int hash = key.hashCode();
        int shift = 0;
        Object current = mRoot;
        while (true) {
            if (current instanceof Node) {
                // Case 1: a node - continue searching from the child node
                Node currentNode = (Node) current;
                // take a 5-bit chunk of the hash code
                int offset = (hash >>> shift) & HASH_MASK;
                int mask = 1 << offset;
                if ((currentNode.hasChild & mask) != 0) {
                    // we have found a child (which must be non-null), so continue searching
                    current = currentNode.children[Integer.bitCount(currentNode.hasChild & (mask - 1))];
                    shift += HASH_SHIFT;
                } else {
                    // no child with that hash - key must be missing
                    return null;
                }

            } else if (current instanceof Entry) {
                // Case 2: an Entry - return the value only if the key matches
                Entry<K, V> entry = (Entry) current;
                return key.equals(entry.getKey()) ? entry.getValue() : null;

            } else {
                // Case 3: must be a collision (Entry[]) - do a linear search in the collision 'array map'
                for (Entry<K,V> entry : (Entry[]) current) {
                    if (key.equals(entry.getKey())) {
                        return entry.getValue();
                    }
                }
                return null;
            }
        }
    }

    // helper function to implement 'put'
    // connects up the object 'next' to the trie between 'root' & 'parent'
    // returns the new value of the trie root
    private static Object connect(Object root, Object[] parent, int parentIndex, Object next) {
        if (root == null) {
            // we're looking at the root node, return it
            return next;
        } else {
            // we're looking at some child node, the root doesn't change,
            // but we must connect 'next' up to its' parent instead
            parent[parentIndex] = next;
            return root;
        }
    }

    /**
     * Find a key in a collision array.
     * @param entries the collision array to search
     * @param key the key to match
     * @return the index of the entry, or <code>entries.length</code> if not found
     */
    private static <K> int findCollision(Entry<K,?>[] entries, K key) {
        int index = 0;
        while (index < entries.length) {
            if (key.equals(entries[index].getKey())) {
                break;
            }
            ++index;
        }
        return index;
    }

    private static <T> T[] copyWithout(T[] original, int index) {
        T[] result = Arrays.copyOf(original, original.length - 1);
        if (index < original.length - 1) {
            result[index] = original[original.length - 1];
        }
        return result;
    }

    @Override
    public HashTrieMap<K, V> put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("Cannot add a null key to a HashTrieMap");
        }
        if (value == null) {
            throw new NullPointerException("Cannot add a null value to a HashTrieMap");
        }
        // special case the root - the only nullable object
        if (mRoot == null) {
            return singleton(key, value);
        }

        // convenience
        final Entry<K, V> entry = new Entry<K,V>(key, value);
        final int hash = key.hashCode();

        // a lot of state!
        Object newRoot = null;
        Object[] parent = null;
        int parentIndex = -1;
        Object current = mRoot;
        int shift = 0;
        while (true) {
            // are we at a leaf (single-element or multi-element 'collision')
            if (current instanceof Entry) {
                Entry<K,V> currentEntry = (Entry) current;

                if (key.equals(currentEntry.getKey())) {
                    // TERMINAL replace the existing entry (same key)
                    return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, entry), mSize);

                } else if (shift < Integer.SIZE) {
                    // split into a Node (and get handled as 'instanceof Node' below)
                    int currentEntryHash = (currentEntry.getKey().hashCode() >>> shift) & HASH_MASK;
                    current = new Node(new Object[] { currentEntry }, 1 << currentEntryHash);

                } else {
                    // TERMINAL generate a 2-element collision array
                    return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, new Entry[] { entry, currentEntry }), mSize + 1);
                }

            } else if (current instanceof Entry[]) {
                // a collision - we must be a leaf, so just add or replace the entry in the collision list
                Entry<K,V>[] currentCollision = (Entry[]) current;

                // find the existing index of the match
                int idx = findCollision(currentCollision, key);
                boolean found = idx < currentCollision.length;
                // expand if needed, and write in the new element
                Entry<K,V>[] newCollision = Arrays.copyOf(currentCollision, currentCollision.length + (found ? 0 : 1));
                newCollision[idx] = entry;

                // TERMINAL
                return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, newCollision), mSize + (found ? 0 : 1));
            }

            // then we must be at a Node
            Node currentNode = (Node) current;
            // take a 5-bit chunk of the hash code
            int offset = (hash >>> shift) & HASH_MASK;
            int mask = 1 << offset;
            int childIndex = Integer.bitCount(currentNode.hasChild & (mask - 1));
            if ((currentNode.hasChild & mask) == 0) {
                // missing key - expand space & add the entry into the empty slot
                Object[] children = new Object[currentNode.children.length + 1];
                for (int i = 0; i < childIndex; ++i) {
                    children[i] = currentNode.children[i];
                }
                children[childIndex] = entry;
                for (int i = childIndex + 1; i < children.length; ++i) {
                    children[i] = currentNode.children[i-1];
                }
                // TERMINAL
                return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, new Node(children, currentNode.hasChild | mask)), mSize + 1);

            } else {
                // NON-TERMINAL hash prefix collision
                current = currentNode.children[childIndex];
                shift += HASH_SHIFT;
                Object[] children = Arrays.copyOf(currentNode.children, currentNode.children.length);
                children[childIndex] = null;
                newRoot = connect(newRoot, parent, parentIndex, new Node(children, currentNode.hasChild));
                parent = children;
                parentIndex = childIndex;
                //continue; (implicit)
            }
        }
    }

    /**
     * Helper function for the implementation of {@link #remove(Object)}.
     * Sadly we must use this recursive method of removal (we cannot be iterative, like put),
     * as we don't always know if a parent node will be deleted until we reach the leaf.
     * @param current the current node, collision list, or entry
     * @param key the key to be removed
     * @param hash the hash value of key
     * @return the new version of current, with 'key' removed
     */
    private static <K> Object removeKey(Object current, K key, int hash, int shift) {
        if (current instanceof Node) {
            Node currentNode = (Node) current;
            // take a 5-bit chunk of the hash code
            int offset = (hash >>> shift) & HASH_MASK;
            int mask = 1 << offset;
            if ((currentNode.hasChild & mask) == 0) {
                // key not found - don't modify
                return current;

            } else {
                int childIndex = Integer.bitCount(currentNode.hasChild & (mask - 1));
                Object currentChild = currentNode.children[childIndex];
                Object newChild = removeKey(currentChild, key, hash, shift + HASH_SHIFT);
                if (currentChild == newChild) {
                    // key not found (recursively) - don't modify
                    return current;

                } else if (newChild == null) {
                    if (currentNode.children.length == 1) {
                        // no children left - delete this node
                        return null;

                    } else if (currentNode.children.length == 2) {
                        // only one child left - can 'collapse' to an Entry for the other child
                        return currentNode.children[1 - childIndex];

                    } else {
                        // remove the child
                        return new Node(copyWithout(currentNode.children, childIndex), currentNode.hasChild & ~mask);
                    }

                } else {
                    // rebuild children map
                    Object[] newChildren = Arrays.copyOf(currentNode.children, currentNode.children.length);
                    newChildren[childIndex] = newChild;
                    return new Node(newChildren, currentNode.hasChild);
                }
            }

        } else if (current instanceof Entry) {
            // if current is a match, remove it by returning null, otherwise don't modify
            return key.equals(((Entry) current).getKey()) ? null : current;

        } else { // must be an Entry[]
            Entry<K,?>[] currentCollision = (Entry[]) current;
            int idx = findCollision(currentCollision, key);

            if (currentCollision.length <= idx) {
                // key not found - don't modify
                return current;

            } else if (currentCollision.length == 2) {
                // only one left - not a collision list anymore!
                return currentCollision[2 - idx];

            } else {
                // create a new array without this entry
                return copyWithout(currentCollision, idx);
            }
        }
    }

    @Override
    public PersistentMap<K, V> remove(K key) {
        if (key == null) {
            throw new NullPointerException("Cannot add a null key to a HashTrieMap");
        }
        if (mRoot == null) {
            return this; // we were empty - still empty
        }
        // delegate to a recursive helper
        Object newRoot = removeKey(mRoot, key, key.hashCode(), 0);
        return newRoot == mRoot ? this : new HashTrieMap<K, V>(newRoot, mSize - 1);
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    public Map<K, V> asMap() {
        return new PersistentMapAdapter<K, V>(this);
    }

    private static class DepthFirstIterator<K,V> implements Iterator<Map.Entry<K,V>> {
        // stack of nodes from root to current leaf
        private static final int MAX_DEPTH = 7; // max size of the deque is 32 bits / 5 (bits/node) = 7 nodes
        private final Node[] mNodeStack = new Node[MAX_DEPTH];
        private final int[] mNodeIndexStack = new int[MAX_DEPTH];
        private int mNodeStackPointer = -1;
        // the current leaf (and position in the collision array, if needed)
        private Object mCurrent;
        private int mCurrentCollisionIndex = -1;

        private DepthFirstIterator(Object root) {
            if (root instanceof Node) {
                mNodeStack[0] = (Node) root;
                mNodeIndexStack[0] = -1;
                mNodeStackPointer = 0;
                moveToNext();
            } else {
                mCurrent = root;
            }
        }

        private void moveToNext() {
            if (mCurrent instanceof Map.Entry[]
                    && ((Map.Entry[]) mCurrent).length <= ++mCurrentCollisionIndex) {
                // do nothing - we've already advanced to the next collision node

            } else {
                // walk up the tree until we have successfully advanced to the next child
                while (mNodeStack[mNodeStackPointer].children.length <= ++mNodeIndexStack[mNodeStackPointer]) {
                    --mNodeStackPointer;
                    if (mNodeStackPointer < 0) {
                        mCurrent = null;
                        return;
                    }
                }
                // walk down the tree until we have found the first child from the current top of the stack
                while (true) {
                    Object child = mNodeStack[mNodeStackPointer].children[mNodeIndexStack[mNodeStackPointer]];
                    if (child instanceof Map.Entry) {
                        mCurrent = child;
                        return;

                    } else if (child instanceof Map.Entry[]) {
                        assert(0 < ((Map.Entry[]) child).length);
                        mCurrent = child;
                        mCurrentCollisionIndex = 0;
                        return;

                    } else { // Node
                        // push onto the stack & keep searching
                        assert(0 < ((Node) child).children.length);
                        ++mNodeStackPointer;
                        mNodeStack[mNodeStackPointer] = (Node) child;
                        mNodeIndexStack[mNodeStackPointer] = 0;
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            return mCurrent != null;
        }

        @Override
        public Map.Entry<K, V> next() {
            Map.Entry<K,V> next =
                    (mCurrent instanceof Entry[])
                    ? ((Entry[]) mCurrent)[mCurrentCollisionIndex]
                    : (Entry) mCurrent;
            moveToNext();
            return next;
        }
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new DepthFirstIterator<K, V>(mRoot);
    }

    @Override
    public boolean equals(Object _that) {
        if (_that == this) {
            return true;

        } else if (!(_that instanceof PersistentMap)) {
            return false;

        } else {
            PersistentMap<K,V> that = (PersistentMap) _that;
            if (this.size() != that.size()) {
                return false;
            }
            for (Map.Entry<K,V> thisEntry : this) {
                if (!thisEntry.getValue().equals(that.get(thisEntry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Map.Entry<K,V> entry : this) {
            hash += entry.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean separate = false;
        for (Map.Entry<K,V> entry : this) {
            if (separate) { sb.append(", "); }
            sb.append(entry);
            separate = true;
        }
        sb.append("}");
        return sb.toString();
    }
}
