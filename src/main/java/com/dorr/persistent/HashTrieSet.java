package com.dorr.persistent;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * An implementation of the persistent hash trie set, based on the HashTrieMap.
 */
public class HashTrieSet<T> extends AbstractSet<T> implements PersistentSet<T> {
    private final HashTrieMap<T, Object> mMap;
    private static final Object PRESENT = new Object();

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

    // *** PersistentSet methods ***

    @Override
    public HashTrieSet<T> with(T value) {
        return new HashTrieSet<T>(mMap.with(value, PRESENT));
    }

    @Override
    public HashTrieSet<T> without(T value) {
        HashTrieMap<T, Object> mapWithoutValue = mMap.without(value);
        return mapWithoutValue == mMap ? this : new HashTrieSet<T>(mapWithoutValue);
    }
}
