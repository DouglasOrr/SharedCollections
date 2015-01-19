package com.dorr.persistent;

import com.google.common.base.Joiner;
import objectexplorer.Chain;
import objectexplorer.MemoryMeasurer;
import objectexplorer.ObjectVisitor;

import java.util.*;

public class Comparisons {
    public static final MapTester<String, Integer> TEST_DOUG = new MapTester.PersistentMapTester<String, Integer>(HashTrieMap.<String, Integer> empty());
    public static final MapTester<String, Integer> TEST_JAVA = new MapTester.JavaHashMapTester<String, Integer>();
    public static final MapTester<String, Integer> TEST_JAVA_TREE = new MapTester.JavaTreeMapTester<String, Integer>();
    public static final MapTester<String, Integer> TEST_CLOJURE = new MapTester.ClojureIPersistentMapTester<String, Integer>();
    public static final MapTester<String, Integer> TEST_SCALA = new MapTester.ScalaImmutableMapTester<String, Integer>(scala.collection.immutable.HashMap$.MODULE$.<String, Integer> empty());
    public static List<MapTester<String, Integer>> TEST_MAPS = Arrays.asList(
            TEST_DOUG,
            TEST_JAVA,
            TEST_JAVA_TREE,
            TEST_CLOJURE,
            TEST_SCALA
    );

    public static class Memory {
        private static final int PRIME = 61;

        @SuppressWarnings("unused")
        public static void dump(Object root) {
            objectexplorer.ObjectExplorer.exploreObject(root, new ObjectVisitor<Object>() {
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

        private static long getMemory() {
            Runtime runtime = Runtime.getRuntime();
            for (int i = 0; i < 4; ++i) { runtime.gc(); }
            return runtime.totalMemory() - runtime.freeMemory();
        }

        public static void profile(MapTester<String, Integer> tester) {
            System.out.println(Joiner.on(",").join("Size", tester + " (measured)", tester + " (heap)"));
            for (int size = 1; size <= 1000000; size *= 10) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.put(Integer.toHexString(PRIME * i), i);
                }
                long measuredBytes = MemoryMeasurer.measureBytes(tester);
                long usageWith = getMemory();
                tester.reset();
                long heapDelta = usageWith - getMemory();

                System.out.println(Joiner.on(',').join(size, measuredBytes, heapDelta));
            }
        }

        /**
         * Run the memory tests.
         * <p>These are not considered part of the unit test suite - as they need to be run
         * in a controlled environment.</p>
         */
        public static void main(String[] args) {
            for (MapTester<String, Integer> tester : TEST_MAPS) {
                profile(tester);
            }
        }
    }

    public static class Performance {
        private static final int PRIME = 61;

        private static void runInserts(MapTester<String, Integer> tester, int runs, int size) {
            for (int n = 0; n < runs; ++n) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.put(Integer.toHexString(PRIME * i), i + n);
                }
            }
        }

        public static void profile(MapTester<String, Integer> tester) {
            System.out.println(Joiner.on(',').join("Size", tester));
            for (int size = 10; size <= 1000000; size *= 10) {
                int runs = 100000000 / size;

                long t0 = System.nanoTime();
                runInserts(tester, runs, size);
                long t1 = System.nanoTime();

                System.out.println(Joiner.on(',').join(size, (t1 - t0) / (1.0E3 * runs * size)));
            }
        }

        /**
         * Run the performance tests.
         * <p>These are not considered part of the unit test suite - as they need to be run
         * in a controlled environment, and are slow.</p>
         */
        public static void main(String[] args) {
            for (MapTester<String, Integer> tester : TEST_MAPS) {
                profile(tester);
            }
        }
    }
}
