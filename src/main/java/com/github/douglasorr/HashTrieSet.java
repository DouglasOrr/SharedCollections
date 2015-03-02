package com.github.douglasorr;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * An implementation of the shared hash trie set, based on the HashTrieMap.
 */
public class HashTrieSet<T> extends AbstractSet<T> implements SharedSet<T>, Externalizable {
    private static final long serialVersionUID = 1046994592038807120L;
    private static final Object PRESENT = new Object();
    // this would be final, but for Java's horrid readExternal() deserialization
    private HashTrieMap<T, Object> mMap;

    private HashTrieSet(HashTrieMap<T, Object> map) {
        mMap = map;
    }

    // *** Factories ***

    public HashTrieSet() {
        mMap = HashTrieMap.EMPTY;
    }
    public HashTrieSet(Collection<? extends T> c) {
        HashTrieMap<T, Object> map = HashTrieMap.empty();
        for (T value : c) {
            map = map.with(value, PRESENT);
        }
        mMap = map;
    }

    public static HashTrieSet EMPTY = new HashTrieSet();
    public static <T> HashTrieSet<T> empty() {
        return EMPTY;
    }
    public static <T> HashTrieSet<T> singleton(T value) {
        return new HashTrieSet<T>(HashTrieMap.singleton(value, PRESENT));
    }
    public static <T> HashTrieSet<T> of(T... values) {
        return new HashTrieSet<T>(Arrays.asList(values));
    }

    // *** Core methods ***

    @Override
    public Iterator<T> iterator() {
        return mMap.keySet().iterator();
    }

    @Override
    public int size() {
        return mMap.size();
    }

    // *** Overridden for performance ***

    @Override
    public boolean contains(Object value) {
        return mMap.get(value) == PRESENT;
    }

    // *** SharedSet methods ***

    @Override
    public HashTrieSet<T> with(T value) {
        return new HashTrieSet<T>(mMap.with(value, PRESENT));
    }

    @Override
    public HashTrieSet<T> without(T value) {
        HashTrieMap<T, Object> mapWithoutValue = mMap.without(value);
        return mapWithoutValue == mMap ? this : new HashTrieSet<T>(mapWithoutValue);
    }

    // *** Externalizable ***

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // cannot just writeObject(mMap) as we never want to serialize PRESENT
        out.writeInt(mMap.size());
        for (T value : this) {
            out.writeObject(value);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        for (int i = 0; i < size; ++i) {
            mMap = mMap.with((T) in.readObject(), PRESENT);
        }
    }
}
