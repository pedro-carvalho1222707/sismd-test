package org;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Word‑count implementation that uses CompletableFuture to process pages in parallel.
 */
public class CompletableFutureWordCount {

    // Configuration
    private static final int MAX_PAGES = 100000;
    static final String FILE_NAME = System.getProperty("fileName", "enwiki.xml");
    static int THREAD_COUNT = System.getProperty("threadCount") != null
            ? Integer.parseInt(System.getProperty("threadCount"))
            : Runtime.getRuntime().availableProcessors();

    // Shared state
    private static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(THREAD_COUNT);

    private static void countWord(String word) {
        COUNTS.merge(word, 1, Integer::sum); // thread‑safe merge
    }

    public static void main(String[] args) {
        System.out.println("-----------------------------------------------------");
        System.out.println("=== Completable Future   ===");
        System.out.printf("Processing file: %s%n", FILE_NAME);
        System.out.printf("Number of threads: %d%n", THREAD_COUNT);
        System.out.println("-----------------------------------------------------");

        long start = System.currentTimeMillis();   // start timer
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
        AtomicInteger processedPages = new AtomicInteger(0);

        // Launch asynchronous tasks, one per page
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Page page : pages) {
            if (page == null) break;               // end of stream safeguard
            final String text = page.getText();    // make effectively‑final
            CompletableFuture<Void> fut =
                    CompletableFuture.runAsync(() -> {
                        Iterable<String> words = new Words(text);
                        for (String w : words) {
                            if (w.length() > 1 || w.equals("a") || w.equals("I"))
                                countWord(w);
                        }
                        processedPages.incrementAndGet();
                    }, EXECUTOR);
            futures.add(fut);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        EXECUTOR.shutdown();

        long end = System.currentTimeMillis();
        long elapsed = end - start;

        // Metrics
        MemoryUsage heapUsage = memBean.getHeapMemoryUsage();
        long heapUsed = heapUsage.getUsed();

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getProcessCpuLoad() * 100.0;

        // Reporting
        System.out.println("Processed pages: " + processedPages.get());
        System.out.println("Elapsed time: " + elapsed + " ms");
        System.out.println("Heap Memory Used: " + heapUsed + " bytes");
        System.out.printf("CPU Load: %.2f%%\n", cpuLoad);

        // Sort and print top 3 most frequent words
        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        COUNTS.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> commonWords.put(e.getKey(), e.getValue()));

        commonWords.entrySet().stream()
                .limit(3)
                .toList()
                .forEach(e -> System.out.println(
                        "Word: '" + e.getKey() +
                                "' with total " + e.getValue() + " occurrences!"));
    }
}

