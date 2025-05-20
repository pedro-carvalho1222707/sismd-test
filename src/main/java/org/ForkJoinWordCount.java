package org;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class ForkJoinWordCount {
    // Configurable parameters for scalability analysis
    static final int MAX_PAGES = 100000;
    static final String FILE_NAME = System.getProperty("fileName", "enwiki.xml");
    static int THRESHOLD = 1000;

    public static void main(String[] args) {
        System.out.println("-----------------------------------------------------");
        System.out.println("=== Fork Join ===");
        System.out.printf("Processing file: %s%n", FILE_NAME);
        System.out.println("-----------------------------------------------------");

        // Load all pages into a list
        List<Page> pages = new ArrayList<>();
        for (Page page : new Pages(MAX_PAGES, FILE_NAME)) {
            if (page == null) {
                break;
            }
            pages.add(page);
        }

        ForkJoinPool pool = new ForkJoinPool();

        long start = System.currentTimeMillis();
        WordCountTask task = new WordCountTask(pages, 0, pages.size());
        Map<String, Integer> result = pool.invoke(task);
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

        // Sorting and displaying the most common words
        LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
        result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));

        commonWords.entrySet().stream().limit(3).toList()
                .forEach(x -> System.out
                        .println("Word: '" + x.getKey() + "' with total " + x.getValue() + " occurrences!"));
    }

    static class WordCountTask extends RecursiveTask<Map<String, Integer>> {
        private final List<Page> pages;
        private final int start, end;

        public WordCountTask(List<Page> pages, int start, int end) {
            this.pages = pages;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Map<String, Integer> compute() {
            int size = end - start;
            if (size <= THRESHOLD) {
                return processPages();
            } else {
                int mid = start + size / 2;
                WordCountTask leftTask = new WordCountTask(pages, start, mid);
                WordCountTask rightTask = new WordCountTask(pages, mid, end);

                invokeAll(leftTask, rightTask);
                Map<String, Integer> leftResult = leftTask.join();
                Map<String, Integer> rightResult = rightTask.join();

                return mergeCounts(leftResult, rightResult);
            }
        }

        private Map<String, Integer> processPages() {
            Map<String, Integer> counts = new HashMap<>();
            for (int i = start; i < end; i++) {
                for (String word : new Words(pages.get(i).getText())) {
                    if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                        counts.merge(word, 1, Integer::sum);
                    }
                }
            }
            return counts;
        }

        private Map<String, Integer> mergeCounts(Map<String, Integer> left, Map<String, Integer> right) {
            for (Map.Entry<String, Integer> entry : right.entrySet()) {
                left.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            return left;
        }
    }
}
