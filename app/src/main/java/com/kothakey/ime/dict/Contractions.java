package com.kothakey.ime.dict;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * English contractions keyed by their apostrophe-less spelling, so that typing "dont" offers
 * "don't".
 */
public final class Contractions {

  private static final String[] PAIRS = {
    "im", "I'm",
    "ive", "I've",
    "ill", "I'll",
    "id", "I'd",
    "youre", "you're",
    "youve", "you've",
    "youll", "you'll",
    "youd", "you'd",
    "hes", "he's",
    "shes", "she's",
    "its", "it's",
    "theyre", "they're",
    "theyve", "they've",
    "theyll", "they'll",
    "theyd", "they'd",
    "were", "we're",
    "weve", "we've",
    "well", "we'll",
    "wed", "we'd",
    "thats", "that's",
    "whats", "what's",
    "wheres", "where's",
    "whos", "who's",
    "hows", "how's",
    "theres", "there's",
    "heres", "here's",
    "lets", "let's",
    "dont", "don't",
    "doesnt", "doesn't",
    "didnt", "didn't",
    "cant", "can't",
    "couldnt", "couldn't",
    "wont", "won't",
    "wouldnt", "wouldn't",
    "shouldnt", "shouldn't",
    "isnt", "isn't",
    "arent", "aren't",
    "wasnt", "wasn't",
    "werent", "weren't",
    "hasnt", "hasn't",
    "havent", "haven't",
    "hadnt", "hadn't",
    "mustnt", "mustn't",
    "neednt", "needn't",
    "aint", "ain't",
    "yall", "y'all",
    "couldve", "could've",
    "shouldve", "should've",
    "wouldve", "would've",
    "mightve", "might've",
    "mustve", "must've",
    "oclock", "o'clock"
  };

  /** Spellings that are real words on their own, so they must stay as dictionary hits. */
  private static final Set<String> REAL_WORDS =
      new HashSet<>(java.util.Arrays.asList("its", "ill", "id", "were", "well", "wed", "lets", "wont"));

  private static final Map<String, String> BY_STRIPPED = new LinkedHashMap<>();
  private static final Set<String> NON_WORDS = new HashSet<>();

  static {
    for (int i = 0; i < PAIRS.length; i += 2) {
      BY_STRIPPED.put(PAIRS[i], PAIRS[i + 1]);
      if (!REAL_WORDS.contains(PAIRS[i])) {
        NON_WORDS.add(PAIRS[i]);
      }
    }
  }

  private Contractions() {}

  /** Lower-cases and removes apostrophes. */
  public static String strip(String typed) {
    StringBuilder sb = new StringBuilder(typed.length());
    for (int i = 0; i < typed.length(); i++) {
      char c = typed.charAt(i);
      if (c != '\'' && c != '\u2019') {
        sb.append(c);
      }
    }
    return sb.toString().toLowerCase(Locale.ROOT);
  }

  /** True for apostrophe-less spellings like "dont" that are not real words. */
  public static boolean isBrokenSpelling(String word) {
    return NON_WORDS.contains(word.toLowerCase(Locale.ROOT));
  }

  /** The contraction that exactly matches the typed text (with or without apostrophe), or null. */
  public static String exact(String typed) {
    return BY_STRIPPED.get(strip(typed));
  }

  /** Contractions that begin with the typed text, best (shortest) first, excluding the exact one. */
  public static List<String> prefix(String typed, int limit) {
    List<String> out = new ArrayList<>();
    String lower = typed.toLowerCase(Locale.ROOT).replace('\u2019', '\'');
    boolean hasApostrophe = lower.indexOf('\'') >= 0;
    String stripped = strip(typed);
    if (stripped.length() < (hasApostrophe ? 1 : 3)) {
      return out;
    }
    for (Map.Entry<String, String> e : BY_STRIPPED.entrySet()) {
      if (out.size() >= limit) {
        break;
      }
      String contraction = e.getValue();
      String lowered = contraction.toLowerCase(Locale.ROOT);
      boolean match =
          hasApostrophe
              ? lowered.startsWith(lower)
              : e.getKey().startsWith(stripped);
      if (match && !lowered.equals(lower) && !e.getKey().equals(stripped)) {
        out.add(contraction);
      }
    }
    return out;
  }
}
