package com.dorr.persistent;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HashTrieMapTest extends TestCase {
    /** A wrapper class with an explicit hash code, specified at construction time. */
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
        System.out.println("Map: " + m);
    }

    public void testRemove() {
        HashTrieMap<String, Integer> m = HashTrieMap.<String, Integer>empty().put("one", 1).put("two", 2);
        assertThat(m.get("one"), equalTo(1));
        assertThat(m.remove("one").get("one"), nullValue());
        assertThat(m.remove("one").get("two"), equalTo(2));
        assertThat(m.remove("three").get("one"), equalTo(1)); // remove missing
    }

    public void testIterate() {
        HashTrieMap<String, Integer> m = HashTrieMap.<String, Integer>empty().put("one", 1).put("two", 2).put("three", 3);

        Map<String, Integer> copy = new HashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : m) {
            copy.put(entry.getKey(), entry.getValue());
        }
        assertThat(copy, CoreMatchers.<Map<String, Integer>> equalTo(ImmutableMap.of("one", 1, "two", 2, "three", 3)));
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
                TestHash<String> key = new TestHash<String>(Integer.toString(i), hash);
                assertThat(m.get(key), equalTo(i));
                assertThat(m.remove(key).get(key), nullValue());
            }
            // TODO: remove all-but-1, all
        }
    }
}
