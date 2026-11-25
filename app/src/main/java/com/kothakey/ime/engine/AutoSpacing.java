package com.kothakey.ime.engine;

/** Pure rules for the automatic space after punctuation and the double-space period. */
public final class AutoSpacing {

  /** Two space taps within this window turn into a period and a space. */
  public static final long DOUBLE_SPACE_MS = 300L;

  private AutoSpacing() {}

  /** Punctuation that is normally followed by a space. */
  public static boolean triggersSpace(int codePoint) {
    switch (codePoint) {
      case '.':
      case ',':
      case ';':
      case ':':
      case '!':
      case '?':
      case '\u0964': // dari
        return true;
      default:
        return false;
    }
  }

  /**
   * True when the punctuation is part of a number or time such as 3.14, 5,000 or 10:30, so no
   * space should follow it.
   *
   * @param before the character just before the punctuation, or 0 when there is none
   */
  public static boolean continuesNumber(int codePoint, int before) {
    boolean separator = codePoint == '.' || codePoint == ',' || codePoint == ':';
    return separator && before != 0 && Character.isDigit(before);
  }

  /** True for a character a sentence can end on: a letter, a digit or a Bengali vowel sign. */
  public static boolean endsSentence(int c) {
    if (Character.isLetterOrDigit(c)) {
      return true;
    }
    int type = Character.getType(c);
    return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
  }

  /**
   * True when the text before the cursor is a word ending followed by one space, so that a second
   * space should become a period.
   */
  public static boolean isDoubleSpacePeriod(CharSequence lastTwo) {
    return lastTwo != null
        && lastTwo.length() == 2
        && lastTwo.charAt(1) == ' '
        && endsSentence(lastTwo.charAt(0));
  }
}
