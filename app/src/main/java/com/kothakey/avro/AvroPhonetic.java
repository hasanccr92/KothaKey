/*
 * Copyright (c) 2026 AnySoftKeyboard contributors
 *
 * Port of OmicronLab jsAvroPhonetic (MPL-1.1). The original work is:
 *   The Original Code is jsAvroPhonetic
 *   The Initial Developer of the Original Code is
 *   Rifat Nabi <to.rifat@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.kothakey.avro;

/**
 * Native Avro phonetic transliterator.
 *
 * <p>Semantics match {@code avro-lib.js}: a single ordered pass over the pattern list (first match
 * wins; the dict is stored longest-first) and the historical {@code end < length} off-by-one in
 * {@code isExact}.
 */
public final class AvroPhonetic {

  private static final int ASCII = 128;

  private static final class Node {
    final Node[] children = new Node[ASCII];
    AvroDict.Pattern pattern;
  }

  private final AvroDict mDict;
  private final Node mRoot;
  private final boolean[] mVowel = new boolean[ASCII];
  private final boolean[] mConsonant = new boolean[ASCII];
  private final boolean[] mCaseSensitive = new boolean[ASCII];
  private final StringBuilder mFixed = new StringBuilder(64);
  private final StringBuilder mOutput = new StringBuilder(64);

  public AvroPhonetic(AvroDict dict) {
    mDict = dict;
    mRoot = new Node();
    fillSet(mVowel, dict.vowel);
    fillSet(mConsonant, dict.consonant);
    fillSet(mCaseSensitive, dict.casesensitive);
    for (AvroDict.Pattern pattern : dict.patterns) {
      Node node = mRoot;
      for (int i = 0; i < pattern.find.length(); i++) {
        char c = pattern.find.charAt(i);
        if (c >= ASCII) {
          throw new IllegalStateException("Non-ASCII Avro find key: " + pattern.find);
        }
        if (node.children[c] == null) {
          node.children[c] = new Node();
        }
        node = node.children[c];
      }
      // First pattern that ends here wins (original list order = JS iteration order).
      if (node.pattern == null) {
        node.pattern = pattern;
      }
    }
  }

  public static AvroPhonetic createDefault() {
    return new AvroPhonetic(AvroDict.loadDefault());
  }

  public AvroDict dict() {
    return mDict;
  }

  /** Transliterate {@code input} to Bengali. Reuses internal buffers; copy the result if kept. */
  public CharSequence parse(CharSequence input) {
    fixString(input);
    mOutput.setLength(0);
    final int length = mFixed.length();
    for (int cur = 0; cur < length; ) {
      AvroDict.Pattern pattern = findPattern(cur);
      if (pattern == null) {
        mOutput.append(mFixed.charAt(cur));
        cur++;
        continue;
      }
      int end = cur + pattern.find.length();
      if (pattern.hasRules()) {
        String replaced = processRules(pattern, cur, end);
        mOutput.append(replaced != null ? replaced : pattern.replace);
      } else {
        mOutput.append(pattern.replace);
      }
      cur = end;
    }
    return mOutput;
  }

  public String parseToString(CharSequence input) {
    return parse(input).toString();
  }

  public CharSequence appendAndParse(StringBuilder latin, int codePoint) {
    latin.appendCodePoint(codePoint);
    return parse(latin);
  }

  private AvroDict.Pattern findPattern(int cur) {
    Node node = mRoot;
    AvroDict.Pattern best = null;
    int bestIndex = Integer.MAX_VALUE;
    final int length = mFixed.length();
    for (int i = cur; i < length; i++) {
      char c = mFixed.charAt(i);
      if (c >= ASCII || node.children[c] == null) {
        break;
      }
      node = node.children[c];
      if (node.pattern != null && node.pattern.index < bestIndex) {
        best = node.pattern;
        bestIndex = node.pattern.index;
      }
    }
    return best;
  }

  private String processRules(AvroDict.Pattern pattern, int cur, int end) {
    for (AvroDict.Rule rule : pattern.rules) {
      boolean matched = true;
      for (AvroDict.Match match : rule.matches) {
        if (!processMatch(match, cur, end)) {
          matched = false;
          break;
        }
      }
      if (matched) {
        return rule.replace;
      }
    }
    return null;
  }

  private boolean processMatch(AvroDict.Match match, int cur, int end) {
    final int length = mFixed.length();
    final boolean negative = match.negative;
    if (match.scope == AvroDict.SCOPE_EXACT) {
      int start;
      int stop;
      if (match.type == AvroDict.TYPE_SUFFIX) {
        start = end;
        stop = end + match.value.length();
      } else {
        start = cur - match.value.length();
        stop = cur;
      }
      return isExact(match.value, start, stop, negative);
    }

    int chk = match.type == AvroDict.TYPE_PREFIX ? cur - 1 : end;
    boolean condition;
    if (match.scope == AvroDict.SCOPE_PUNCTUATION) {
      condition =
          (chk < 0 && match.type == AvroDict.TYPE_PREFIX)
              || (chk >= length && match.type == AvroDict.TYPE_SUFFIX)
              || (chk >= 0 && chk < length && isPunctuation(mFixed.charAt(chk)));
    } else if (match.scope == AvroDict.SCOPE_VOWEL) {
      condition =
          ((chk >= 0 && match.type == AvroDict.TYPE_PREFIX)
                  || (chk < length && match.type == AvroDict.TYPE_SUFFIX))
              && chk >= 0
              && chk < length
              && isVowel(mFixed.charAt(chk));
    } else if (match.scope == AvroDict.SCOPE_CONSONANT) {
      condition =
          ((chk >= 0 && match.type == AvroDict.TYPE_PREFIX)
                  || (chk < length && match.type == AvroDict.TYPE_SUFFIX))
              && chk >= 0
              && chk < length
              && isConsonant(mFixed.charAt(chk));
    } else {
      condition = true;
    }
    // JS: if ((!condition) ^ negative) replace = false. Keep that XOR.
    return !((!condition) ^ negative);
  }

  /**
   * Bug-compatible with jsAvroPhonetic {@code isExact}: the exclusive {@code end < length} check
   * means a suffix exact-match at the very end of the buffer never succeeds (unless negated).
   */
  private boolean isExact(String needle, int start, int end, boolean negative) {
    boolean found =
        start >= 0
            && end < mFixed.length()
            && end - start == needle.length()
            && regionMatches(start, needle);
    return found ^ negative;
  }

  private boolean regionMatches(int start, String needle) {
    for (int i = 0; i < needle.length(); i++) {
      if (mFixed.charAt(start + i) != needle.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private void fixString(CharSequence input) {
    mFixed.setLength(0);
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c < ASCII && mCaseSensitive[c]) {
        mFixed.append(c);
      } else if (c >= 'A' && c <= 'Z') {
        mFixed.append((char) (c + ('a' - 'A')));
      } else {
        mFixed.append(Character.toLowerCase(c));
      }
    }
  }

  private boolean isVowel(char c) {
    return c < ASCII && mVowel[c];
  }

  private boolean isConsonant(char c) {
    return c < ASCII && mConsonant[c];
  }

  private boolean isPunctuation(char c) {
    char lower = (c >= 'A' && c <= 'Z') ? (char) (c + ('a' - 'A')) : c;
    return !isVowel(lower) && !isConsonant(lower);
  }

  private static void fillSet(boolean[] dest, String chars) {
    for (int i = 0; i < chars.length(); i++) {
      char c = chars.charAt(i);
      if (c < dest.length) {
        dest[c] = true;
        if (c >= 'a' && c <= 'z') {
          dest[c - ('a' - 'A')] = true;
        }
      }
    }
  }
}
