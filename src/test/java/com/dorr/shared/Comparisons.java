package com.dorr.shared;

import com.google.common.base.Joiner;
import objectexplorer.Chain;
import objectexplorer.MemoryMeasurer;
import objectexplorer.ObjectVisitor;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class Comparisons {
    public static final List<MapTester<String, Integer>> TEST_MAPS = asList(
            new MapTester.SharedMapTester<String, Integer>(HashTrieMap.<String, Integer>empty()),
            new MapTester.JavaHashMapTester<String, Integer>(),
            new MapTester.JavaTreeMapTester<String, Integer>(),
            new MapTester.ClojureIPersistentMapTester<String, Integer>(clojure.lang.PersistentHashMap.create()),
            new MapTester.ScalaImmutableMapTester<String, Integer>(scala.collection.immutable.HashMap$.MODULE$.<String, Integer>empty())
    );

    public static final List<ArrayTester<Integer>> TEST_ARRAYS = asList(
            new ArrayTester.SharedArrayTester<Integer>(TrieArray.<Integer>empty()),
            new ArrayTester.JavaArrayListTester<Integer>(),
            new ArrayTester.ClojureVectorTester<Integer>(clojure.lang.PersistentVector.create())
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

        private abstract static class Run<T extends Tester> {
            public final String name;
            public final int maxSize;
            public final int opsPerSize;
            public Run(String name, int maxSize, int opsPerSize) {
                this.name = name;
                this.maxSize = maxSize;
                this.opsPerSize = opsPerSize;
            }
            @Override
            public String toString() {
                return name;
            }
            public void prepare(T tester, int size) { }
            public abstract void run(T tester, int size);
        }

        private static final Run<MapTester<String, Integer>> RUN_MAP_PUT
                = new Run<MapTester<String, Integer>>("Map.put", (int) 1E6, (int) 1E8) {
            @Override
            public void run(MapTester<String, Integer> tester, int size) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.put(Integer.toHexString(PRIME * i), i);
                }
            }
        };

        public static final Run<MapTester<String, Integer>> RUN_MAP_GET
                = new Run<MapTester<String, Integer>>("Map.get", (int) 1E6, (int) 1E8) {
            @Override
            public void prepare(MapTester<String, Integer> tester, int size) {
                for (int i = 0; i < size; ++i) {
                    tester.put(Integer.toHexString(PRIME * i), i);
                }
            }
            @Override
            public void run(MapTester<String, Integer> tester, int size) {
                for (int i = 0; i < size; ++i) {
                    tester.get(Integer.toHexString(PRIME * i));
                }
            }
        };

        public static final Run<MapTester<String, Integer>> RUN_MAP_ITERATE
                = new Run<MapTester<String, Integer>>("Map.iterate", (int) 1E7, (int) 1E8) {
            @Override
            public void prepare(MapTester<String, Integer> tester, int size) {
                for (int i = 0; i < size; ++i) {
                    tester.put(Integer.toHexString(PRIME * i), i);
                }
            }
            @Override
            public void run(MapTester<String, Integer> tester, int size) {
                Iterator<Map.Entry<String, Integer>> it = tester.iterator();
                while (it.hasNext()) {
                    it.next();
                }
            }
        };

        public static final Run<ArrayTester<Integer>> RUN_ARRAY_ADD
                = new Run<ArrayTester<Integer>>("Array.add", (int) 1E7, (int) 1E8) {
            @Override
            public void run(ArrayTester<Integer> tester, int size) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.add(i);
                }
            }
        };

        public static final Run<ArrayTester<Integer>> RUN_ARRAY_ADD_FIRST
                = new Run<ArrayTester<Integer>>("Array.addfirst", (int) 1E4, (int) 1E6) {
            @Override
            public void run(ArrayTester<Integer> tester, int size) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.add(0, i);
                }
            }
        };

        public static final Run<ArrayTester<Integer>> RUN_ARRAY_ADD_MID
                = new Run<ArrayTester<Integer>>("Array.addmid", (int) 1E4, (int) 1E5) {
            @Override
            public void run(ArrayTester<Integer> tester, int size) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.add(i / 2, i);
                }
            }
        };

        public static final Run<ArrayTester<Integer>> RUN_ARRAY_UPDATE
                = new Run<ArrayTester<Integer>>("Array.update", (int) 1E7, (int) 1E8) {
            @Override
            public void prepare(ArrayTester<Integer> tester, int size) {
                for (int i = 0; i < size; ++i) {
                    tester.add(i);
                }
            }
            @Override
            public void run(ArrayTester<Integer> tester, int size) {
                for (int i = 0; i < size; ++i) {
                    tester.set(i, -i);
                }
            }
        };

        public static final Run<ArrayTester<Integer>> RUN_ARRAY_REMOVE
                = new Run<ArrayTester<Integer>>("Array.remove", (int) 1E7, (int) 1E8) {
            @Override
            public void run(ArrayTester<Integer> tester, int size) {
                tester.reset();
                for (int i = 0; i < size; ++i) {
                    tester.add(i);
                }
                for (int i = 0; i < size; ++i) {
                    tester.remove();
                }
            }
        };

        public static final Run<ArrayTester<Integer>> RUN_ARRAY_ITERATE
                = new Run<ArrayTester<Integer>>("Array.iterate", (int) 1E7, (int) 1E9) {
            @Override
            public void prepare(ArrayTester<Integer> tester, int size) {
                for (int i = 0; i < size; ++i) {
                    tester.add(i);
                }
            }
            @Override
            public void run(ArrayTester<Integer> tester, int size) {
                Iterator<Integer> it = tester.iterator();
                while (it.hasNext()) {
                    it.next();
                }
            }
        };

        private static <T extends Tester> void profile(T tester, Run run) {
            System.out.println(Joiner.on(',').join("Size", run.name + "/" + tester));
            try {
                for (int size = 10; size <= run.maxSize; size *= 10) {
                    int runs = Math.max(run.opsPerSize / size, 100);

                    tester.reset();
                    run.prepare(tester, size);
                    System.gc(); System.gc();
                    long t0 = System.nanoTime();
                    for (int i = 0; i < runs; ++i) {
                        run.run(tester, size);
                    }
                    long t1 = System.nanoTime();

                    System.out.println(Joiner.on(',').join(size, (double) (t1 - t0) / (runs * size)));
                }
            } catch (UnsupportedOperationException e) {
                System.out.println(",not supported");
            } finally {
                tester.reset();
            }
        }

        /**
         * Run the performance tests.
         * <p>These are not considered part of the unit test suite - as they need to be run
         * in a controlled environment, and are slow.</p>
         */
        public static void main(String[] args) {
            String regexFilter = args.length == 0 ? ".+" : args[0];

            for (Run<MapTester<String, Integer>> run : asList(RUN_MAP_PUT, RUN_MAP_GET, RUN_MAP_ITERATE)) {
                for (MapTester<String, Integer> tester : TEST_MAPS) {
                    String name = run.toString() + "/" + tester.toString();
                    if (name.matches(regexFilter)) {
                        profile(tester, run);
                    }
                }
            }
            for (Run<ArrayTester<Integer>> run : asList(
                    RUN_ARRAY_ADD, RUN_ARRAY_ADD_FIRST, RUN_ARRAY_ADD_MID,
                    RUN_ARRAY_UPDATE, RUN_ARRAY_REMOVE, RUN_ARRAY_ITERATE)) {
                for (ArrayTester<Integer> tester : TEST_ARRAYS) {
                    String name = run.toString() + "/" + tester.toString();
                    if (name.matches(regexFilter)) {
                        profile(tester, run);
                    }
                }
            }
        }
    }
}
