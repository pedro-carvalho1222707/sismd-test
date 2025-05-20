package org;

import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * GCBenchmarkRunner runs different word-count strategies under various GC configurations.
 * It captures GC metrics (collection count and time) before and after each run, and prints
 * a comparative summary of execution time and GC statistics for each strategy.
 */
public class GCBenchmarkRunner {
    public static void main(String[] args) {
        // Gather GC beans and system info
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Set<String> gcNames = new HashSet<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            gcNames.add(gc.getName());
        }
        int processors = Runtime.getRuntime().availableProcessors();
        long maxHeapBytes = Runtime.getRuntime().maxMemory();
        long maxHeapMB = maxHeapBytes / (1024 * 1024);

        // Print header with GC, CPU, and memory info
        System.out.println("=== GCBenchmarkRunner ===");
        System.out.printf("GC in use: %s%n", String.join(", ", gcNames));
        System.out.printf("Available processors: %-2d | Max heap: %-4d MB%n", processors, maxHeapMB);
        System.out.println("-----------------------------------------------------");
        System.out.println("Running benchmarks...");

        // Run benchmarks for each strategy
        runBenchmark("Sequential", () -> {
            try {
                WordCount.main(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        runBenchmark("WithoutThreadPool", () -> {
            try {
                MultiThreadWithoutThreadPoolWordCount.main(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        runBenchmark("WithThreadPool", () -> {
            try {
                MultiThreadWithThreadPoolWordCount.main(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        runBenchmark("ForkJoin", () -> {
            try {
                ForkJoinWordCount.main(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        runBenchmark("CompletableFuture", () -> {
            try {
                CompletableFutureWordCount.main(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Runs a single strategy, measuring GC before/after and execution time.
     * @param name Name of the strategy (for reporting)
     * @param strategy A Runnable that executes the word-count strategy
     */
    private static void runBenchmark(String name, Runnable strategy) {
        // Snapshot GC metrics before execution
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalCountBefore = 0;
        long totalTimeBefore = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            totalCountBefore += (count < 0 ? 0 : count);
            totalTimeBefore += (time < 0 ? 0 : time);
        }

        long startTime = System.currentTimeMillis();
        // Run the strategy
        try {
            strategy.run();
        } catch (Throwable t) {
            System.err.println("Error running strategy " + name);
            t.printStackTrace();
        }
        long elapsed = System.currentTimeMillis() - startTime;

        // Snapshot GC metrics after execution
        long totalCountAfter = 0;
        long totalTimeAfter = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            totalCountAfter += (count < 0 ? 0 : count);
            totalTimeAfter += (time < 0 ? 0 : time);
        }

        // Calculate deltas
        long gcCountDelta = totalCountAfter - totalCountBefore;
        long gcTimeDelta = totalTimeAfter - totalTimeBefore;

        // Print header for strategy results
        System.out.printf("%-20s %12s %12s %12s%n", "Strategy", "Time(ms)", "GC Count", "GC Time(ms)");
        // Print data row
        System.out.printf("%-20s %12d %12d %12d%n", name, elapsed, gcCountDelta, gcTimeDelta);
    }
}
