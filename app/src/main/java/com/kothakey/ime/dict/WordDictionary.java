package com.kothakey.ime.dict;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Accent-insensitive prefix dictionary backed by word\\tfreq TSV. */
public final class WordDictionary {

  public static final class Hit {
    public final String word;
    public final int freq;

    Hit(String word, int freq) {
      this.word = word;
      this.freq = freq;
    }
  }

  private static final class Entry {
    final String word;
    final String key;
    final int freq;

    Entry(String word, String key, int freq) {
      this.word = word;
      this.key = key;
      this.freq = freq;
    }
  }

  private Entry[] mEntries = new Entry[0];
  private volatile boolean mReady;

  public void loadAsync(Context context, String asset) {
    new Thread(
            () -> {
              List<Entry> list = new ArrayList<>();
              try (BufferedReader reader =
                  new BufferedReader(
                      new InputStreamReader(
                          context.getAssets().open(asset), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                  Entry entry = parse(line);
                  if (entry != null) {
                    list.add(entry);
                  }
                }
              } catch (IOException ignored) {
              }
              list.sort(Comparator.comparing(e -> e.key));
              mEntries = list.toArray(new Entry[0]);
              mReady = true;
            },
            "kothakey-words")
        .start();
  }

  public boolean ready() {
    return mReady;
  }

  /** Loads {@code word<TAB>freq} lines. Used by tests and by {@link #loadAsync}. */
  public void loadLines(List<String> lines) {
    List<Entry> list = new ArrayList<>();
    if (lines != null) {
      for (String line : lines) {
        Entry entry = parse(line);
        if (entry != null) {
          list.add(entry);
        }
      }
    }
    list.sort(Comparator.comparing(e -> e.key));
    mEntries = list.toArray(new Entry[0]);
    mReady = true;
  }

  /** True when this exact spelling is in the list (accents count, case does not). */
  public boolean contains(String query) {
    if (!mReady || query == null || query.isEmpty() || mEntries.length == 0) {
      return false;
    }
    String key = fold(query);
    for (int i = lower(key); i < mEntries.length && mEntries[i].key.equals(key); i++) {
      if (mEntries[i].word.equalsIgnoreCase(query)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Dictionary words close to a mistyped word, closest first. Nothing is returned for a word
   * shorter than three letters.
   */
  public List<Hit> near(String typed, KeyProximity proximity, int limit) {
    if (!mReady || typed == null || limit <= 0 || mEntries.length == 0) {
      return Collections.emptyList();
    }
    String folded = fold(typed);
    double max = FuzzyMatcher.maxCost(folded.length());
    if (folded.isEmpty() || max <= 0) {
      return Collections.emptyList();
    }
    List<Hit> found = new ArrayList<>();
    List<Double> costs = new ArrayList<>();
    scan(typed, folded, folded.charAt(0), 0, max, proximity, found, costs);
    if (proximity != null) {
      for (char neighbour : proximity.neighbours(folded.charAt(0))) {
        scan(typed, folded, neighbour, 0.5, max, proximity, found, costs);
      }
    }
    Integer[] order = new Integer[found.size()];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    Arrays.sort(
        order,
        (a, b) -> {
          int c = Double.compare(costs.get(a), costs.get(b));
          if (c != 0) {
            return c;
          }
          return Integer.compare(found.get(b).freq, found.get(a).freq);
        });
    List<Hit> out = new ArrayList<>();
    for (int index : order) {
      if (out.size() >= limit) {
        break;
      }
      Hit hit = found.get(index);
      boolean seen = false;
      for (Hit have : out) {
        if (have.word.equals(hit.word)) {
          seen = true;
          break;
        }
      }
      if (!seen) {
        out.add(hit);
      }
    }
    return out;
  }

  private void scan(
      String typed,
      String folded,
      char first,
      double extra,
      double max,
      KeyProximity proximity,
      List<Hit> found,
      List<Double> costs) {
    String prefix = String.valueOf(first);
    for (int i = lower(prefix); i < mEntries.length; i++) {
      Entry e = mEntries[i];
      if (e.key.isEmpty() || e.key.charAt(0) != first) {
        break;
      }
      if (Math.abs(e.key.length() - folded.length()) > 2) {
        continue;
      }
      double cost = extra + FuzzyMatcher.cost(typed, e.word, proximity);
      if (cost <= 0 || cost > max || Double.isInfinite(cost)) {
        continue;
      }
      found.add(new Hit(e.word, e.freq));
      costs.add(cost);
    }
  }

  public List<Hit> prefix(String query, int limit) {
    if (!mReady || query == null || query.isEmpty() || mEntries.length == 0) {
      return Collections.emptyList();
    }
    String key = fold(query);
    List<Hit> hits = new ArrayList<>();
    for (int i = lower(key); i < mEntries.length; i++) {
      Entry e = mEntries[i];
      if (!e.key.startsWith(key)) {
        break;
      }
      // Same letters with a different accent ("ca" -> "ça") are still a suggestion.
      if (e.key.equals(key) && e.word.equalsIgnoreCase(query)) {
        continue;
      }
      hits.add(new Hit(e.word, e.freq));
    }
    hits.sort((a, b) -> Integer.compare(b.freq, a.freq));
    if (hits.size() > limit) {
      return hits.subList(0, limit);
    }
    return hits;
  }

  private int lower(String key) {
    int lo = 0;
    int hi = mEntries.length;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (mEntries[mid].key.compareTo(key) < 0) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return lo;
  }

  private static Entry parse(String line) {
    if (line == null) {
      return null;
    }
    int tab = line.indexOf('\t');
    if (tab <= 0) {
      return null;
    }
    String word = line.substring(0, tab).trim();
    if (word.isEmpty()) {
      return null;
    }
    int freq = 1;
    try {
      freq = Integer.parseInt(line.substring(tab + 1).trim());
    } catch (NumberFormatException ignored) {
    }
    return new Entry(word, fold(word), freq);
  }

  public static String fold(String raw) {
    String n = Normalizer.normalize(raw, Normalizer.Form.NFD);
    StringBuilder sb = new StringBuilder(n.length());
    for (int i = 0; i < n.length(); i++) {
      char c = n.charAt(i);
      if (Character.getType(c) != Character.NON_SPACING_MARK) {
        sb.append(c);
      }
    }
    return sb.toString().toLowerCase(Locale.ROOT);
  }
}
