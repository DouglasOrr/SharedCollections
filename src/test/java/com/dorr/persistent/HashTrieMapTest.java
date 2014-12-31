package com.dorr.persistent;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HashTrieMapTest extends TestCase {

    public void testEmpty() {
        assertThat(HashTrieMap.empty().get("foo"), nullValue());
    }

    public void testSingleton() {
        HashTrieMap<String, Integer> m = HashTrieMap.singleton("forty-two", 42);
        assertThat(m.get("forty-two"), equalTo(42));
        assertThat(m.get("one"), nullValue());
    }

    public void testPut() {
        HashTrieMap<String, Integer> empty = HashTrieMap.empty();
        assertThat(empty.put("one", 1).get("one"), equalTo(1));
        assertThat(empty.put("one", 1).put("one", 100).get("one"), equalTo(100));

        HashTrieMap<String, Integer> m = empty.put("one", 1).put("onehundred", 100).put("twenty-two", 22);
        assertThat(m.get("one"), equalTo(1));
        assertThat(m.get("onehundred"), equalTo(100));
        assertThat(m.get("twenty-two"), equalTo(22));
        assertThat(m.get("two"), nullValue());
    }

    public static class TestHash<T> {
        public final int hash;
        public final T value;
        public TestHash(T value, int hash) {
            this.hash = hash;
            this.value = value;
        }
        @Override
        public int hashCode() { return hash; }
        @Override
        public String toString() { return value.toString(); }
        @Override
        public boolean equals(Object that) {
            return that instanceof TestHash && this.value.equals(((TestHash) that).value);
        }
    }

    public void testCollisions() throws Exception {
        Set<Integer> extremeHashes = new HashSet<Integer>();
        for (int hash = 0; hash < 100; ++hash) extremeHashes.add(hash);
        for (int hash = 1; hash != 0; hash <<= 1) extremeHashes.add(hash);

        for (Integer hash : extremeHashes) {
            HashTrieMap<TestHash<String>, Integer> m = HashTrieMap.empty();
            for (int i = 0; i < 100; ++i) {
                m = m.put(new TestHash<String>(Integer.toString(i), hash), i);
            }
            for (int i = 0; i < 100; ++i) {
                assertThat(m.get(new TestHash<String>(Integer.toString(i), hash)), equalTo(i));
            }
        }
    }
}
