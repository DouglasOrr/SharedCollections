package com.dorr.persistent;

import java.util.*;

/** Adapts a PersistentMap abstraction to the java.util.Map. */
public class PersistentMapAdapter<K,V> extends AbstractMap<K,V> {
    private final PersistentMap<K,V> mMap;
    private final Set<Entry<K,V>> mEntrySet;
    private final Set<K> mKeySet;

    public PersistentMapAdapter(PersistentMap<K, V> map) {
        mMap = map;

        mEntrySet = new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return mMap.iterator();
            }

            @Override
            public int size() {
                return mMap.size();
            }

            // Overrides for performance

            @Override
            public boolean contains(Object o) {
                Entry<K,V> mapping = (Entry) o;
                K key = mapping.getKey();
                return key != null && mMap.get(key).equals(mapping.getValue());
            }
        };

        mKeySet = new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                final Iterator<Entry<K,V>> entryIterator = mMap.iterator();
                return new Iterator<K>() {
                    @Override
                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }
                    @Override
                    public K next() {
                        return entryIterator.next().getKey();
                    }
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("remove() called on persistent iterator (you cannot mutate a PersistentMap using its' iterator)");
                    }
                };
            }

            @Override
            public int size() {
                return mMap.size();
            }

            // Overrides for performance

            @Override
            public boolean contains(Object key) {
                return mMap.get((K) key) != null;
            }
        };
    }

    /** Get the underlying {@link com.dorr.persistent.PersistentMap} implementation. */
    public PersistentMap<K,V> getMap() { return mMap; }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return mEntrySet;
    }

    // Overrides for performance

    @Override
    public Set<K> keySet() {
        return mKeySet;
    }

    @Override
    public boolean containsKey(Object key) {
        return mMap.get((K) key) != null;
    }

    @Override
    public V get(Object key) {
        return mMap.get((K) key);
    }
}
