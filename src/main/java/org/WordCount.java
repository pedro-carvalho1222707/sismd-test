package org;

import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public class WordCount {
  static final int MAX_PAGES = 100000;
  static final String FILE_NAME = System.getProperty("fileName", "enwiki.xml");

  private static final HashMap<String, Integer> counts = new HashMap<>();

  public static void main(String[] args) {
    System.out.println("-----------------------------------------------------");
    System.out.println("=== Word Count Benchmark ===");
    System.out.printf("Processing file: %s%n", FILE_NAME);
    System.out.println("-----------------------------------------------------");

    long start = System.currentTimeMillis();
    Iterable<Page> pages = new Pages(MAX_PAGES, FILE_NAME);
    int processedPages = 0;
    for(Page page: pages) {
      if(page == null)
        break;
      Iterable<String> words = new Words(page.getText());
      for (String word: words)
        if(word.length()>1 || word.equals("a") || word.equals("I"))
          countWord(word);
      ++processedPages;    
    }
    long end = System.currentTimeMillis();
    System.out.println("Processed pages: " + processedPages);
    System.out.println("Elapsed time: " + (end - start) + "ms");

    LinkedHashMap<String, Integer> commonWords = new LinkedHashMap<>();
    counts.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) .forEachOrdered(x -> commonWords.put(x.getKey(), x.getValue()));
    commonWords.entrySet().stream().limit(3).toList().forEach(x -> System.out.println("Word: \'"+x.getKey()+"\' with total "+x.getValue()+" occurrences!"));
  }

  private static void countWord(String word) {
    Integer currentCount = counts.get(word);
    if (currentCount == null)
      counts.put(word, 1);
    else
      counts.put(word, currentCount + 1);
  }
}
