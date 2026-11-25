package com.kothakey.ime.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccentPopupTest {

  private static final String NEEDED = "çéèêëàâîïôùûüœæÿ";

  @Test
  public void everyFrenchAccentIsOnAzertyAndQwerty() {
    for (Arrangement arrangement : Arrangement.values()) {
      StringBuilder found = new StringBuilder();
      KeyboardLayout layout =
          Layouts.create(Layouts.Page.LETTERS, Language.FR, false, arrangement);
      for (Key key : layout.keys) {
        if (key.popup == null) {
          continue;
        }
        for (String extra : key.popup) {
          found.append(extra);
        }
      }
      String all = found.toString();
      for (int i = 0; i < NEEDED.length(); ) {
        int cp = NEEDED.codePointAt(i);
        String letter = new String(Character.toChars(cp));
        assertTrue(arrangement.id + " missing " + letter, all.contains(letter));
        i += Character.charCount(cp);
      }
    }
  }

  @Test
  public void shiftLiftsAccentedLetters() {
    assertEquals("É", KeyCase.apply("é", true));
    assertEquals("Ç", KeyCase.apply("ç", true));
    assertEquals("Œ", KeyCase.apply("œ", true));
    assertEquals("é", KeyCase.apply("é", false));
    assertEquals("copy", KeyCase.apply("copy", true));
  }
}
