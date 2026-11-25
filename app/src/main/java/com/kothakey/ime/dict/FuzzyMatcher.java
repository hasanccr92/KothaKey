package com.kothakey.ime.dict;

/**
 * How far a typed word is from a dictionary spelling. Neighbour-key slips, swapped letters and a
 * missing accent are cheap; anything past {@link #maxCost(int)} is not a suggestion.
 */
public final class FuzzyMatcher {

  static final double SWAP = 0.6;
  static final double ACCENT = 0.1;
  static final double NEIGHBOUR = 0.5;
  static final double OTHER = 1.0;
  static final double INDEL = 0.7;
  /** A jumbled spelling, used when ordinary edits are too expensive (bearou / bureau). */
  static final double ANAGRAM = 1.15;
  static final double ONE_OFF_JUMBLE = 1.4;

  private FuzzyMatcher() {}

  /** The most error allowed for a word of this length. Shorter than 3 is never corrected. */
  public static double maxCost(int length) {
    if (length <= 2) {
      return 0;
    }
    if (length <= 4) {
      return 1;
    }
    if (length <= 7) {
      return 2;
    }
    return 3;
  }

  /**
   * Distance from what was typed to a candidate, or {@link Double#POSITIVE_INFINITY} when it is too
   * far to suggest. Both strings are compared without accents except for the accent-only case.
   */
  public static double cost(String typed, String candidate, KeyProximity proximity) {
    String a = WordDictionary.fold(typed);
    String b = WordDictionary.fold(candidate);
    if (a.equals(b)) {
      return typed.equalsIgnoreCase(candidate) ? 0 : ACCENT;
    }
    double max = maxCost(a.length());
    if (max <= 0 || Math.abs(a.length() - b.length()) > 2) {
      return Double.POSITIVE_INFINITY;
    }
    double edit = damerau(a, b, proximity, max);
    if (edit <= max) {
      return edit;
    }
    double jumble = jumble(a, b);
    return jumble <= max ? jumble : Double.POSITIVE_INFINITY;
  }

  private static double damerau(String a, String b, KeyProximity proximity, double max) {
    int n = a.length();
    int m = b.length();
    double[][] dp = new double[n + 1][m + 1];
    for (int i = 0; i <= n; i++) {
      dp[i][0] = i * INDEL;
    }
    for (int j = 0; j <= m; j++) {
      dp[0][j] = j * INDEL;
    }
    for (int i = 1; i <= n; i++) {
      double rowBest = dp[i][0];
      for (int j = 1; j <= m; j++) {
        char ca = a.charAt(i - 1);
        char cb = b.charAt(j - 1);
        double sub = ca == cb ? 0 : substitute(ca, cb, proximity);
        double best = dp[i - 1][j - 1] + sub;
        best = Math.min(best, dp[i - 1][j] + INDEL);
        best = Math.min(best, dp[i][j - 1] + INDEL);
        if (i > 1 && j > 1 && ca == b.charAt(j - 2) && a.charAt(i - 2) == cb) {
          best = Math.min(best, dp[i - 2][j - 2] + SWAP);
        }
        dp[i][j] = best;
        rowBest = Math.min(rowBest, best);
      }
      if (rowBest > max) {
        return Double.POSITIVE_INFINITY;
      }
    }
    return dp[n][m];
  }

  private static double substitute(char a, char b, KeyProximity proximity) {
    if (proximity != null && proximity.adjacent(a, b)) {
      return NEIGHBOUR;
    }
    return OTHER;
  }

  /** Same letters, or one letter off. Costs more than a clean edit so a real typo still wins. */
  private static double jumble(String a, String b) {
    if (Math.abs(a.length() - b.length()) > 1) {
      return Double.POSITIVE_INFINITY;
    }
    int[] count = new int[26];
    int extra = 0;
    for (int i = 0; i < a.length(); i++) {
      int k = a.charAt(i) - 'a';
      if (k < 0 || k >= 26) {
        extra++;
      } else {
        count[k]++;
      }
    }
    for (int i = 0; i < b.length(); i++) {
      int k = b.charAt(i) - 'a';
      if (k < 0 || k >= 26) {
        extra++;
      } else {
        count[k]--;
      }
    }
    int imbalance = extra;
    for (int n : count) {
      imbalance += Math.abs(n);
    }
    imbalance /= 2;
    if (imbalance == 0 && a.length() == b.length()) {
      return ANAGRAM;
    }
    if (imbalance <= 1) {
      return ONE_OFF_JUMBLE;
    }
    return Double.POSITIVE_INFINITY;
  }
}
