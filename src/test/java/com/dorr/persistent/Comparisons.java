package com.dorr.persistent;

import clojure.lang.PersistentVector;
import com.google.common.base.Joiner;
import objectexplorer.Chain;
import objectexplorer.MemoryMeasurer;
import objectexplorer.ObjectVisitor;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

public class Comparisons {
    public static final List<MapTester<String, Integer>> TEST_MAPS = Arrays.asList(
            new MapTester.PersistentMapTester<String, Integer>(HashTrieMap.<String, Integer> empty()),
            new MapTester.JavaHashMapTester<String, Integer>(),
            new MapTester.JavaTreeMapTester<String, Integer>(),
            new MapTester.ClojureIPersistentMapTester<String, Integer>(clojure.lang.PersistentHashMap.create()),
            new MapTester.ScalaImmutableMapTester<String, Integer>(scala.collection.immutable.HashMap$.MODULE$.<String, Integer> empty())
    );

    public static final List<ArrayTester<Integer>> TEST_ARRAYS = Arrays.asList(
            new ArrayTester.PersistentArrayTester<Integer>(TrieArray.<Integer> empty()),
            new ArrayTester.JavaArrayListTester<Integer>(),
            new ArrayTester.ClojureVectorTester<Integer>(clojure.lang.PersistentVector.create())
           // new ArrayTester.ScalaIndexedSeqTester<Integer>(scala.collection.immutable.Vector$.MODULE$.<Integer> empty())
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

        private static abstract class Fill {
            public abstract void fill(int size);
        }

        public static void profile(Tester tester, Fill fill) {
            System.out.println(Joiner.on(",").join("Size", tester + " (measured)", tester + " (heap)"));
            for (int size = 1; size <= 1000000; size *= 10) {
                tester.reset();
                fill.fill(size);
                long measuredBytes = MemoryMeasurer.measureBytes(tester);
                long usageWith = getMemory();
                tester.reset();
                long heapDelta = usageWith - getMemory();

                System.out.println(Joiner.on(',').join(size, measuredBytes, heapDelta));
            }
        }
        public static void profileMap(final MapTester<String, Integer> tester) {
            profile(tester, new Fill() {
                @Override
                public void fill(int size) {
                    for (int i = 0; i < size; ++i) {
                        tester.put(Integer.toHexString(PRIME * i), i);
                    }
                }
            });
        }
        public static void profileArray(final ArrayTester<Integer> tester) {
            profile(tester, new Fill() {
                @Override
                public void fill(int size) {
                    for (int i = 0; i < size; ++i) {
                        tester.add(i);
                    }
                }
            });
        }

        /**
         * Run the memory tests.
         * <p>These are not considered part of the unit test suite - as they need to be run
         * in a controlled environment.</p>
         */
        public static void main(String[] args) {
            for (MapTester<String, Integer> tester : TEST_MAPS) {
                profileMap(tester);
            }
            for (ArrayTester<Integer> tester : TEST_ARRAYS) {
                profileArray(tester);
            }
        }
    }

    public static class Performance {
        private static final int PRIME = 61;

        private abstract static class Run {
            public abstract void run(int runs, int size);
        }

        private static void profile(Tester tester, int maxSize, int opsPerSize, Run run) {
            System.out.println(Joiner.on(',').join("Size", tester));
            for (int size = 10; size <= maxSize; size *= 10) {
                int runs = Math.max(opsPerSize / size, 100);

                long t0 = System.nanoTime();
                run.run(runs, size);
                long t1 = System.nanoTime();

                System.out.println(Joiner.on(',').join(size, (double)(t1 - t0) / (runs * size)));
            }
        }

        public static void profileMap(final MapTester<String, Integer> tester) {
            profile(tester, (int) 1E6, (int) 1E8, new Run() {
                @Override
                public void run(int runs, int size) {
                    for (int n = 0; n < runs; ++n) {
                        tester.reset();
                        for (int i = 0; i < size; ++i) {
                            tester.put(Integer.toHexString(PRIME * i), i + n);
                        }
                    }
                }
            });
        }

        private static void profileArray(final ArrayTester<Integer> tester) {
            profile(tester, (int) 1E7, (int) 1E8, new Run() {
                @Override
                public void run(int runs, int size) {
                    for (int n = 0; n < runs; ++n) {
                        tester.reset();
                        for (int i = 0; i < size; ++i) {
                            tester.add(i);
                        }
                    }
                }
            });
        }

        private static void headToHead() {
            final int SHORT = 1000, LONG = 100000;
            final int runs = SHORT, size = 100000;

            for (int x = 0; x < 10; ++x) {
                long t0 = System.nanoTime();
                for (int n = 0; n < runs; ++n) {
                    PersistentVector v = PersistentVector.create();
                    for (int i = 0; i < size; ++i) {
                        v = v.cons(i);
                    }
                }
                long t1 = System.nanoTime();
                for (int n = 0; n < runs; ++n) {
                    TrieArray<Integer> a = TrieArray.empty();
                    for (int i = 0; i < size; ++i) {
                        a = a.append(i);
                    }
                }
                long t2 = System.nanoTime();
                double t01 = (t1 - t0) / (double) (runs * size);
                double t12 = (t2 - t1) / (double) (runs * size);
                System.out.println("Doug: " + t12 + " ns");
                System.out.println(" Clj: " + t01 + " ns");
            }
        }

        /**
         * Run the performance tests.
         * <p>These are not considered part of the unit test suite - as they need to be run
         * in a controlled environment, and are slow.</p>
         */
        public static void main(String[] args) {
            for (MapTester<String, Integer> tester : TEST_MAPS) {
                profileMap(tester);
            }
            for (ArrayTester<Integer> tester : TEST_ARRAYS) {
                profileArray(tester);
            }
        }
    }
}
