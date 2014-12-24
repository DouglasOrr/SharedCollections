package com.dorr.persistent;

import junit.framework.TestCase;

import java.util.HashMap;

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

    // performance/fuzz

    private static final int PRIME = 61;
    private static final int SIZE = 10000;
    private static final int N = 100;

    public void testHuge() {
        for (int n = 0; n < N; ++n) {
            HashTrieMap<String, Integer> m = HashTrieMap.empty();
            for (int i = 0; i < SIZE; ++i) {
                m = m.put(Integer.toHexString(PRIME * i), i + n);
            }
            for (int i = 0; i < SIZE; ++i) {
                assertThat(m.get(Integer.toHexString(PRIME * i)), equalTo(i + n));
                assertThat(m.get(Integer.toHexString(PRIME * i + 1)), nullValue());
                assertThat(m.get(Integer.toHexString(PRIME * i - 1)), nullValue());
            }
        }
    }

    public void testHugeHashMap() {
        for (int n = 0; n < N; ++n) {
            HashMap<String, Integer> m = new HashMap<String, Integer>();
            for (int i = 0; i < SIZE; ++i) {
                m.put(Integer.toHexString(PRIME * i), i + n);
            }
            for (int i = 0; i < SIZE; ++i) {
                assertThat(m.get(Integer.toHexString(PRIME * i)), equalTo(i + n));
                assertThat(m.get(Integer.toHexString(PRIME * i + 1)), nullValue());
                assertThat(m.get(Integer.toHexString(PRIME * i - 1)), nullValue());
            }
        }
    }

    // TODO: more big fuzz
    // TODO: collision fest
    // TODO: performance
    // TODO: all the other crazy operations
}
