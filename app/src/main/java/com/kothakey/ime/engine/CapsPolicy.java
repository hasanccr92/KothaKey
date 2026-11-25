package com.kothakey.ime.engine;

/**
 * Decides whether the next Latin letter should be a capital, from the text before the cursor. It
 * does not ask the text field, so moving the cursor back into a sentence drops the capital again.
 */
public final class CapsPolicy {

  private CapsPolicy() {}

  /**
   * True at the start of the field, just after a newline, or after {@code . ! ? …} once a space
   * has been typed. {@code Mr } stays lowercase: the letter before the space is not a sentence end.
   */
  public static boolean shouldCapitalize(CharSequence before) {
    if (before == null || before.length() == 0) {
      return true;
    }
    int i = before.length();
    while (i > 0 && isInlineSpace(before.charAt(i - 1))) {
      i--;
    }
    if (i == 0) {
      return true;
    }
    char last = before.charAt(i - 1);
    if (last == '\n' || last == '\r') {
      return true;
    }
    boolean whitespaceFollowed = i < before.length();
    return whitespaceFollowed && isSentenceEnd(last);
  }

  private static boolean isInlineSpace(char c) {
    return c == ' ' || c == '\t' || c == '\u00a0';
  }

  private static boolean isSentenceEnd(char c) {
    return c == '.' || c == '!' || c == '?' || c == '\u2026';
  }
}
