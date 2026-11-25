package com.kothakey.ime.keyboard;

public final class Key {
  public static final int LETTER = 0;
  public static final int FUNCTION = 1;
  public static final int SPACE = 2;
  public static final int ENTER = 3;

  public final int code;
  public final String label;
  public final float weight;
  public final String hint;
  public final String[] popup;
  public final int kind;

  public float x;
  public float y;
  public float w;
  public float h;

  public Key(int code, String label, float weight, String hint, String[] popup, int kind) {
    this.code = code;
    this.label = label;
    this.weight = weight;
    this.hint = hint;
    this.popup = popup;
    this.kind = kind;
  }

  public static Key letter(int code, String label) {
    return new Key(code, label, 1f, null, null, LETTER);
  }

  public static Key letter(int code, String label, String hint, String popup) {
    String[] extra = popup == null || popup.isEmpty() ? null : split(popup);
    return new Key(code, label, 1f, hint, extra, LETTER);
  }

  public static Key fn(int code, String label, float weight) {
    return new Key(code, label, weight, null, null, FUNCTION);
  }

  public static Key space(String label, float weight) {
    return new Key(Codes.SPACE, label, weight, null, null, SPACE);
  }

  public static Key enter(float weight) {
    return new Key(Codes.ENTER, "\u21b5", weight, null, null, ENTER);
  }

  public boolean contains(float px, float py) {
    return px >= x && px < x + w && py >= y && py < y + h;
  }

  public boolean isLetter() {
    return kind == LETTER && code > 0;
  }

  private static String[] split(String popup) {
    int n = popup.codePointCount(0, popup.length());
    String[] out = new String[n];
    int i = 0;
    int idx = 0;
    while (i < popup.length()) {
      int cp = popup.codePointAt(i);
      int next = i + Character.charCount(cp);
      out[idx++] = popup.substring(i, next);
      i = next;
    }
    return out;
  }
}
