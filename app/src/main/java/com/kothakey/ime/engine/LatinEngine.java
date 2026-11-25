package com.kothakey.ime.engine;

import android.view.inputmethod.InputConnection;
import com.kothakey.ime.dict.Contractions;
import com.kothakey.ime.dict.FuzzyMatcher;
import com.kothakey.ime.dict.KeyProximity;
import com.kothakey.ime.dict.NextWordModel;
import com.kothakey.ime.dict.WordDictionary;
import com.kothakey.ime.emoji.EmojiKeywordIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** French + English composing engine with multilingual suggestions. */
public final class LatinEngine implements InputEngine {

  private final WordDictionary mFr;
  private final WordDictionary mEn;
  private final EmojiKeywordIndex mEmoji;
  private final StringBuilder mWord = new StringBuilder(48);
  private final Map<String, Integer> mLearned = new HashMap<>();
  private final Map<String, String> mLearnedDisplay = new HashMap<>();

  public LatinEngine(WordDictionary fr, WordDictionary en, EmojiKeywordIndex emoji) {
    mFr = fr;
    mEn = en;
    mEmoji = emoji;
  }

  @Override
  public void onChar(InputConnection ic, int codePoint) {
    if (isWordChar(codePoint)) {
      mWord.appendCodePoint(codePoint);
      ic.setComposingText(mWord, 1);
    } else {
      commit(ic);
      ic.commitText(new String(Character.toChars(codePoint)), 1);
    }
  }

  @Override
  public void onDelete(InputConnection ic) {
    if (mWord.length() > 0) {
      mWord.deleteCharAt(mWord.length() - 1);
      ic.setComposingText(mWord, 1);
      return;
    }
    ic.deleteSurroundingText(1, 0);
  }

  @Override
  public void commit(InputConnection ic) {
    if (mWord.length() == 0) {
      return;
    }
    ic.commitText(mWord, 1);
    mWord.setLength(0);
  }

  @Override
  public void reset(InputConnection ic) {
    if (ic != null && mWord.length() > 0) {
      ic.finishComposingText();
    }
    mWord.setLength(0);
  }

  @Override
  public String composing() {
    return mWord.toString();
  }

  @Override
  public List<String> suggestions(int limit) {
    return suggestFor(mWord.toString(), "", "", null, null, limit);
  }

  /**
   * Completions, and when the typed word is unknown a typo correction, with context words such as
   * Allah after "in sha" moved up.
   */
  public List<String> suggestFor(
      String typed, String prev1, String prev2, KeyProximity proximity, NextWordModel next, int limit) {
    if (typed == null || typed.isEmpty() || limit <= 0) {
      return List.of();
    }
    boolean known = mFr.contains(typed) || mEn.contains(typed) || learnedWell(typed);
    List<String> out = new ArrayList<>();
    // "dont" -> "don't" comes first; the user usually wants it.
    String exact = Contractions.exact(typed);
    boolean hadExact = exact != null && !applyCase(typed, exact).equals(typed);
    if (hadExact) {
      out.add(applyCase(typed, exact));
    }
    if (mEmoji != null) {
      for (String emoji : mEmoji.suggest(typed, 3)) {
        if (!out.contains(emoji)) {
          out.add(emoji);
        }
      }
    }
    Map<String, WordDictionary.Hit> merged = new HashMap<>();
    addHits(merged, mFr.prefix(typed, 16));
    addHits(merged, mEn.prefix(typed, 16));
    List<WordDictionary.Hit> hits = new ArrayList<>(merged.values());
    hits.sort((a, b) -> Integer.compare(b.freq, a.freq));
    if (!known && proximity != null) {
      List<WordDictionary.Hit> near = new ArrayList<>();
      near.addAll(mFr.near(typed, proximity, 4));
      near.addAll(mEn.near(typed, proximity, 4));
      near.sort(
          (a, b) -> {
            int c =
                Double.compare(
                    FuzzyMatcher.cost(typed, a.word, proximity),
                    FuzzyMatcher.cost(typed, b.word, proximity));
            return c != 0 ? c : Integer.compare(b.freq, a.freq);
          });
      int added = 0;
      int slot = hadExact ? 1 : 0;
      for (WordDictionary.Hit hit : near) {
        if (added >= 2) {
          break;
        }
        String word = applyCase(typed, hit.word);
        if (!out.contains(word)) {
          out.add(Math.min(slot + added, out.size()), word);
          added++;
        }
      }
    }
    List<String> words = new ArrayList<>();
    addLearnedPrefix(typed, words);
    for (WordDictionary.Hit hit : hits) {
      // The dictionary holds apostrophe-less spellings like "didnt"; the contraction replaces them.
      if (Contractions.isBrokenSpelling(hit.word)) {
        continue;
      }
      String word = applyCase(typed, hit.word);
      if (!words.contains(word)) {
        words.add(word);
      }
    }
    List<String> partial = new ArrayList<>();
    for (String c : Contractions.prefix(typed, 3)) {
      partial.add(applyCase(typed, c));
    }
    // With an apostrophe already typed, completions of the contraction lead; otherwise they
    // follow the two best ordinary words ("did" -> did, didn't...).
    int at = typed.indexOf('\'') >= 0 || typed.indexOf('\u2019') >= 0 ? 0 : Math.min(2, words.size());
    words.addAll(at, partial);
    if (next != null) {
      String folded = WordDictionary.fold(typed);
      List<String> predicted = next.predict(prev1, prev2, 6);
      int insert = 0;
      for (String word : predicted) {
        if (WordDictionary.fold(word).startsWith(folded)) {
          words.add(insert++, applyCase(typed, word));
        }
      }
    }
    for (String word : words) {
      if (out.size() >= limit) {
        break;
      }
      if (!out.contains(word)) {
        out.add(word);
      }
    }
    return out;
  }

  @Override
  public boolean isWordChar(int codePoint) {
    return Character.isLetter(codePoint) || codePoint == '\'' || codePoint == '\u2019';
  }

  public void pick(InputConnection ic, String word) {
    ic.commitText(word + " ", 1);
    mWord.setLength(0);
  }

  public synchronized void setLearned(Map<String, Integer> counts) {
    mLearned.clear();
    mLearnedDisplay.clear();
    if (counts == null) {
      return;
    }
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      String key = WordDictionary.fold(e.getKey());
      mLearned.put(key, e.getValue());
      mLearnedDisplay.put(key, e.getKey());
    }
  }

  public synchronized Map<String, Integer> learnedSnapshot() {
    Map<String, Integer> out = new HashMap<>();
    for (Map.Entry<String, Integer> e : mLearned.entrySet()) {
      out.put(mLearnedDisplay.getOrDefault(e.getKey(), e.getKey()), e.getValue());
    }
    return out;
  }

  public synchronized void clearLearned() {
    mLearned.clear();
    mLearnedDisplay.clear();
  }

  /** Weight 2 for a tapped suggestion, 1 for a word typed out. Two uses make it a known word. */
  public synchronized void learn(String word, int weight) {
    if (word == null || WordDictionary.fold(word).length() < 2 || weight <= 0) {
      return;
    }
    String key = WordDictionary.fold(word);
    Integer old = mLearned.get(key);
    mLearned.put(key, (old == null ? 0 : old) + weight);
    mLearnedDisplay.put(key, word);
  }

  private synchronized boolean learnedWell(String typed) {
    Integer count = mLearned.get(WordDictionary.fold(typed));
    return count != null && count >= 2;
  }

  private synchronized void addLearnedPrefix(String typed, List<String> dest) {
    String key = WordDictionary.fold(typed);
    for (Map.Entry<String, Integer> e : mLearned.entrySet()) {
      if (e.getValue() >= 2 && e.getKey().startsWith(key) && !e.getKey().equals(key)) {
        String word = mLearnedDisplay.get(e.getKey());
        if (word != null) {
          dest.add(applyCase(typed, word));
        }
      }
    }
  }

  private static void addHits(Map<String, WordDictionary.Hit> dest, List<WordDictionary.Hit> src) {
    for (WordDictionary.Hit hit : src) {
      String key = WordDictionary.fold(hit.word);
      WordDictionary.Hit prev = dest.get(key);
      if (prev == null || hit.freq > prev.freq) {
        dest.put(key, hit);
      }
    }
  }

  private static String applyCase(String typed, String word) {
    if (typed.isEmpty()) {
      return word;
    }
    boolean allUpper = true;
    boolean firstUpper = Character.isUpperCase(typed.codePointAt(0));
    int count = 0;
    for (int i = 0; i < typed.length(); ) {
      int cp = typed.codePointAt(i);
      count++;
      if (!Character.isUpperCase(cp) && Character.isLetter(cp)) {
        allUpper = false;
      }
      i += Character.charCount(cp);
    }
    if (allUpper && count > 1) {
      return word.toUpperCase(java.util.Locale.ROOT);
    }
    if (firstUpper) {
      int cp = word.codePointAt(0);
      return new String(Character.toChars(Character.toUpperCase(cp)))
          + word.substring(Character.charCount(cp));
    }
    return word;
  }
}
