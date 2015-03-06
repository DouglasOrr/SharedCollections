package com.github.douglasorr.shared;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import objectexplorer.Chain;
import objectexplorer.MemoryMeasurer;
import objectexplorer.ObjectVisitor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class Profile {
    // Types of maps & arrays to run our tests over

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

    // Test infrastructure

    public static abstract class Measurement {
        public final long size;
        Measurement(long size) {
            this.size = size;
        }
        public abstract String csvHeader();
        public abstract String csvRow();
    }
    public static class MemoryMeasurement extends Measurement {
        public final long heap_bytes;
        public final long measured_bytes;
        public MemoryMeasurement(long size, long heap_bytes, long measured_bytes) {
            super(size);
            this.heap_bytes = heap_bytes;
            this.measured_bytes = measured_bytes;
        }
        @Override
        public String toString() {
            return String.format("size: %d, heap: %.1e, measured: %.1e", size, (double) heap_bytes, (double) measured_bytes);
        }
        @Override
        public String csvHeader() {
            return "size,heap_bytes,measured_bytes";
        }
        @Override
        public String csvRow() {
            return Joiner.on(",").join(size, heap_bytes, measured_bytes);
        }
    }
    public static class LatencyMeasurement extends Measurement {
        private final long repetitions;
        private final long latency_ns;
        public LatencyMeasurement(long size, long repetitions, long latency_ns) {
            super(size);
            this.repetitions = repetitions;
            this.latency_ns = latency_ns;
        }
        @Override
        public String toString() {
            return String.format("size: %d, time: %.1e ns", size, latency_ns / (double) repetitions);
        }
        @Override
        public String csvHeader() {
            return "size,repetitions,latency_ns";
        }
        @Override
        public String csvRow() {
            return Joiner.on(",").join(size, repetitions, latency_ns);
        }
    }

    /** A set of performance/memory tests that produce some measurements. */
    public static abstract class Test<T extends Tester> {
        private final List<Integer> mSizes;
        protected Test(List<Integer> sizes) {
            mSizes = sizes;
        }
        public List<Measurement> run(T tester) {
            List<Measurement> results = new ArrayList<Measurement>();
            try {
                for (int size : mSizes) {
                    tester.reset();
                    results.add(runSingle(tester, size));
                }
            } catch (UnsupportedOperationException e) {
                // ignore, for now - we just can't measure this
            } finally {
                tester.reset();
            }
            return results;
        }
        protected abstract Measurement runSingle(T tester, int size);
    }

    // Memory tests

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
        for (int i = 0; i < 8; ++i) { runtime.gc(); }
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /** A geometric series within an inclusive range [lo hi]. */
    private static List<Integer> geometricSeries(int lo, int hi, int step) {
        List<Integer> result = new ArrayList<Integer>();
        for (int size = lo; size <= hi; size *= step) {
            result.add(size);
        }
        return result;
    }

    private static final int SMALL_PRIME = 61;

    public static abstract class MemoryTest<T extends Tester> extends Test<T> {
        public MemoryTest() {
            super(geometricSeries(1, (int) 1E6, 10));
        }
        @Override
        protected Measurement runSingle(T tester, int size) {
            tester.reset();
            fill(tester, size);

            long measuredBytes = MemoryMeasurer.measureBytes(tester);
            long usageWith = getMemory();
            tester.reset();
            long heapDelta = usageWith - getMemory();

            return new MemoryMeasurement(size, heapDelta, measuredBytes);
        }
        protected abstract void fill(T tester, int size);
    }

    private static final Test<ArrayTester<Integer>> MEASURE_ARRAY = new MemoryTest<ArrayTester<Integer>>() {
        @Override
        protected void fill(ArrayTester<Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.add(i);
            }
        }
        @Override
        public String toString() {
            return "Array.memory";
        }
    };

    private static final Test<MapTester<String, Integer>> MEASURE_MAP = new MemoryTest<MapTester<String, Integer>>() {
        @Override
        protected void fill(MapTester<String, Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.put(Integer.toHexString(SMALL_PRIME * i), i);
            }
        }
        @Override
        public String toString() {
            return "Map.memory";
        }
    };

    // Latency tests

    private static abstract class LatencyTest<T extends Tester> extends Test<T> {
        private final int mOpsPerSize;
        public LatencyTest(int maxSize, int opsPerSize) {
            super(geometricSeries(10, maxSize, 10));
            mOpsPerSize = opsPerSize;
        }
        @Override
        protected Measurement runSingle(T tester, int size) {
            int runs = Math.max(mOpsPerSize / size, 100);

            tester.reset();
            prepare(tester, size);
            System.gc(); System.gc();
            long t0 = System.nanoTime();
            for (int i = 0; i < runs; ++i) {
                execute(tester, size);
            }
            long t1 = System.nanoTime();

            return new LatencyMeasurement(size, runs, t1 - t0);
        }
        protected void prepare(T tester, int size) { }
        protected abstract void execute(T tester, int size);
    }

    private static final Test<MapTester<String, Integer>> RUN_MAP_PUT
            = new LatencyTest<MapTester<String, Integer>>((int) 1E6, (int) 1E8) {
        @Override
        protected void execute(MapTester<String, Integer> tester, int size) {
            tester.reset();
            for (int i = 0; i < size; ++i) {
                tester.put(Integer.toHexString(SMALL_PRIME * i), i);
            }
        }
        @Override
        public String toString() {
            return "Map.put";
        }
    };

    public static final Test<MapTester<String, Integer>> RUN_MAP_GET
            = new LatencyTest<MapTester<String,Integer>>((int) 1E6, (int) 1E8) {
        @Override
        public void prepare(MapTester<String, Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.put(Integer.toHexString(SMALL_PRIME * i), i);
            }
        }
        @Override
        public void execute(MapTester<String, Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.get(Integer.toHexString(SMALL_PRIME * i));
            }
        }
        @Override
        public String toString() {
            return "Map.get";
        }
    };

    public static final Test<MapTester<String, Integer>> RUN_MAP_ITERATE
            = new LatencyTest<MapTester<String, Integer>>((int) 1E7, (int) 1E8) {
        @Override
        public void prepare(MapTester<String, Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.put(Integer.toHexString(SMALL_PRIME * i), i);
            }
        }
        @Override
        public void execute(MapTester<String, Integer> tester, int size) {
            Iterator<Map.Entry<String, Integer>> it = tester.iterator();
            while (it.hasNext()) {
                it.next();
            }
        }
        @Override
        public String toString() {
            return "Map.iterate";
        }
    };

    public static final Test<ArrayTester<Integer>> RUN_ARRAY_ADD
            = new LatencyTest<ArrayTester<Integer>>((int) 1E7, (int) 1E8) {
        @Override
        public void execute(ArrayTester<Integer> tester, int size) {
            tester.reset();
            for (int i = 0; i < size; ++i) {
                tester.add(i);
            }
        }
        @Override
        public String toString() {
            return "Array.add";
        }
    };

    public static final Test<ArrayTester<Integer>> RUN_ARRAY_ADD_FIRST
            = new LatencyTest<ArrayTester<Integer>>((int) 1E4, (int) 1E6) {
        @Override
        public void execute(ArrayTester<Integer> tester, int size) {
            tester.reset();
            for (int i = 0; i < size; ++i) {
                tester.add(0, i);
            }
        }
        @Override
        public String toString() {
            return "Array.addFirst";
        }
    };

    public static final Test<ArrayTester<Integer>> RUN_ARRAY_ADD_MID
            = new LatencyTest<ArrayTester<Integer>>((int) 1E4, (int) 1E5) {
        @Override
        public void execute(ArrayTester<Integer> tester, int size) {
            tester.reset();
            for (int i = 0; i < size; ++i) {
                tester.add(i / 2, i);
            }
        }
        @Override
        public String toString() {
            return "Array.add";
        }
    };

    public static final Test<ArrayTester<Integer>> RUN_ARRAY_UPDATE
            = new LatencyTest<ArrayTester<Integer>>((int) 1E7, (int) 1E8) {
        @Override
        public void prepare(ArrayTester<Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.add(i);
            }
        }
        @Override
        public void execute(ArrayTester<Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.set(i, -i);
            }
        }
        @Override
        public String toString() {
            return "Array.update";
        }
    };

    public static final Test<ArrayTester<Integer>> RUN_ARRAY_REMOVE
            = new LatencyTest<ArrayTester<Integer>>((int) 1E7, (int) 1E8) {
        @Override
        public void execute(ArrayTester<Integer> tester, int size) {
            tester.reset();
            for (int i = 0; i < size; ++i) {
                tester.add(i);
            }
            for (int i = 0; i < size; ++i) {
                tester.remove();
            }
        }
        @Override
        public String toString() {
            return "Array.remove";
        }
    };

    public static final Test<ArrayTester<Integer>> RUN_ARRAY_ITERATE
            = new LatencyTest<ArrayTester<Integer>>((int) 1E7, (int) 1E9) {
        @Override
        public void prepare(ArrayTester<Integer> tester, int size) {
            for (int i = 0; i < size; ++i) {
                tester.add(i);
            }
        }
        @Override
        public void execute(ArrayTester<Integer> tester, int size) {
            Iterator<Integer> it = tester.iterator();
            while (it.hasNext()) {
                it.next();
            }
        }
        @Override
        public String toString() {
            return "Array.iterate";
        }
    };

    public static final List<Test<ArrayTester<Integer>>> ARRAY_TESTS = asList(
            MEASURE_ARRAY, RUN_ARRAY_ADD, RUN_ARRAY_ADD_FIRST, RUN_ARRAY_ADD_MID,
            RUN_ARRAY_ITERATE, RUN_ARRAY_REMOVE, RUN_ARRAY_REMOVE, RUN_ARRAY_UPDATE);

    public static final List<Test<MapTester<String, Integer>>> MAP_TESTS = asList(
            MEASURE_MAP, RUN_MAP_GET, RUN_MAP_ITERATE, RUN_MAP_PUT
    );

    private static class TestRun<T extends Tester> {
        final Test<T> test;
        final T tester;
        public TestRun(Test<T> test, T tester) {
            this.test = test;
            this.tester = tester;
        }
        public List<Measurement> run() {
            return test.run(tester);
        }
        @Override
        public String toString() {
            return test + "/" + tester;
        }
    }
    private static List<TestRun> testRuns() {
        List<TestRun> runs = new ArrayList<TestRun>();
        for (final Test<ArrayTester<Integer>> test : ARRAY_TESTS) {
            for (final ArrayTester<Integer> tester : TEST_ARRAYS) {
                runs.add(new TestRun<ArrayTester<Integer>>(test, tester));
            }
        }
        for (final Test<MapTester<String, Integer>> test : MAP_TESTS) {
            for (final MapTester<String, Integer> tester : TEST_MAPS) {
                runs.add(new TestRun<MapTester<String, Integer>>(test, tester));
            }
        }
        return runs;
    }

    public static class EnvironmentReport {
        private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'") {{
            setTimeZone(TimeZone.getTimeZone("UTC"));
        }};
        private static final Pattern CPU_MODEL_PATTERN = Pattern.compile("model name\\s*:\\s+(.+)\\n");
        private static List<String> getCpus() {
            List<String> cpus = new ArrayList<String>();
            try {
                Matcher matcher = CPU_MODEL_PATTERN.matcher(Files.toString(new File("/proc/cpuinfo"), Charset.defaultCharset()));
                while (matcher.find()) {
                    cpus.add(matcher.group(1));
                }
                if (cpus.isEmpty()) {
                    cpus.add("unknown (couldn't parse /proc/cpuinfo)");
                }
            } catch (IOException e) {
                cpus.add("unknown (couldn't open /proc/cpuinfo)");
            }
            return cpus;
        }

        public final String start_time;
        public final String java_version, java_vm_name;
        public final String os_name, os_version, os_arch;
        public final List<String> cpus;
        public final long max_memory;

        public EnvironmentReport() {
            start_time = DATETIME_FORMAT.format(new Date());
            java_version = System.getProperty("java.version");
            java_vm_name = System.getProperty("java.vm.name");
            os_name = System.getProperty("os.name");
            os_version = System.getProperty("os.version");
            os_arch = System.getProperty("os.arch");
            max_memory = Runtime.getRuntime().maxMemory();
            cpus = getCpus();
        }

        @Override
        public String toString() {
            return String.format("start_time: %s, java.version: %s, java.vm.name: %s, os.name: %s, os.version: %s, os.arch: %s, max_memory: %d, cpus: %s",
                    start_time, java_version, java_vm_name, os_name, os_version, os_arch, max_memory, cpus);
        }
        public String csvHeader() {
            return "start_time,java.version,java.vm.name,os.name,os.version,os.arch,max_memory,cpus";
        }
        public String csvRow() {
            return "\"" + Joiner.on("\",\"").join(start_time, java_version, java_vm_name, os_name, os_version, os_arch, max_memory, cpus) + "\"";
        }
    }

    private interface Output extends Closeable {
        void add(TestRun run, List<Measurement> measurements);
    }
    public static class ConsoleOutput implements Output {
        public ConsoleOutput(EnvironmentReport report) {
            System.out.println(report);
            System.out.println("----------");
        }
        @Override
        public void add(TestRun run, List<Measurement> measurements) {
            System.out.println(run);
            for (Measurement measurement : measurements) {
                System.out.println("\t" + measurement);
            }
            System.out.flush();
        }
        @Override
        public void close() throws IOException { }
    }
    public static class JsonOutput implements Output {
        private final File mPath;
        private final EnvironmentReport mEnvironmentReport;
        private final Map<String, Map<String, List<Measurement>>> mMeasurements
                = new HashMap<String, Map<String, List<Measurement>>>();

        public JsonOutput(File filePath, EnvironmentReport report) {
            mPath = filePath;
            mEnvironmentReport = report;
        }
        @Override
        public void add(TestRun run, List<Measurement> measurements) {
            String testName = run.test.toString();
            Map<String, List<Measurement>> testMeasurements = mMeasurements.get(testName);
            if (testMeasurements == null) {
                testMeasurements = new HashMap<String, List<Measurement>>();
                mMeasurements.put(testName, testMeasurements);
            }
            String testerName = run.tester.toString();
            testMeasurements.put(testerName, measurements);
        }
        @Override
        public void close() throws IOException {
            Files.createParentDirs(mPath);
            FileWriter writer = new FileWriter(mPath);
            try {
                Map<String, Object> root = new HashMap<String, Object>();
                root.put("environment", mEnvironmentReport);
                root.put("profile", mMeasurements);
                new Gson().toJson(root, writer);
            } finally {
                writer.close();
            }
        }
    }
    public static class CsvOutput implements Output {
        private final PrintWriter mWriter;
        public CsvOutput(File filePath, EnvironmentReport report) throws IOException {
            Files.createParentDirs(filePath);
            mWriter = new PrintWriter(filePath);
            mWriter.println(report.csvHeader());
            mWriter.println(report.csvRow());
            mWriter.println();
        }
        @Override
        public void add(TestRun run, List<Measurement> measurements) {
            mWriter.println(run);
            mWriter.println(measurements.isEmpty() ? "No data" : measurements.get(0).csvHeader());
            for (Measurement measurement : measurements) {
                mWriter.println(measurement.csvRow());
            }
            mWriter.println();
            mWriter.flush();
        }
        @Override
        public void close() throws IOException {
            mWriter.close();
        }
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("list", false, "List the available tests");
        options.addOption("json", true, "Write a JSON report of the tests out to the given file");
        options.addOption("csv", true, "Write a CSV report of the tests out to the given file");

        try {
            CommandLine commandLine = new PosixParser().parse(options, args);
            if (commandLine.hasOption("list")) {
                for (TestRun run : testRuns()) {
                    System.out.println(run);
                }

            } else {
                List<Pattern> patterns = new ArrayList<Pattern>();
                for (String arg : commandLine.getArgs()) {
                    patterns.add(Pattern.compile(arg));
                }
                EnvironmentReport env = new EnvironmentReport();
                List<Output> outputs = new ArrayList<Output>();
                outputs.add(new ConsoleOutput(env));
                if (commandLine.hasOption("json")) {
                    outputs.add(new JsonOutput(new File(commandLine.getOptionValue("json")), env));
                }
                if (commandLine.hasOption("csv")) {
                    outputs.add(new CsvOutput(new File(commandLine.getOptionValue("csv")), env));
                }

                for (TestRun<?> run : testRuns()) {
                    boolean shouldRun = patterns.isEmpty();
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(run.toString()).matches()) {
                            shouldRun = true;
                            break;
                        }
                    }
                    if (shouldRun) {
                        List<Measurement> measurements = run.run();
                        for (Output output : outputs) {
                            output.add(run, measurements);
                        }
                    }
                }

                for (Output output : outputs) {
                    output.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();

        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
    }
}
