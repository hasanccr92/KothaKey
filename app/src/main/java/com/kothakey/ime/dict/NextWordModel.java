package com.kothakey.ime.dict;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Remembers which word tends to follow another. A pair ({@code I} -&gt; {@code can}) and a triple
 * ({@code in sha} -&gt; {@code Allah}) are counted separately; triples are offered first.
 */
public final class NextWordModel {

  private static final int MAX_ENTRIES = 20000;

  private final Map<String, Bucket> bigrams = new HashMap<>();
  private final Map<String, Bucket> trigrams = new HashMap<>();

  /** One context and the words seen after it, most common first when read. */
  private static final class Bucket {
    final Map<String, Integer> counts = new HashMap<>();
    final Map<String, String> display = new HashMap<>();
  }

  public synchronized void learn(String first, String second, String next) {
    if (!usable(second) || !usable(next)) {
      return;
    }
    add(bigrams, norm(second), next, 1);
    if (usable(first)) {
      add(trigrams, norm(first) + "\t" + norm(second), next, 1);
    }
    trim();
  }

  /** A bundled pair that must not stack up every time the keyboard starts. */
  public synchronized void seed(String context, String next) {
    if (context == null || !usable(next)) {
      return;
    }
    String[] parts = context.trim().split(" +");
    if (parts.length >= 2) {
      seedInto(trigrams, norm(parts[parts.length - 2]) + "\t" + norm(parts[parts.length - 1]), next);
      seedInto(bigrams, norm(parts[parts.length - 1]), next);
    } else if (parts.length == 1 && usable(parts[0])) {
      seedInto(bigrams, norm(parts[0]), next);
    }
  }

  /**
   * Words that follow this context, best first. {@code first} may be empty when only the previous
   * word is known.
   */
  public synchronized List<String> predict(String first, String second, int limit) {
    if (!usable(second) || limit <= 0) {
      return Collections.emptyList();
    }
    List<String> out = new ArrayList<>();
    if (usable(first)) {
      take(trigrams.get(norm(first) + "\t" + norm(second)), out, limit);
    }
    take(bigrams.get(norm(second)), out, limit);
    return out;
  }

  public synchronized void clear() {
    bigrams.clear();
    trigrams.clear();
  }

  public synchronized int size() {
    return count(bigrams) + count(trigrams);
  }

  /** The last {@code n} words in the text, oldest first. */
  public static List<String> wordsBefore(CharSequence text, int n) {
    List<String> words = new ArrayList<>();
    if (text == null || n <= 0) {
      return words;
    }
    int i = text.length();
    while (i > 0 && words.size() < n) {
      while (i > 0 && !isWordChar(text.charAt(i - 1))) {
        i--;
      }
      int end = i;
      while (i > 0 && isWordChar(text.charAt(i - 1))) {
        i--;
      }
      if (end > i) {
        words.add(text.subSequence(i, end).toString());
      }
    }
    Collections.reverse(words);
    return words;
  }

  public synchronized void load(File file) {
    bigrams.clear();
    trigrams.clear();
    if (file == null || !file.exists()) {
      return;
    }
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] p = line.split("\t", -1);
        if (p.length != 4) {
          continue;
        }
        int count;
        try {
          count = Integer.parseInt(p[3].trim());
        } catch (NumberFormatException e) {
          continue;
        }
        Map<String, Bucket> table = "t".equals(p[0]) ? trigrams : bigrams;
        add(table, p[1], p[2], count);
      }
    } catch (IOException ignored) {
    }
  }

  public synchronized void save(File file) {
    if (file == null) {
      return;
    }
    File tmp = new File(file.getPath() + ".tmp");
    try (BufferedWriter writer =
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))) {
      write(writer, "b", bigrams);
      write(writer, "t", trigrams);
    } catch (IOException e) {
      return;
    }
    if (!tmp.renameTo(file)) {
      file.delete();
      tmp.renameTo(file);
    }
  }

  private static void write(BufferedWriter writer, String kind, Map<String, Bucket> table)
      throws IOException {
    for (Map.Entry<String, Bucket> e : table.entrySet()) {
      Bucket bucket = e.getValue();
      for (Map.Entry<String, Integer> next : bucket.counts.entrySet()) {
        writer.write(kind);
        writer.write('\t');
        writer.write(e.getKey());
        writer.write('\t');
        writer.write(bucket.display.getOrDefault(next.getKey(), next.getKey()));
        writer.write('\t');
        writer.write(Integer.toString(next.getValue()));
        writer.write('\n');
      }
    }
  }

  private static void add(Map<String, Bucket> table, String context, String next, int by) {
    Bucket bucket = table.get(context);
    if (bucket == null) {
      bucket = new Bucket();
      table.put(context, bucket);
    }
    String key = norm(next);
    Integer old = bucket.counts.get(key);
    bucket.counts.put(key, (old == null ? 0 : old) + by);
    bucket.display.put(key, next);
  }

  private static void seedInto(Map<String, Bucket> table, String context, String next) {
    Bucket bucket = table.get(context);
    String key = norm(next);
    if (bucket != null && bucket.counts.containsKey(key)) {
      return;
    }
    add(table, context, next, 1);
  }

  /** Drops the least-used links once the table is past its cap. */
  private void trim() {
    while (size() > MAX_ENTRIES) {
      Map<String, Bucket> bestTable = null;
      String bestContext = null;
      String bestWord = null;
      int best = Integer.MAX_VALUE;
      best = consider(bigrams, best);
      int fromBigrams = consider(bigrams, Integer.MAX_VALUE);
      int fromTrigrams = consider(trigrams, Integer.MAX_VALUE);
      Map<String, Bucket> table = fromBigrams <= fromTrigrams ? bigrams : trigrams;
      int target = Math.min(fromBigrams, fromTrigrams);
      if (target == Integer.MAX_VALUE) {
        return;
      }
      for (Map.Entry<String, Bucket> e : table.entrySet()) {
        for (Map.Entry<String, Integer> n : e.getValue().counts.entrySet()) {
          if (n.getValue() == target) {
            bestTable = table;
            bestContext = e.getKey();
            bestWord = n.getKey();
            best = target;
            break;
          }
        }
        if (bestWord != null) {
          break;
        }
      }
      if (bestTable == null) {
        return;
      }
      Bucket bucket = bestTable.get(bestContext);
      bucket.counts.remove(bestWord);
      bucket.display.remove(bestWord);
      if (bucket.counts.isEmpty()) {
        bestTable.remove(bestContext);
      }
      if (best < 0) {
        return;
      }
    }
  }

  private static int consider(Map<String, Bucket> table, int lowest) {
    int best = lowest;
    for (Bucket bucket : table.values()) {
      for (int count : bucket.counts.values()) {
        if (count < best) {
          best = count;
        }
      }
    }
    return best;
  }

  private static void take(Bucket bucket, List<String> out, int limit) {
    if (bucket == null) {
      return;
    }
    List<String> keys = new ArrayList<>(bucket.counts.keySet());
    keys.sort((a, b) -> Integer.compare(bucket.counts.get(b), bucket.counts.get(a)));
    for (String key : keys) {
      if (out.size() >= limit) {
        return;
      }
      String word = bucket.display.getOrDefault(key, key);
      if (!out.contains(word)) {
        out.add(word);
      }
    }
  }

  private static int count(Map<String, Bucket> table) {
    int n = 0;
    for (Bucket bucket : table.values()) {
      n += bucket.counts.size();
    }
    return n;
  }

  static String norm(String word) {
    return word.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean usable(String word) {
    return word != null && word.codePointCount(0, word.length()) >= 1 && !word.trim().isEmpty();
  }

  private static boolean isWordChar(char c) {
    return Character.isLetter(c) || c == '\'' || c == '\u2019' || (c >= '\u0980' && c <= '\u09ff');
  }
}
