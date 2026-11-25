package com.kothakey.ime.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyboardLayoutHitTest {

  private final KeyboardLayout layout =
      Layouts.create(Layouts.Page.LETTERS, Language.FR, false, Arrangement.AZERTY);

  public KeyboardLayoutHitTest() {
    layout.layout(1000, 280, 4, 6, 6, 10);
  }

  @Test
  public void theInnerEdgeOfPeriodIsSpace() {
    Key period = key('.');
    float y = period.y + period.h / 2f;
    assertEquals(Codes.SPACE, layout.hit(period.x + period.w * 0.1f, y).code);
    assertEquals((int) '.', layout.hit(period.x + period.w * 0.7f, y).code);
  }

  @Test
  public void theInnerEdgeOfCommaIsSpace() {
    Key comma = key(',');
    float y = comma.y + comma.h / 2f;
    assertEquals(Codes.SPACE, layout.hit(comma.x + comma.w * 0.9f, y).code);
    assertEquals((int) ',', layout.hit(comma.x + comma.w * 0.3f, y).code);
  }

  @Test
  public void belowTheKeyboardIsSpace() {
    Key period = key('.');
    assertEquals(Codes.SPACE, layout.hit(period.x + period.w * 0.8f, period.y + period.h + 4).code);
  }

  @Test
  public void theGapAbovePeriodGoesToTheLetterRow() {
    Key period = key('.');
    Key hit = layout.hit(period.x + period.w * 0.7f, period.y - 2);
    assertTrue(hit.y + hit.h <= period.y);
    assertTrue(hit.code != '.');
  }

  private Key key(int code) {
    for (Key key : layout.keys) {
      if (key.code == code) {
        return key;
      }
    }
    throw new AssertionError("missing " + code);
  }
}
