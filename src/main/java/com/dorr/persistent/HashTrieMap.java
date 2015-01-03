package com.dorr.persistent;

import java.util.*;

/**
 * An implementation of the persistent hash trie map.
 */
public class HashTrieMap<K,V> implements PersistentMap<K,V> {

    private static class Node {
        // children is an array of stride-2, with each pair of elements either being:
        // (Node, null) | (K, V) | (KV[], null)
        public final Object[] children;
        public final int hasChild;
        private Node(Object[] children, int hasChild) {
            this.children = children;
            this.hasChild = hasChild;
            assert children != null;
            assert 2*Integer.bitCount(hasChild) == children.length;
        }
    }

    // TODO: Clojure's implementation saves memory by putting keys & values adjacent in the children array - do this?
    private static class EntryView<K,V> implements Map.Entry<K,V> {
        private final Object[] mObjects;
        private final int mIndex;

        private EntryView(Object[] objects, int index) {
            mObjects = objects;
            mIndex = index;
        }

        @Override
        public K getKey() {
            return (K) mObjects[mIndex];
        }
        @Override
        public V getValue() {
            return (V) mObjects[mIndex + 1];
        }

        @Override
        public boolean equals(Object _that) {
            if (!(_that instanceof Map.Entry)) {
                return false;

            } else {
                Map.Entry<K,V> that = (Map.Entry) _that;
                return getKey().equals(that.getKey())
                        && getValue().equals(that.getValue());
            }
        }
        @Override
        public int hashCode() {
            return mObjects[mIndex].hashCode() ^ mObjects[mIndex + 1].hashCode();
        }

        @Override
        public V setValue(V v) {
            throw new UnsupportedOperationException("SetValue on an immutable map");
        }
    }

    private final Node mRoot;
    private final int mSize;
    private static final int HASH_MASK = 0x001F;
    private static final int HASH_SHIFT = 5;

    private HashTrieMap(Node root, int size) {
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
        return new HashTrieMap<K,V>(new Node(new Object[] { key, value }, 1 << (key.hashCode() & HASH_MASK)), 1);
    }

    // *** Core ***

    @Override
    public V get(K key) {
        if (key == null || mRoot == null) {
            return null;
        }

        final int hash = key.hashCode();
        int shift = 0;
        Node current = mRoot;
        while (true) {
            // take a 5-bit chunk of the hash code
            int offset = (hash >>> shift) & HASH_MASK;
            int mask = 1 << offset;
            if ((current.hasChild & mask) != 0) {
                // we have found a child (which must be non-null)
                int index = Integer.bitCount(current.hasChild & (mask - 1));
                Object next = current.children[2*index];

                if (next instanceof Node) {
                    // Case 1: found another node - continue searching
                    current = (Node) next;
                    shift += HASH_SHIFT;

                } else { // a leaf
                    Object nextValue = current.children[2*index + 1];
                    if (nextValue != null) {
                        // Case 2: a key-value mapping
                        return key.equals(next) ? (V) nextValue : null;

                    } else {
                        // Case 3: a collision node
                        Object[] collision = (Object[]) next;
                        for (int i = 0; i < collision.length; i += 2) {
                            if (key.equals(collision[i])) {
                                return (V) collision[i + 1];
                            }
                        }
                        return null;
                    }
                }

            } else {
                // no child with that hash - key must be missing
                return null;
            }
        }
    }

    // helper function to implement 'put'
    // connects up the object 'next' to the trie between 'root' & 'parent'
    // returns the new value of the trie root
    private static Node connect(Node root, Object[] parent, int parentIndex, Object next, Object nextValue) {
        if (root == null) {
            // we're looking at the root node, return it
            return (Node) next;
        } else {
            // we're looking at some child node, the root doesn't change,
            // but we must connect 'next' up to its' parent instead
            parent[2*parentIndex] = next;
            parent[2*parentIndex+1] = nextValue;
            return root;
        }
    }

    /**
     * Find a key in a collision array.
     * @param entries the collision array to search
     * @param key the key to match
     * @return the index of the entry, or <code>entries.length</code> if not found
     */
    private static int findCollision(Object[] keyValues, Object key) {
        for (int index = 0; index < keyValues.length; index += 2) {
            if (key.equals(keyValues[index])) {
                return index;
            }
        }
        return keyValues.length;
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
        // special case the root - the only nullable Node
        if (mRoot == null) {
            return singleton(key, value);
        }
        final int hash = key.hashCode();

        // a lot of state!
        Node newRoot = null;
        Node current = mRoot;
        Object[] parent = null;
        int parentIndex = -1;
        int shift = 0;
        while (true) {
            // take a 5-bit chunk of the hash code
            int offset = (hash >>> shift) & HASH_MASK;
            int mask = 1 << offset;
            int childIndex = Integer.bitCount(current.hasChild & (mask - 1));
            if ((current.hasChild & mask) == 0) {
                // missing key - expand space by 2 and add the (key,value) pair
                Object[] children = new Object[current.children.length + 2];
                for (int i = 0; i < 2*childIndex; ++i) {
                    children[i] = current.children[i];
                }
                children[2*childIndex] = key;
                children[2*childIndex+1] = value;
                for (int i = 2*childIndex; i < current.children.length; ++i) {
                    children[i+2] = current.children[i];
                }
                // TERMINAL
                return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, new Node(children, current.hasChild | mask), null), mSize + 1);

            } else {
                // NON-TERMINAL hash prefix collision
                Object[] children = Arrays.copyOf(current.children, current.children.length);
                newRoot = connect(newRoot, parent, parentIndex, new Node(children, current.hasChild), null);
                parent = children;
                parentIndex = childIndex;

                Object next = current.children[2*childIndex];
                if (next instanceof Node) {
                    current = (Node) next;
                    shift += HASH_SHIFT;
                    //continue; (implicit)

                } else { // leaf
                    Object nextValue = current.children[2*childIndex+1];
                    if (nextValue != null) {
                        // single element leaf
                        if (key.equals(next)) {
                            // TERMINAL replace the existing entry (same key)
                            return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, key, value), mSize);

                        } else if (shift < Integer.SIZE) {
                            // split into a Node (and get handled next time round the loop)
                            int nextHash = (next.hashCode() >>> shift) & HASH_MASK;
                            current = new Node(new Object[] { next, nextValue }, 1 << nextHash);
                            shift += HASH_SHIFT;
                            //continue; (implicit)

                        } else {
                            // TERMINAL generate a 4-element collision array
                            return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, new Object[] { next, nextValue, key, value }, null), mSize + 1);
                        }

                    } else {
                        // collision leaf - just add or replace the entry in the collision list
                        Object[] collision = (Object[]) next;

                        // find the existing index of the match
                        int idx = findCollision(collision, key);
                        boolean found = idx < collision.length;
                        // expand if needed, and write in the new element
                        Object[] newCollision = Arrays.copyOf(collision, collision.length + (found ? 0 : 2));
                        newCollision[idx] = key;
                        newCollision[idx+1] = value;

                        // TERMINAL
                        return new HashTrieMap<K,V>(connect(newRoot, parent, parentIndex, newCollision, null), mSize + (found ? 0 : 1));
                    }
                }
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
    private static <K> Node removeFrom(Node current, K key, int hash, int shift) {
        return null;
//        if (current instanceof Node) {
//            Node currentNode = (Node) current;
//            // take a 5-bit chunk of the hash code
//            int offset = (hash >>> shift) & HASH_MASK;
//            int mask = 1 << offset;
//            if ((currentNode.hasChild & mask) == 0) {
//                // key not found - don't modify
//                return current;
//
//            } else {
//                int childIndex = Integer.bitCount(currentNode.hasChild & (mask - 1));
//                Object currentChild = currentNode.children[childIndex];
//                Object newChild = removeFrom(currentChild, key, hash, shift + HASH_SHIFT);
//                if (currentChild == newChild) {
//                    // key not found (recursively) - don't modify
//                    return current;
//
//                } else if (newChild == null) {
//                    if (currentNode.children.length == 1) {
//                        // no children left - delete this node
//                        return null;
//
//                    } else if (currentNode.children.length == 2) {
//                        // only one child left - can 'collapse' to an Entry for the other child
//                        return currentNode.children[1 - childIndex];
//
//                    } else {
//                        // remove the child
//                        return new Node(copyWithout(currentNode.children, childIndex), currentNode.hasChild & ~mask);
//                    }
//
//                } else {
//                    // rebuild children map
//                    Object[] newChildren = Arrays.copyOf(currentNode.children, currentNode.children.length);
//                    newChildren[childIndex] = newChild;
//                    return new Node(newChildren, currentNode.hasChild);
//                }
//            }
//
//        } else if (current instanceof Entry) {
//            // if current is a match, remove it by returning null, otherwise don't modify
//            return key.equals(((Entry) current).getKey()) ? null : current;
//
//        } else { // must be an Entry[]
//            Entry<K,?>[] currentCollision = (Entry[]) current;
//            int idx = findCollision(currentCollision, key);
//
//            if (currentCollision.length <= idx) {
//                // key not found - don't modify
//                return current;
//
//            } else if (currentCollision.length == 2) {
//                // only one left - not a collision list anymore!
//                return currentCollision[2 - idx];
//
//            } else {
//                // create a new array without this entry
//                return copyWithout(currentCollision, idx);
//            }
//        }
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
        Node newRoot = removeFrom(mRoot, key, key.hashCode(), 0);
        return newRoot == mRoot ? this : new HashTrieMap<K, V>(newRoot, mSize - 1);
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
            if (mCurrent instanceof Entry[]
                    && ((Entry[]) mCurrent).length <= ++mCurrentCollisionIndex) {
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
                    if (child instanceof Entry) {
                        mCurrent = child;
                        return;

                    } else if (child instanceof Entry[]) {
                        assert(0 < ((Entry[]) child).length);
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

    // *** Other methods ***

    @Override
    public int size() {
        return mSize;
    }

    @Override
    public Map<K, V> asMap() {
        return new PersistentMapAdapter<K, V>(this);
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
