package com.kothakey.ime.dict;

import com.kothakey.ime.keyboard.Arrangement;

/** Which letter keys sit next to each other, so a slip onto a neighbour counts as a small typo. */
public final class KeyProximity {

  private final boolean[][] adjacent = new boolean[26][26];

  private KeyProximity(String[] rows) {
    for (String row : rows) {
      for (int i = 1; i < row.length(); i++) {
        link(row.charAt(i - 1), row.charAt(i));
      }
    }
    for (int r = 0; r < rows.length - 1; r++) {
      String above = rows[r];
      String below = rows[r + 1];
      for (int i = 0; i < above.length(); i++) {
        for (int j = i - 1; j <= i + 1; j++) {
          if (j >= 0 && j < below.length()) {
            link(above.charAt(i), below.charAt(j));
          }
        }
      }
    }
  }

  public static KeyProximity of(Arrangement arrangement) {
    if (arrangement == Arrangement.QWERTY) {
      return new KeyProximity(new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"});
    }
    return new KeyProximity(new String[] {"azertyuiop", "qsdfghjklm", "wxcvbn"});
  }

  public boolean adjacent(char a, char b) {
    int ia = index(a);
    int ib = index(b);
    return ia >= 0 && ib >= 0 && adjacent[ia][ib];
  }

  public char[] neighbours(char letter) {
    int i = index(letter);
    if (i < 0) {
      return new char[0];
    }
    int n = 0;
    for (int j = 0; j < 26; j++) {
      if (adjacent[i][j]) {
        n++;
      }
    }
    char[] out = new char[n];
    int k = 0;
    for (int j = 0; j < 26; j++) {
      if (adjacent[i][j]) {
        out[k++] = (char) ('a' + j);
      }
    }
    return out;
  }

  private void link(char a, char b) {
    int ia = index(a);
    int ib = index(b);
    if (ia >= 0 && ib >= 0 && ia != ib) {
      adjacent[ia][ib] = true;
      adjacent[ib][ia] = true;
    }
  }

  private static int index(char c) {
    char lower = Character.toLowerCase(c);
    if (lower < 'a' || lower > 'z') {
      return -1;
    }
    return lower - 'a';
  }
}
