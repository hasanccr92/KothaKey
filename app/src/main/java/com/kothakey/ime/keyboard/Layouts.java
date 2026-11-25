package com.kothakey.ime.keyboard;

/** AZERTY letters plus Gboard-style symbol pages. */
public final class Layouts {

  public enum Page {
    LETTERS,
    SYMBOLS1,
    SYMBOLS2,
    NUMPAD
  }

  private Layouts() {}

  public static KeyboardLayout create(
      Page page, Language language, boolean numberRow, Arrangement arrangement) {
    switch (page) {
      case SYMBOLS1:
        return symbols1();
      case SYMBOLS2:
        return symbols2();
      case NUMPAD:
        return numpad();
      case LETTERS:
      default:
        return arrangement == Arrangement.QWERTY
            ? lettersQwerty(language, numberRow)
            : lettersAzerty(language, numberRow);
    }
  }

  private static KeyboardLayout lettersAzerty(Language language, boolean numberRow) {
    boolean bn = language == Language.BN;
    KeyboardLayout layout = new KeyboardLayout();
    if (numberRow) {
      layout.add(
          Key.letter('1', "1"),
          Key.letter('2', "2"),
          Key.letter('3', "3"),
          Key.letter('4', "4"),
          Key.letter('5', "5"),
          Key.letter('6', "6"),
          Key.letter('7', "7"),
          Key.letter('8', "8"),
          Key.letter('9', "9"),
          Key.letter('0', "0"));
    }
    layout.add(
        letter('a', "a", numberRow ? null : "1", bn ? "1" : "1àâæáäãåāœ"),
        letter('z', "z", numberRow ? null : "2", bn ? "2" : "2"),
        letter('e', "e", numberRow ? null : "3", bn ? "3" : "3éèêëęėē"),
        letter('r', "r", numberRow ? null : "4", bn ? "4" : "4"),
        letter('t', "t", numberRow ? null : "5", bn ? "5" : "5"),
        letter('y', "y", numberRow ? null : "6", bn ? "6" : "6ÿýŷ"),
        letter('u', "u", numberRow ? null : "7", bn ? "7" : "7ùûüúū"),
        letter('i', "i", numberRow ? null : "8", bn ? "8" : "8îïíīįì"),
        letter('o', "o", numberRow ? null : "9", bn ? "9" : "9ôöòóœøōõ"),
        letter('p', "p", numberRow ? null : "0", bn ? "0" : "0"));
    layout.add(
        letter('q', "q", null, null),
        letter('s', "s", null, bn ? null : "ßśš"),
        letter('d', "d", null, null),
        letter('f', "f", null, null),
        letter('g', "g", null, null),
        letter('h', "h", null, null),
        letter('j', "j", null, null),
        letter('k', "k", null, null),
        letter('l', "l", null, null),
        letter('m', "m", null, null));
    layout.add(
        Key.fn(Codes.SHIFT, "\u21e7", 1.5f),
        letter('w', "w", null, null),
        letter('x', "x", null, null),
        letter('c', "c", null, bn ? null : "çćč"),
        letter('v', "v", null, null),
        letter('b', "b", null, null),
        letter('n', "n", null, bn ? null : "ñń"),
        letter('\'', "'", null, "`’"),
        Key.fn(Codes.DELETE, "\u232b", 1.5f));
    layout.add(bottom(true, language));
    return layout;
  }

  private static KeyboardLayout lettersQwerty(Language language, boolean numberRow) {
    boolean bn = language == Language.BN;
    KeyboardLayout layout = new KeyboardLayout();
    if (numberRow) {
      layout.add(
          Key.letter('1', "1"),
          Key.letter('2', "2"),
          Key.letter('3', "3"),
          Key.letter('4', "4"),
          Key.letter('5', "5"),
          Key.letter('6', "6"),
          Key.letter('7', "7"),
          Key.letter('8', "8"),
          Key.letter('9', "9"),
          Key.letter('0', "0"));
    }
    layout.add(
        letter('q', "q", numberRow ? null : "1", bn ? "1" : "1"),
        letter('w', "w", numberRow ? null : "2", bn ? "2" : "2"),
        letter('e', "e", numberRow ? null : "3", bn ? "3" : "3éèêëęėē"),
        letter('r', "r", numberRow ? null : "4", bn ? "4" : "4"),
        letter('t', "t", numberRow ? null : "5", bn ? "5" : "5"),
        letter('y', "y", numberRow ? null : "6", bn ? "6" : "6ÿýŷ"),
        letter('u', "u", numberRow ? null : "7", bn ? "7" : "7ùûüúū"),
        letter('i', "i", numberRow ? null : "8", bn ? "8" : "8îïíīįì"),
        letter('o', "o", numberRow ? null : "9", bn ? "9" : "9ôöòóœøōõ"),
        letter('p', "p", numberRow ? null : "0", bn ? "0" : "0"));
    layout.add(
        letter('a', "a", null, bn ? null : "àâæáäãåāœ"),
        letter('s', "s", null, bn ? null : "ßśš"),
        letter('d', "d", null, null),
        letter('f', "f", null, null),
        letter('g', "g", null, null),
        letter('h', "h", null, null),
        letter('j', "j", null, null),
        letter('k', "k", null, null),
        letter('l', "l", null, null));
    layout.add(
        Key.fn(Codes.SHIFT, "\u21e7", 1.5f),
        letter('z', "z", null, null),
        letter('x', "x", null, null),
        letter('c', "c", null, bn ? null : "çćč"),
        letter('v', "v", null, null),
        letter('b', "b", null, null),
        letter('n', "n", null, bn ? null : "ñń"),
        letter('m', "m", null, null),
        letter('\'', "'", null, "`’"),
        Key.fn(Codes.DELETE, "\u232b", 1.5f));
    layout.add(bottom(true, language));
    return layout;
  }

  private static KeyboardLayout symbols1() {
    KeyboardLayout layout = new KeyboardLayout();
    layout.add(
        Key.letter('1', "1"),
        Key.letter('2', "2"),
        Key.letter('3', "3"),
        Key.letter('4', "4"),
        Key.letter('5', "5"),
        Key.letter('6', "6"),
        Key.letter('7', "7"),
        Key.letter('8', "8"),
        Key.letter('9', "9"),
        Key.letter('0', "0"));
    layout.add(
        Key.letter('@', "@"),
        Key.letter('#', "#"),
        Key.letter('\u20ac', "\u20ac"),
        Key.letter('_', "_"),
        Key.letter('&', "&"),
        Key.letter('-', "-"),
        Key.letter('+', "+"),
        Key.letter('(', "("),
        Key.letter(')', ")"),
        Key.letter('/', "/"));
    layout.add(
        Key.fn(Codes.SYM2, "=\\<", 1.5f),
        Key.letter('*', "*"),
        Key.letter('"', "\""),
        Key.letter('\'', "'"),
        Key.letter(':', ":"),
        Key.letter(';', ";"),
        Key.letter('!', "!"),
        Key.letter('?', "?"),
        Key.fn(Codes.DELETE, "\u232b", 1.5f));
    layout.add(bottomSymbols());
    return layout;
  }

  private static KeyboardLayout symbols2() {
    KeyboardLayout layout = new KeyboardLayout();
    layout.add(
        Key.letter('~', "~"),
        Key.letter('`', "`"),
        Key.letter('|', "|"),
        Key.letter('\u2022', "\u2022"),
        Key.letter('\u221a', "\u221a"),
        Key.letter('\u03c0', "\u03c0"),
        Key.letter('\u00f7', "\u00f7"),
        Key.letter('\u00d7', "\u00d7"),
        Key.letter('\u00a7', "\u00a7"),
        Key.letter('\u0394', "\u0394"));
    layout.add(
        Key.letter('\u00a3', "\u00a3"),
        Key.letter('\u00a5', "\u00a5"),
        Key.letter('$', "$"),
        Key.letter('\u00a2', "\u00a2"),
        Key.letter('^', "^"),
        Key.letter('\u00b0', "\u00b0"),
        Key.letter('=', "="),
        Key.letter('{', "{"),
        Key.letter('}', "}"),
        Key.letter('\\', "\\"));
    layout.add(
        Key.fn(Codes.SYM1, "?123", 1.5f),
        Key.letter('%', "%"),
        Key.letter('\u00a9', "\u00a9"),
        Key.letter('\u00ae', "\u00ae"),
        Key.letter('\u2122', "\u2122"),
        Key.letter('\u2713', "\u2713"),
        Key.letter('[', "["),
        Key.letter(']', "]"),
        Key.fn(Codes.DELETE, "\u232b", 1.5f));
    layout.add(
        Key.fn(Codes.SYMBOLS, "ABC", 1.2f),
        Key.letter('<', "<"),
        Key.fn(Codes.NUMPAD, "1234", 1.1f),
        Key.space("", 3.5f),
        Key.letter('>', ">"),
        Key.enter(1.4f));
    return layout;
  }

  private static KeyboardLayout numpad() {
    KeyboardLayout layout = new KeyboardLayout();
    layout.add(Key.letter('1', "1"), Key.letter('2', "2"), Key.letter('3', "3"));
    layout.add(Key.letter('4', "4"), Key.letter('5', "5"), Key.letter('6', "6"));
    layout.add(Key.letter('7', "7"), Key.letter('8', "8"), Key.letter('9', "9"));
    layout.add(
        Key.fn(Codes.SYMBOLS, "ABC", 1f),
        Key.letter('0', "0"),
        Key.fn(Codes.DELETE, "\u232b", 1f));
    layout.add(
        Key.fn(Codes.SYM1, "?123", 1.2f),
        Key.letter(',', ","),
        Key.space("", 3.6f),
        Key.letter('.', "."),
        Key.enter(1.4f));
    return layout;
  }

  private static Key[] bottom(boolean letters, Language language) {
    // Weights sum to 10 like the letter rows; ?123 lines up with Shift and enter with Delete.
    return new Key[] {
      Key.fn(Codes.SYMBOLS, letters ? "?123" : "ABC", 1.5f),
      weighted(',', ",", 1f),
      Key.space(language.spaceLabel, 5f),
      weighted('.', ".", 1f),
      Key.enter(1.5f)
    };
  }

  private static Key weighted(int code, String label, float weight) {
    return new Key(code, label, weight, null, null, Key.LETTER);
  }

  private static Key[] bottomSymbols() {
    return new Key[] {
      Key.fn(Codes.SYMBOLS, "ABC", 1.2f),
      Key.letter(',', ","),
      Key.fn(Codes.NUMPAD, "1234", 1.1f),
      Key.space("", 3.4f),
      Key.letter('.', "."),
      Key.enter(1.4f)
    };
  }

  private static Key letter(int code, String label, String hint, String popup) {
    return Key.letter(code, label, hint, popup);
  }
}
