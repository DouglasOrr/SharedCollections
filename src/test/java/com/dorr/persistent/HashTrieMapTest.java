package com.dorr.persistent;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import scala.Option;
import scala.Option$;
import scala.Some$;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
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
        Set<Integer> extremeHashes = new HashSet<Integer>(); // TODO fill with lots of exciting hash values
        for (int hash = 0; hash < 100; ++hash) extremeHashes.add(hash);
        for (int hash = 1; hash != 0; hash <<= 1) extremeHashes.add(hash);
        System.out.println(extremeHashes);

        for (int hash = 0; hash < 100; ++hash) {
            HashTrieMap<TestHash<String>, Integer> m = HashTrieMap.empty();
            for (int i = 0; i < 100; ++i) {
                m = m.put(new TestHash<String>(Integer.toString(i), hash), i);
            }
            for (int i = 0; i < 100; ++i) {
                assertThat(m.get(new TestHash<String>(Integer.toString(i), hash)), equalTo(i));
            }
        }
    }

    // performance/fuzz

    public static class Performance extends TestCase {
        private static final int PRIME = 61;
        private static final int SIZE = 100000;
        private static final int N = 100;

        public void testDougMap() {
            for (int n = 0; n < N; ++n) {
                HashTrieMap<String, Integer> m = HashTrieMap.empty();
                for (int i = 0; i < SIZE; ++i) {
                    m = m.put(Integer.toHexString(PRIME * i), i + n);
                }
                for (int i = 0; i < SIZE; ++i) {
                    assertThat(m.get(Integer.toHexString(PRIME * i)), equalTo(i + n));
                    assertThat(m.get(Integer.toHexString(PRIME * i + 1)), nullValue());
                }
            }
        }

        public void testJavaHashMap() {
            for (int n = 0; n < N; ++n) {
                HashMap<String, Integer> m = new HashMap<String, Integer>();
                for (int i = 0; i < SIZE; ++i) {
                    m.put(Integer.toHexString(PRIME * i), i + n);
                }
                for (int i = 0; i < SIZE; ++i) {
                    assertThat(m.get(Integer.toHexString(PRIME * i)), equalTo(i + n));
                    assertThat(m.get(Integer.toHexString(PRIME * i + 1)), nullValue());
                }
            }
        }

        public void testClojurePersistentHashMap() {
            for (int n = 0; n < N; ++n) {
                IPersistentMap m = PersistentHashMap.create();
                for (int i = 0; i < SIZE; ++i) {
                    m = m.assoc(Integer.toHexString(PRIME * i), i + n);
                }
                for (int i = 0; i < SIZE; ++i) {
                    assertThat(m.valAt(Integer.toHexString(PRIME * i)), CoreMatchers.<Object> equalTo(i + n));
                    assertThat(m.valAt(Integer.toHexString(PRIME * i + 1)), nullValue());
                }
            }
        }

        public void testScalaMap() {
            for (int n = 0; n < N; ++n) {
                scala.collection.immutable.Map<String, Integer> m = scala.collection.immutable.HashMap$.MODULE$.empty();
                for (int i = 0; i < SIZE; ++i) {
                    m = m.updated(Integer.toHexString(PRIME * i), i + n);
                }
                for (int i = 0; i < SIZE; ++i) {
                    assertThat(m.get(Integer.toHexString(PRIME * i)), equalTo(Option$.MODULE$.apply(i + n)));
                    assertThat(m.get(Integer.toHexString(PRIME * i + 1)), equalTo(Option$.MODULE$.<Integer>empty()));
                }
            }
        }
    }

    // TODO: collision fest
    // TODO: performance
    // TODO: all the other crazy operations
}
