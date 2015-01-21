package com.dorr.persistent;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

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
        public String toString() { return value.toString() + "#" + Integer.toBinaryString(hash); }
        @Override
        public boolean equals(Object that) {
            return that instanceof TestHash && this.value.equals(((TestHash) that).value);
        }
    }

    // basic use

    public void testWith() {
        HashTrieMap<String, Integer> empty = HashTrieMap.empty();
        assertThat(empty.with("one", 1).get("one"), equalTo(1));
        assertThat(empty.with("one", 1).with("one", 100).get("one"), equalTo(100));

        HashTrieMap<String, Integer> m = empty.with("one", 1).with("onehundred", 100).with("twenty-two", 22);
        assertThat(m.get("one"), equalTo(1));
        assertThat(m.get("onehundred"), equalTo(100));
        assertThat(m.get("twenty-two"), equalTo(22));
        assertThat(m.get("two"), nullValue());
    }

    public void testWithout() {
        HashTrieMap<String, Integer> m = HashTrieMap.<String, Integer>empty().with("one", 1).with("two", 2);
        assertThat(m.get("one"), equalTo(1));
        assertThat(m.without("one").get("one"), nullValue());
        assertThat(m.without("one").get("two"), equalTo(2));
        assertThat(m.without("three"), sameInstance(m)); // remove missing
    }

    public void testIterate() {
        HashTrieMap<String, Integer> m = HashTrieMap.of("one", 1, "two", 2, "three", 3);

        // this will do entrySet iteration internally
        assertThat(new HashMap<String, Integer>(m), CoreMatchers.<Map<String, Integer>> equalTo(ImmutableMap.of("one", 1, "two", 2, "three", 3)));
    }

    // special cases

    private static class Verifier<K,V> {
        private final Set<K> mKeySuperset = new HashSet<K>();
        private final Set<V> mValueSuperset = new HashSet<V>();
        private final Deque<Map.Entry<HashTrieMap<K,V>, Map<K,V>>> mStates = new ArrayDeque<Map.Entry<HashTrieMap<K, V>, Map<K, V>>>();

        public Verifier() {
            mStates.add(new SimpleImmutableEntry<HashTrieMap<K, V>, Map<K, V>>(
                    HashTrieMap.<K, V> empty(),
                    new HashMap<K, V>())
            );
        }

        private void addState(HashTrieMap<K,V> hashTrieMap, HashMap<K,V> hashMap) {
            mStates.add(new SimpleImmutableEntry<HashTrieMap<K, V>, Map<K, V>>(hashTrieMap, hashMap));
            verify();
        }

        /** Call to update the map under test & baseline implementation (keeping a record of previous states). */
        public void put(K key, V value) {
            HashTrieMap<K,V> hashTrieMap = mStates.getLast().getKey().with(key, value);
            HashMap<K,V> hashMap = new HashMap<K, V>(mStates.getLast().getValue());
            hashMap.put(key, value);
            mKeySuperset.add(key);
            mValueSuperset.add(value);
            addState(hashTrieMap, hashMap);
        }
        /** Call to update the map under test & baseline implementation (keeping a record of previous states). */
        public void remove(K key) {
            HashTrieMap<K,V> hashTrieMap = mStates.getLast().getKey().without(key);
            HashMap<K,V> hashMap = new HashMap<K, V>(mStates.getLast().getValue());
            hashMap.remove(key);
            addState(hashTrieMap, hashMap);
        }

        /** Call to perform a plethora of tests to make sure that the map (and all previous states) are consistent
         * with the baseline implementation. */
        private void verify() {
            for (Map.Entry<HashTrieMap<K,V>, Map<K,V>> state : mStates) {
                HashTrieMap<K,V> map = state.getKey();
                Map<K,V> baseline = state.getValue();

                assertThat(map, equalTo(baseline));
                assertThat(map.entrySet(), equalTo(baseline.entrySet()));
                assertThat(map.keySet(), equalTo(baseline.keySet()));
                assertThat(map.values(), Matchers.containsInAnyOrder(baseline.values().toArray()));

                assertThat("copy entrySet (force use of iteration)",
                        new HashSet<Map.Entry<K,V>>(map.entrySet()), equalTo(baseline.entrySet()));
                assertThat("copy keySet (force use of iteration)",
                        new HashSet<K>(map.keySet()), equalTo(baseline.keySet()));

                assertThat(map.size(), equalTo(baseline.size()));
                assertThat(map.entrySet().size(), equalTo(baseline.entrySet().size()));
                assertThat(map.keySet().size(), equalTo(baseline.keySet().size()));
                assertThat(map.values().size(), equalTo(baseline.values().size()));

                assertThat(map.hashCode(), equalTo(baseline.hashCode()));

                for (K key : mKeySuperset) {
                    assertThat(map.get(key), equalTo(baseline.get(key)));
                    assertThat(map.containsKey(key), equalTo(baseline.containsKey(key)));
                    assertThat(map.keySet().contains(key), equalTo(baseline.keySet().contains(key)));
                    assertThat(map.entrySet().contains(new SimpleImmutableEntry<K, V>(key, null)), is(false));

                    if (baseline.containsKey(key)) {
                        Map.Entry<K,V> entry = new SimpleImmutableEntry<K, V>(key, baseline.get(key));
                        assertThat(map.toString(), containsString(entry.toString()));
                        assertThat(map.entrySet().contains(entry), is(true));
                    }
                }

                for (V value : mValueSuperset) {
                    assertThat(map.containsValue(value), is(baseline.containsValue(value)));
                }
            }
        }
    }

    public void testEmpty() {
        for (HashTrieMap<Object,Object> empty : Arrays.asList(
                HashTrieMap.EMPTY,
                HashTrieMap.empty(),
                HashTrieMap.of(),
                new HashTrieMap<Object, Object>(),
                new HashTrieMap<Object, Object>(Collections.emptyMap()),
                new HashTrieMap<Object, Object>(new HashMap<Object, Object>())
        )) {
            assertThat(empty.size(), is(0));
            assertThat(empty.get("foo"), nullValue());
            assertThat(empty.without("foo"), sameInstance(empty));
            assertThat(empty.toString(), is("{}"));
            assertThat(empty, equalTo(Collections.emptyMap()));
        }
    }

    public void testSmallMaps() {
        Verifier<String, Integer> test = new Verifier<String, Integer>();
        test.put("one", 1); // insert
        test.put("two", 2); // insert
        test.put("two", 3); // update value
        test.remove("two"); // remove
        test.remove("two"); // remove (missing)
        test.put("two", 4); // reinsert
        test.remove("one"); // remove
        test.remove("two"); // remove
        test.remove("zero"); // remove (missing/empty)
    }

    private static <T> TestHash<T> th(T value, int hash) {
        return new TestHash<T>(value, hash);
    }

    /** Messing with the array of children. */
    public void testChildMovement() {
        Verifier<TestHash<String>, Integer> test = new Verifier<TestHash<String>, Integer>();

        // moving children around
        test.put(th("01000", 0x0008), 1); // entry
        test.put(th("00011", 0x0003), 2); // node, 2* children (split)
        test.put(th("01111", 0x000f), 3); // node, 3* children (insert last)
        test.put(th("00111", 0x0007), 4); // node, 4* children (insert mid)
        test.put(th("00001", 0x0001), 5); // node, 5* children (insert first)

        test.remove(th("00001", 0x0001)); // first
        test.remove(th("00111", 0x0007)); // mid
        test.remove(th("01111", 0x000f)); // last
        test.remove(th("00011", 0x0003)); // join
        test.remove(th("01000", 0x0008)); // empty
    }

    /** When removing alters the structure of the trie. */
    public void testRemoveCollapse() {
        Verifier<TestHash<String>, Integer> test = new Verifier<TestHash<String>, Integer>();

        test.put(th("0100000", 0x20), 1);
        test.put(th("1100000", 0x60), 2);
        test.put(th("0011111", 0x1f), 3);
        // REGRESSION - remove an entry that is the sibling of a node - if the node is collapsed the hashes will be wrong
        test.remove(th("0011111", 0x1f));
    }

    /** Replacing existing keys - at the root, normal nodes, collision nodes. */
    public void testReplace() {
        Verifier<TestHash<String>, Integer> test = new Verifier<TestHash<String>, Integer>();

        test.put(th("a",  0xa00008), 1);
        test.put(th("a",  0xa00008), 2); // replace root

        test.put(th("b",  0xb00008), 3);
        test.put(th("b",  0xb00008), 4); // replace node

        test.put(th("b2", 0xb00008), 5);
        test.put(th("b3", 0xb00008), 6);
        test.put(th("b",  0xb00008), 7); // replace first collision
        test.put(th("b2", 0xb00008), 8); // replace mid collision
        test.put(th("b3", 0xb00008), 9); // replace last collision
    }

    /** Add a load of entries that all have exactly the same hash. */
    public void testCollisions() {
        final int repeat = 5;

        List<Integer> extremeHashes = new ArrayList<Integer>();
        for (int hash = 0; hash < 100; ++hash) extremeHashes.add(hash);
        for (int hash = 1; hash != 0; hash <<= 1) extremeHashes.add(hash);
        extremeHashes.add(-1);

        for (Integer hash : extremeHashes) {
            Verifier<TestHash<String>, Integer> test = new Verifier<TestHash<String>, Integer>();
            // insert (collide)
            test.put(th(Integer.toString(0), hash), -100); // to force updating a single entry, first
            for (int i = 0; i < repeat; ++i) {
                test.put(th(Integer.toString(i), hash), i); // collision array
            }
            // update
            for (int i = 0; i < repeat; ++i) {
                test.put(th(Integer.toString(i), hash), i + 1000);
            }
            // remove
            for (int i = 0; i < repeat; ++i) {
                test.remove(th(Integer.toString(i), hash));
            }
        }
    }

    // Fuzz tests

    private static abstract class IntegerGenerator {
        private final String mName;
        public IntegerGenerator(String name) { mName = name; }
        public abstract int next(Random random);
        @Override
        public String toString() {
            return mName;
        }
    }
    private static final IntegerGenerator GOOD_RANDOM_GENERATOR = new IntegerGenerator("goodhash") {
        @Override
        public int next(Random random) {
            return random.nextInt();
        }
    };
    private static final IntegerGenerator WEIRD_RANDOM_GENERATOR = new IntegerGenerator("weirdhash") {
        @Override
        public int next(Random random) {
            // reversed floats in the range [0 1] should have lots of collisions for the
            // early levels (which use the least significant portion of the hash), then get
            // quite bushy toward the mantissa
            return Integer.reverse(Float.floatToIntBits(random.nextFloat()));
        }
    };
    private static IntegerGenerator limitedRandomGenerator(final int range) {
        return new IntegerGenerator("limitedhash:" + range) {
            @Override
            public int next(Random random) {
                return Hashing.sipHash24().hashInt(random.nextInt(range)).asInt();
            }
        };
    }

    /** General fuzz */
    public void testFuzz() {
        for (IntegerGenerator generator : Arrays.asList(
                GOOD_RANDOM_GENERATOR,
                WEIRD_RANDOM_GENERATOR,
                limitedRandomGenerator(100000),
                limitedRandomGenerator(10000),
                limitedRandomGenerator(1000))) {

            final int nOuter = 100;
            final int nInner = 1000;
            Random random = new Random(42);
            HashTrieMap<TestHash<Integer>, Integer> map = HashTrieMap.empty();
            HashMap<TestHash<Integer>, Integer> reference = new HashMap<TestHash<Integer>, Integer>();
            long before = System.nanoTime();

            // add the keys, checking validity periodically
            int nextKey = 0;
            for (int n = 0; n < nOuter; ++n) {
                for (int i = 0; i < nInner; ++i) {
                    TestHash<Integer> k = th(nextKey++, generator.next(random));
                    map = map.with(k, k.value);
                    reference.put(k, k.value);
                }
                assertThat(map, Matchers.<Map<TestHash<Integer>, Integer>> equalTo(reference));
            }

            // remove all the keys, checking validity periodically
            List<TestHash<Integer>> keys = new ArrayList<TestHash<Integer>>(reference.keySet());
            Collections.shuffle(keys, random);
            Iterator<TestHash<Integer>> keyIterator = keys.iterator();
            for (int n = 0; n < nOuter; ++n) {
                for (int i = 0; i < nInner; ++i) {
                    TestHash<Integer> key = keyIterator.next();
                    map = map.without(key);
                    reference.remove(key);
                }
                assertThat(map, Matchers.<Map<TestHash<Integer>, Integer>> equalTo(reference));
            }

            long after = System.nanoTime();
            System.out.println("Generator " + generator + " took " + (after - before) / 1.0E6 + " ms");
        }
    }
}
