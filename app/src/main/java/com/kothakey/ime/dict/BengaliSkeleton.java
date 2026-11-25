package com.kothakey.ime.dict;

/**
 * Collapses Bengali spellings that people confuse when typing phonetically, so that a misspelt
 * word and its correct form get the same key (for example {@code উচু} and {@code উঁচু}, or {@code
 * তড়িত} and {@code তড়িৎ}).
 *
 * <p>The groups follow {@link com.kothakey.avro.AvroConfusables} and are kept in one switch so they
 * are easy to tune. The key is only for finding candidates; it is never shown.
 */
public final class BengaliSkeleton {

  private BengaliSkeleton() {}

  /** Returns the confusion-tolerant key of a Bengali string. */
  public static String of(CharSequence word) {
    String s = joinNukta(word.toString());
    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      char mapped = map(c);
      if (mapped != 0) {
        out.append(mapped);
      }
    }
    return out.toString();
  }

  /** Turns decomposed ড় ঢ় য় (base letter plus nukta) into the single letters before mapping. */
  public static String joinNukta(String s) {
    if (s.indexOf('\u09bc') < 0) {
      return s;
    }
    return s.replace("\u09a1\u09bc", "\u09dc")
        .replace("\u09a2\u09bc", "\u09dd")
        .replace("\u09af\u09bc", "\u09df");
  }

  /** The key letter for one character, or 0 when the character is ignored. */
  private static char map(char c) {
    switch (c) {
      // Ignored: chandrabindu, visarga, nukta, zero-width joiners.
      case '\u0981':
      case '\u0983':
      case '\u09bc':
      case '\u200c':
      case '\u200d':
        return 0;
      // Khanda ta looks and sounds like ta.
      case '\u09ce':
        return '\u09a4';
      // Dot-below r sounds, and the two ya letters, fall to ra and ja.
      case '\u09dc':
      case '\u09dd':
        return '\u09b0';
      case '\u09df':
      case '\u09af':
        return '\u099c';
      // Long and short vowels.
      case '\u09c0':
        return '\u09bf';
      case '\u09c2':
        return '\u09c1';
      case '\u0988':
        return '\u0987';
      case '\u098a':
        return '\u0989';
      // Retroflex and dental stops, and na.
      case '\u09a3':
      case '\u099e':
        return '\u09a8';
      case '\u099f':
        return '\u09a4';
      case '\u09a0':
        return '\u09a5';
      case '\u09a1':
        return '\u09a6';
      case '\u09a2':
        return '\u09a7';
      // Sibilants.
      case '\u09b6':
      case '\u09b7':
        return '\u09b8';
      // Anusvara and nga.
      case '\u0982':
        return '\u0999';
      default:
        return c;
    }
  }
}
