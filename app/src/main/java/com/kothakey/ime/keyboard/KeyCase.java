package com.kothakey.ime.keyboard;

/**
 * Uppercase for a key or a long-press character. Unlike a plain a-z check, this also lifts accented
 * letters, so shift plus é produces É.
 */
public final class KeyCase {

  private KeyCase() {}

  public static int apply(int codePoint, boolean shift) {
    if (!shift || !Character.isLetter(codePoint)) {
      return codePoint;
    }
    return Character.toUpperCase(codePoint);
  }

  public static String apply(String text, boolean shift) {
    if (!shift || text == null || text.isEmpty()) {
      return text;
    }
    if (text.codePointCount(0, text.length()) != 1) {
      return text;
    }
    int cp = text.codePointAt(0);
    int up = apply(cp, true);
    if (up == cp) {
      return text;
    }
    return new String(Character.toChars(up));
  }
}
