package com.dorr.persistent;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
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
        HashTrieMap<Object, Object> empty = HashTrieMap.empty();
        assertThat(empty.get("foo"), nullValue());
        assertThat(empty.size(), is(0));
        assertThat(empty.without("foo"), equalTo(empty));
        assertThat(empty.toString(), is("{}"));
    }

    public void testSingleton() {
        HashTrieMap<String, Integer> m = HashTrieMap.singleton("forty-two", 42);
        assertThat(m.get("forty-two"), equalTo(42));
        assertThat(m.get("one"), nullValue());
        assertThat(m.size(), is(1));
        assertThat(m.without("forty-two"), equalTo(HashTrieMap.<String, Integer>empty()));
        assertThat(m.without("forty-two").get("forty-two"), nullValue());
        assertThat(m.without("one"), equalTo(m));
        assertThat(m.without("one").get("forty-two"), equalTo(42));
    }

    public void testPut() {
        HashTrieMap<String, Integer> empty = HashTrieMap.empty();
        assertThat(empty.with("one", 1).get("one"), equalTo(1));
        assertThat(empty.with("one", 1).with("one", 100).get("one"), equalTo(100));

        HashTrieMap<String, Integer> m = empty.with("one", 1).with("onehundred", 100).with("twenty-two", 22);
        assertThat(m.get("one"), equalTo(1));
        assertThat(m.get("onehundred"), equalTo(100));
        assertThat(m.get("twenty-two"), equalTo(22));
        assertThat(m.get("two"), nullValue());
        System.out.println("Map: " + m);
    }

    public void testRemove() {
        HashTrieMap<String, Integer> m = HashTrieMap.<String, Integer>empty().with("one", 1).with("two", 2);
        assertThat(m.get("one"), equalTo(1));
        assertThat(m.without("one").get("one"), nullValue());
        assertThat(m.without("one").get("two"), equalTo(2));
        assertThat(m.without("three").get("one"), equalTo(1)); // remove missing
    }

    public void testIterate() {
        HashTrieMap<String, Integer> m = HashTrieMap.<String, Integer>empty().with("one", 1).with("two", 2).with("three", 3);

        Map<String, Integer> copy = new HashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : m.entrySet()) {
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
                m = m.with(new TestHash<String>(Integer.toString(i), hash), i);
            }
            for (int i = 0; i < 100; ++i) {
                TestHash<String> key = new TestHash<String>(Integer.toString(i), hash);
                assertThat(m.get(key), equalTo(i));
                assertThat(m.without(key).get(key), nullValue());
            }
            // TODO: remove all-but-1, all
        }
    }
}
