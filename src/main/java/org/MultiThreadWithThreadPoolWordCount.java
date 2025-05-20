package org;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MultiThreadWithThreadPoolWordCount {
    static final int MAX_PAGES = 100000;
    static final String FILE_NAME = System.getProperty("fileName", "enwiki.xml");
    static int THREAD_COUNT = System.getProperty("threadCount") != null
            ? Integer.parseInt(System.getProperty("threadCount"))
            : Runtime.getRuntime().availableProcessors();

    // Use a thread-safe collection for counting words
    private static final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

    // Executor service for managing thread pool
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

    public static void main(String[] args) {
        System.out.println("-----------------------------------------------------");
        System.out.println("=== Multi Thread With ThreadPool ===");
        System.out.printf("Processing file: %s%n", FILE_NAME);
        System.out.printf("Number of threads: %d%n", THREAD_COUNT);
        System.out.println("-----------------------------------------------------");

        long start = System.currentTimeMillis();
        Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
        AtomicInteger processedPages = new AtomicInteger(0); // Thread-safe counter

        // Submit tasks to the thread pool for processing pages
        for (Page page : pages) {
            executorService.submit(() -> {
                // Count words for the page
                Iterable<String> words = new Words(page.getText());
                for (String word : words) {
                    if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                        countWord(word);
                    }
                }
                processedPages.incrementAndGet();
            });
        }

        // Wait for all tasks to complete
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        // Record end time and calculate execution time
        long end = System.currentTimeMillis();
        long executionTime = end - start;
        System.out.println("Execution Time: " + executionTime + " ms");

        // Record memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        System.out.println("Heap Memory Used: " + heapUsage.getUsed() + " bytes");
    
        // Record CPU usage
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double processCpuLoad = osBean.getSystemLoadAverage();
        System.out.println("Process CPU Load: " + processCpuLoad * 100 + "%");

        // Report number of processed pages.
        System.out.println("Processed Pages: " + processedPages.get());

        // Sorting and displaying the most common words
        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out
                        .println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }

    // Thread-safe word counting
    private static void countWord(String word) {
        counts.merge(word, 1, Integer::sum);
    }
}
