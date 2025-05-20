package org;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadWithoutThreadPoolWordCount {
    // Configurable parameters for scalability analysis
    static final int MAX_PAGES = 100000;
    static final String FILE_NAME = System.getProperty("fileName", "enwiki.xml");
    static int THREAD_COUNT = System.getProperty("threadCount") != null
            ? Integer.parseInt(System.getProperty("threadCount"))
            : Runtime.getRuntime().availableProcessors();

    // Shared queue for pages
    private static final LinkedList<Page> pageQueue = new LinkedList<>();
    // Lock object for synchronizing access to the queue
    private static final Object queueLock = new Object();
    // Flag to signal when the producer has finished
    private static final AtomicBoolean producerDone = new AtomicBoolean(false);
    // Counter for the number of processed pages
    private static final AtomicInteger processedPages = new AtomicInteger(0);
    // A list to collect local counts from each consumer thread
    private static final List<Map<String, Integer>> consumerLocalCounts = Collections
            .synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        System.out.println("-----------------------------------------------------");
        System.out.println("=== Multi Thread Without ThreadPool ===");
        System.out.printf("Processing file: %s%n", FILE_NAME);
        System.out.printf("Number of threads: %d%n", THREAD_COUNT);
        System.out.println("-----------------------------------------------------");

        // Record start time
        long start = System.currentTimeMillis();

        // Create and start the producer thread
        Thread producer = new Thread(new Producer());
        producer.start();

        // Create and start consumer threads
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread consumer = new Thread(new Consumer());
            consumer.start();
            consumers.add(consumer);
        }

        // Wait for the producer and consumers to finish
        producer.join();
        for (Thread consumer : consumers) {
            consumer.join();
        }

        // Merge all consumer local counts into a global map
        Map<String, Integer> globalCounts = mergeLocalCounts();

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

        // Sort and print the top 3 most common words
        globalCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .forEach(entry -> System.out
                        .println("Word: '" + entry.getKey() + "' with total " + entry.getValue() + " occurrences!"));
    }

    // Merge local count maps from each consumer into one global map
    private static Map<String, Integer> mergeLocalCounts() {
        Map<String, Integer> globalCounts = new HashMap<>();
        for (Map<String, Integer> localCounts : consumerLocalCounts) {
            for (Map.Entry<String, Integer> entry : localCounts.entrySet()) {
                globalCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return globalCounts;
    }

    // Producer: reads pages from the XML file and enqueues them
    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
                for (Page page : pages) {
                    if (page == null) {
                        break;
                    }
                    synchronized (queueLock) {
                        pageQueue.add(page);
                        queueLock.notifyAll(); // Notify waiting consumers
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                producerDone.set(true);
                synchronized (queueLock) {
                    queueLock.notifyAll(); // Wake up waiting consumers on completion
                }
            }
        }
    }

    // Consumer: dequeues pages, tokenizes text, and counts words
    static class Consumer implements Runnable {
        @Override
        public void run() {
            // Each consumer maintains its own local word count map
            Map<String, Integer> localCounts = new HashMap<>();
            while (true) {
                Page page;
                synchronized (queueLock) {
                    // Wait while the queue is empty and the producer hasn't finished
                    while (pageQueue.isEmpty() && !producerDone.get()) {
                        try {
                            queueLock.wait(); // Wait for pages to be available
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    // If the queue is empty and the producer is done, exit
                    if (pageQueue.isEmpty() && producerDone.get()) {
                        break;
                    }

                    page = pageQueue.removeFirst();
                }

                if (page != null) {
                    processPage(page, localCounts);
                    processedPages.incrementAndGet();
                }
            }
            // After processing, store the local counts in the shared list
            consumerLocalCounts.add(localCounts);
        }

        private void processPage(Page page, Map<String, Integer> localCounts) {
            // Tokenize the page's text using the Words iterator
            Iterable<String> words = new Words(page.getText());
            for (String word : words) {
                // Filter words: count those longer than one character or if it's "a" or "I"
                if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                    localCounts.merge(word, 1, Integer::sum);
                }
            }
        }
    }
}
