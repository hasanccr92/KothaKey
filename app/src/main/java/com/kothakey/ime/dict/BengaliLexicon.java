package com.kothakey.ime.dict;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bengali word suggestions that forgive the usual phonetic slips. Words are kept sorted by their
 * {@link BengaliSkeleton} key, so typing {@code উচু} finds {@code উঁচু} and typing {@code তড়িত} finds
 * {@code তড়িৎ}. Suggestions are only offered; nothing is replaced automatically.
 */
public final class BengaliLexicon {

  /** Most words looked at for one lookup, so very short prefixes stay fast. */
  private static final int MAX_SCAN = 3000;
  /** A learned word that is not in the word list needs this many uses before it is suggested. */
  private static final int LEARNED_MIN = 2;
  private static final int LEARNED_LIMIT = 5000;

  /** Words sorted by skeleton key. Replaced as a whole when loading finishes. */
  private volatile String[] mWords = new String[0];
  /** Skeleton key of {@link #mWords}, same order, computed once at load. */
  private volatile String[] mKeys = new String[0];

  private volatile boolean mReady;
  private final Map<String, Integer> mLearned = new HashMap<>();
  private Runnable mLearnedListener;

  /** Reads the given assets (one word per line) on a background thread. */
  public void loadAsync(Context context, String... assets) {
    new Thread(
            () -> {
              List<String> words = new ArrayList<>();
              for (String asset : assets) {
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(
                            context.getAssets().open(asset), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    String w = line.trim();
                    if (!w.isEmpty() && !w.startsWith("#")) {
                      words.add(w);
                    }
                  }
                } catch (IOException ignored) {
                }
              }
              load(words);
            },
            "kothakey-bn-lexicon")
        .start();
  }

  /** Builds the index from the given words (duplicates are dropped). */
  public void load(Collection<String> words) {
    Set<String> unique = new HashSet<>();
    for (String w : words) {
      unique.add(BengaliSkeleton.joinNukta(w));
    }
    final int n = unique.size();
    String[] array = unique.toArray(new String[n]);
    String[] keys = new String[n];
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) {
      keys[i] = BengaliSkeleton.of(array[i]);
      order[i] = i;
    }
    Arrays.sort(
        order,
        (a, b) -> {
          int c = keys[a].compareTo(keys[b]);
          return c != 0 ? c : array[a].compareTo(array[b]);
        });
    String[] sorted = new String[n];
    String[] sortedKeys = new String[n];
    for (int i = 0; i < n; i++) {
      sorted[i] = array[order[i]];
      sortedKeys[i] = keys[order[i]];
    }
    mKeys = sortedKeys;
    mWords = sorted;
    mReady = true;
  }

  public boolean ready() {
    return mReady;
  }

  // ---- learning ----

  /** Called whenever the learned words change, so the owner can save them. */
  public synchronized void setLearnedListener(Runnable listener) {
    mLearnedListener = listener;
  }

  public synchronized void setLearned(Map<String, Integer> counts) {
    mLearned.clear();
    mLearned.putAll(counts);
  }

  public synchronized Map<String, Integer> learnedSnapshot() {
    return new HashMap<>(mLearned);
  }

  public synchronized void clearLearned() {
    mLearned.clear();
  }

  /**
   * Records that the user used a word: weight 2 for a tapped suggestion, 1 for a word typed out.
   */
  public void learn(String wordRaw, int weight) {
    String word = wordRaw == null ? null : BengaliSkeleton.joinNukta(wordRaw);
    if (!worthLearning(word)) {
      return;
    }
    Runnable listener;
    synchronized (this) {
      Integer old = mLearned.get(word);
      mLearned.put(word, (old == null ? 0 : old) + weight);
      if (mLearned.size() > LEARNED_LIMIT) {
        trimLearned();
      }
      listener = mLearnedListener;
    }
    if (listener != null) {
      listener.run();
    }
  }

  private void trimLearned() {
    List<Map.Entry<String, Integer>> entries = new ArrayList<>(mLearned.entrySet());
    entries.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));
    int drop = entries.size() - LEARNED_LIMIT * 9 / 10;
    for (int i = 0; i < drop; i++) {
      mLearned.remove(entries.get(i).getKey());
    }
  }

  /** Only real-looking Bengali words are learned (no digits, Latin text or single letters). */
  static boolean worthLearning(String word) {
    if (word == null || word.codePointCount(0, word.length()) < 2) {
      return false;
    }
    for (int i = 0; i < word.length(); i++) {
      char c = word.charAt(i);
      boolean bengali = c >= '\u0980' && c <= '\u09ff' && !isBengaliDigit(c);
      if (!bengali || c == '\u0964' || c == '\u0965') {
        return false;
      }
    }
    return true;
  }

  private static boolean isBengaliDigit(char c) {
    return c >= '\u09e6' && c <= '\u09ef';
  }

  // ---- lookup ----

  /** True if the exact word is in the list or has been learned enough. */
  public boolean knows(String word) {
    return inList(word) || learnedCount(word) >= LEARNED_MIN;
  }

  private boolean inList(String word) {
    String[] keys = mKeys;
    String[] words = mWords;
    if (keys.length != words.length) {
      return false;
    }
    String key = BengaliSkeleton.of(word);
    for (int i = lowerBound(keys, key); i < words.length; i++) {
      String w = words[i];
      if (!keys[i].equals(key)) {
        return false;
      }
      if (w.equals(word)) {
        return true;
      }
    }
    return false;
  }

  private synchronized int learnedCount(String word) {
    Integer c = mLearned.get(word);
    return c == null ? 0 : c;
  }

  /**
   * Suggestions for the Bengali text typed so far, best first. Spellings that differ only by
   * confusable letters (the corrections) lead when the typed text is not itself a known word.
   */
  public List<String> suggest(String typedRaw, int limit) {
    String typed =
        typedRaw == null ? null : stripTrailing(BengaliSkeleton.joinNukta(typedRaw));
    if (typed == null || typed.isEmpty() || limit <= 0) {
      return Collections.emptyList();
    }
    String[] keys = mKeys;
    String[] words = mWords;
    if (keys.length != words.length) {
      return Collections.emptyList();
    }
    String key = BengaliSkeleton.of(typed);
    boolean known = knows(typed);
    Map<String, Integer> learned;
    synchronized (this) {
      learned = new HashMap<>(mLearned);
    }

    Set<String> seen = new HashSet<>();
    List<Candidate> found = new ArrayList<>();
    int scanned = 0;
    for (int i = lowerBound(keys, key); i < words.length && scanned < MAX_SCAN; i++, scanned++) {
      String w = words[i];
      String wKey = keys[i];
      if (!wKey.startsWith(key)) {
        break;
      }
      consider(found, seen, w, wKey, typed, key, known, learned);
    }
    for (Map.Entry<String, Integer> e : learned.entrySet()) {
      if (e.getValue() >= LEARNED_MIN) {
        String w = e.getKey();
        String wKey = BengaliSkeleton.of(w);
        if (wKey.startsWith(key)) {
          consider(found, seen, w, wKey, typed, key, known, learned);
        }
      }
    }

    Collections.sort(found);
    List<String> out = new ArrayList<>(Math.min(limit, found.size()));
    for (Candidate c : found) {
      if (out.size() >= limit) {
        break;
      }
      out.add(c.word);
    }
    return out;
  }

  private static void consider(
      List<Candidate> found,
      Set<String> seen,
      String w,
      String wKey,
      String typed,
      String key,
      boolean typedKnown,
      Map<String, Integer> learned) {
    if (w.equals(typed) || !seen.add(w)) {
      return;
    }
    int tier;
    if (wKey.equals(key)) {
      // Same sound, different spelling: a correction when the typed text is not a word.
      tier = typedKnown ? 2 : 0;
    } else if (w.startsWith(typed)) {
      tier = 1;
    } else {
      tier = 3;
    }
    Integer count = learned.get(w);
    found.add(new Candidate(w, tier, count == null ? 0 : count, commonPrefix(w, typed)));
  }

  private static final class Candidate implements Comparable<Candidate> {
    final String word;
    final int tier;
    final int learned;
    /** Letters shared with the start of the typed text; closer spellings come first. */
    final int similar;

    Candidate(String word, int tier, int learned, int similar) {
      this.word = word;
      this.tier = tier;
      this.learned = learned;
      this.similar = similar;
    }

    @Override
    public int compareTo(Candidate o) {
      if (tier != o.tier) {
        return Integer.compare(tier, o.tier);
      }
      if (learned != o.learned) {
        return Integer.compare(o.learned, learned);
      }
      if (similar != o.similar) {
        return Integer.compare(o.similar, similar);
      }
      if (word.length() != o.word.length()) {
        return Integer.compare(word.length(), o.word.length());
      }
      return word.compareTo(o.word);
    }
  }

  private static int commonPrefix(String a, String b) {
    int n = Math.min(a.length(), b.length());
    int i = 0;
    while (i < n && a.charAt(i) == b.charAt(i)) {
      i++;
    }
    return i;
  }

  /** First index whose key is not less than the given key. */
  private static int lowerBound(String[] keys, String key) {
    int lo = 0;
    int hi = keys.length;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (keys[mid].compareTo(key) < 0) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return lo;
  }

  /** Drops trailing punctuation such as the dari, keeping letters, vowel signs and digits. */
  static String stripTrailing(String s) {
    if (s == null) {
      return null;
    }
    int end = s.length();
    while (end > 0) {
      int cp = s.codePointBefore(end);
      int type = Character.getType(cp);
      boolean keep =
          Character.isLetterOrDigit(cp)
              || type == Character.NON_SPACING_MARK
              || type == Character.COMBINING_SPACING_MARK;
      if (keep) {
        break;
      }
      end -= Character.charCount(cp);
    }
    return s.substring(0, end);
  }
}
