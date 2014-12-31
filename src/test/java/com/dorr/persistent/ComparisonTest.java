package com.dorr.persistent;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import com.google.common.base.Joiner;
import objectexplorer.Chain;
import objectexplorer.MemoryMeasurer;
import objectexplorer.ObjectVisitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import scala.Option;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ComparisonTest {
    /** Abstracts away the details of map implementations, for fair-ish comparison. */
    public static abstract class MapTester<K,V> {
        public abstract V get(K key);
        public abstract void put(K key, V value);
        public abstract void reset();
    }
    public static class PersistentMapTester<K,V> extends MapTester<K,V> {
        private final PersistentMap<K,V> mEmpty;
        private PersistentMap<K,V> mMap;
        public PersistentMapTester(PersistentMap<K,V> empty) {
            mEmpty = empty;
            mMap = empty;
        }
        @Override
        public V get(K key) {
            return mMap.get(key);
        }
        @Override
        public void put(K key, V value) {
            mMap = mMap.put(key, value);
        }
        @Override
        public void reset() {
            mMap = mEmpty;
        }
        @Override
        public String toString() {
            return mMap.getClass().getName();
        }
    }
    public static abstract class JavaMapTester<K,V> extends MapTester<K,V> {
        protected Map<K,V> mMap;
        { reset(); }
        @Override
        public V get(K key) {
            return mMap.get(key);
        }
        @Override
        public void put(K key, V value) {
            mMap.put(key, value);
        }
        @Override
        public String toString() {
            return mMap.getClass().getName();
        }
    }
    public static class JavaHashMapTester<K,V> extends JavaMapTester<K,V> {
        @Override
        public void reset() {
            mMap = new HashMap<K, V>();
        }
    }
    public static class JavaTreeMapTester<K,V> extends JavaMapTester<K,V> {
        @Override
        public void reset() {
            mMap = new TreeMap<K, V>();
        }
    }

    public static class ClojureIPersistentMapTester<K,V> extends MapTester<K,V> {
        private final IPersistentMap mEmpty = PersistentHashMap.create();
        private IPersistentMap mMap = mEmpty;
        @Override
        public V get(K key) {
            return (V) mMap.valAt(key);
        }
        @Override
        public void put(K key, V value) {
            mMap = mMap.assoc(key, value);
        }
        @Override
        public void reset() {
            mMap = mEmpty;
        }
        @Override
        public String toString() {
            return mMap.getClass().getName();
        }
    }
    public static class ScalaImmutableMapTester<K,V> extends MapTester<K,V> {
        private final scala.collection.immutable.Map<K,V> mEmpty;
        private scala.collection.immutable.Map<K,V> mMap;
        public ScalaImmutableMapTester(scala.collection.immutable.Map<K,V> empty) {
            mEmpty = empty;
            mMap = empty;
        }
        @Override
        public V get(K key) {
            Option<V> opt = mMap.get(key);
            return opt.isDefined() ? opt.get() : null;
        }
        @Override
        public void put(K key, V value) {
            mMap = mMap.updated(key, value);
        }
        @Override
        public void reset() {
            mMap = mEmpty;
        }
        @Override
        public String toString() {
            return mMap.getClass().getName();
        }
    }

    public static final MapTester<String, Integer> TEST_DOUG = new PersistentMapTester<String, Integer>(HashTrieMap.<String, Integer> empty());
    public static final MapTester<String, Integer> TEST_JAVA = new JavaHashMapTester<String, Integer>();
    public static final MapTester<String, Integer> TEST_JAVA_TREE = new JavaTreeMapTester<String, Integer>();
    public static final MapTester<String, Integer> TEST_CLOJURE = new ClojureIPersistentMapTester<String, Integer>();
    public static final MapTester<String, Integer> TEST_SCALA = new ScalaImmutableMapTester<String, Integer>(scala.collection.immutable.HashMap$.MODULE$.<String, Integer> empty());
    public static List<Object[]> TEST_MAPS = Arrays.asList(new Object[][]{
            {TEST_DOUG},
            {TEST_JAVA},
            {TEST_JAVA_TREE},
            {TEST_CLOJURE},
            {TEST_SCALA}
    });

    @RunWith(Parameterized.class)
    public static class Memory {
        private static final int PRIME = 61;

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return TEST_MAPS;
        }
        private final MapTester<String, Integer> mTester;
        public Memory(MapTester<String, Integer> tester) {
            mTester = tester;
        }

        //@Test
        public void dump() {
            mTester.reset();
            for (int i = 0; i < 10; ++i) {
                mTester.put(Integer.toHexString(PRIME * i), i);
            }
            objectexplorer.ObjectExplorer.exploreObject(mTester, new ObjectVisitor<Object>() {
                private IdentityHashMap<Object, Boolean> mExplored = new IdentityHashMap<Object, Boolean>();
                @Override
                public Traversal visit(Chain chain) {
                    if (mExplored.containsKey(chain.getValue())) {
                        return Traversal.SKIP;
                    } else {
                        System.out.println("Visit " + chain.getValueType());
                        mExplored.put(chain.getValue(), null);
                        return Traversal.EXPLORE;
                    }
                }
                @Override
                public Object result() {
                    return null;
                }
            });
        }

        @Test
        public void measureMemory() {
            System.out.println(String.format("Size,%s", mTester));
            for (int size = 1; size <= 1000000; size *= 10) {
                mTester.reset();
                for (int i = 0; i < size; ++i) {
                    mTester.put(Integer.toHexString(PRIME * i), i);
                }
                long nbytes = MemoryMeasurer.measureBytes(mTester);
                System.out.println(String.format("%d,%d", size, nbytes));
            }
        }

        private static long getMemory() {
            Runtime runtime = Runtime.getRuntime();
            for (int i = 0; i < 4; ++i) { runtime.gc(); }
            return runtime.totalMemory() - runtime.freeMemory();
        }

        @Test
        public void measureGc() {
            for (int size = 1; size <= 1000000; size *= 10) {
                mTester.reset();
                for (int i = 0; i < size; ++i) {
                    mTester.put(Integer.toHexString(PRIME * i), i);
                }
                long usageWith = getMemory();
                mTester.reset();
                long usageWithout = getMemory();
                System.out.println(String.format("   Heap %s: %d", mTester, usageWith - usageWithout));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class Performance {
        private static final int PRIME = 61;

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return TEST_MAPS;
        }
        private final MapTester<String, Integer> mTester;
        public Performance(MapTester<String, Integer> tester) {
            mTester = tester;
        }

        private void run(int runs, int size, boolean query, boolean miss) {
            for (int n = 0; n < runs; ++n) {
                mTester.reset();
                for (int i = 0; i < size; ++i) {
                    mTester.put(Integer.toHexString(PRIME * i), i + n);
                }
                if (query) {
                    for (int i = 0; i < size; ++i) {
                        assertThat(mTester.get(Integer.toHexString(PRIME * i)), equalTo(i + n));
                    }
                }
                if (miss) {
                    for (int i = 0; i < size; ++i) {
                        assertThat(mTester.get(Integer.toHexString(PRIME * i + 1)), nullValue());
                    }
                }
            }
        }

        @Test
        public void profile() {
            System.out.println(Joiner.on(',').join("Size", mTester)); // mTester + " insert", mTester + "insert+query", mTester + "insert+miss"
            for (int size = 10; size <= 1000000; size *= 10) {
                int runs = 100000000 / size;

                long t0 = System.nanoTime();
                run(runs, size, false, false);
                long t1 = System.nanoTime();
//                run(runs, size, true, false);
//                long t2 = System.nanoTime();
//                run(runs, size, false, false);
//                long t3 = System.nanoTime();

                double div = 1.0E3 * runs * size;
                System.out.println(Joiner.on(',').join(size, (t1 - t0) / div)); // , (t2 - t1) / div, (t3 - t2) / div
            }
        }
    }
}
