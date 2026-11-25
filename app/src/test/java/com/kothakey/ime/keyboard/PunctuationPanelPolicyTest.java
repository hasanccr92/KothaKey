package com.kothakey.ime.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kothakey.ime.keyboard.PunctuationPanelPolicy.ModeAction;
import org.junit.Before;
import org.junit.Test;

public class PunctuationPanelPolicyTest {

  private static final long T0 = 10_000L;
  private static final long SLOW = PunctuationPanelPolicy.DOUBLE_TAP_MS + 1;

  private PunctuationPanelPolicy mPolicy;

  @Before
  public void setUp() {
    mPolicy = new PunctuationPanelPolicy();
  }

  /** Enters the punctuation panel with a single, unhurried tap. */
  private void enterPanel() {
    assertEquals(ModeAction.SHOW_PUNCTUATION, mPolicy.onModeKey(true, T0));
  }

  @Test
  public void symbolThenSpaceReturnsToLetters() {
    enterPanel();
    mPolicy.onChar();
    assertTrue(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void symbolThenReturnKeyReturnsToLetters() {
    enterPanel();
    mPolicy.onChar();
    // Space and return share one rule.
    assertTrue(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void symbolsKeepThePanelUntilSpace() {
    enterPanel();
    mPolicy.onChar();
    mPolicy.onChar();
    assertTrue(mPolicy.returnsOnSpace());
    assertTrue(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void returnHappensOnlyOncePerSession() {
    enterPanel();
    mPolicy.onChar();
    assertTrue(mPolicy.onSpaceOrEnter());
    assertFalse(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void spaceWithoutTypingASymbolDoesNothing() {
    enterPanel();
    assertFalse(mPolicy.returnsOnSpace());
    assertFalse(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void spaceOnLettersNeverReturns() {
    // The panel was never entered through the mode key.
    mPolicy.onChar();
    assertFalse(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void digitsAndSeparatorsStayUntilSpace() {
    enterPanel();
    // 3.14 then 5,000: digits and . , never end the session; only space does.
    for (int i = 0; i < 4; i++) {
      mPolicy.onChar();
      assertTrue(mPolicy.returnsOnSpace());
    }
    assertTrue(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void doubleTapLocksThePanel() {
    enterPanel();
    assertEquals(ModeAction.LOCK_PUNCTUATION, mPolicy.onModeKey(false, T0 + 100));
    assertTrue(mPolicy.locked());
  }

  @Test
  public void slowSecondTapLeavesThePanel() {
    enterPanel();
    assertEquals(ModeAction.SHOW_LETTERS, mPolicy.onModeKey(false, T0 + SLOW));
    assertFalse(mPolicy.locked());
  }

  @Test
  public void lockedPanelNeverReturnsOnSpace() {
    enterPanel();
    mPolicy.onModeKey(false, T0 + 100);
    mPolicy.onChar();
    assertFalse(mPolicy.returnsOnSpace());
    assertFalse(mPolicy.onSpaceOrEnter());
    assertFalse(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void modeKeyUnlocksAndExits() {
    enterPanel();
    mPolicy.onModeKey(false, T0 + 100);
    assertEquals(ModeAction.SHOW_LETTERS, mPolicy.onModeKey(false, T0 + 5_000));
    assertFalse(mPolicy.locked());
  }

  @Test
  public void reenteringAfterUnlockAutoReturnsAgain() {
    enterPanel();
    mPolicy.onModeKey(false, T0 + 100);
    mPolicy.onModeKey(false, T0 + 5_000);
    assertEquals(ModeAction.SHOW_PUNCTUATION, mPolicy.onModeKey(true, T0 + 6_000));
    mPolicy.onChar();
    assertTrue(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void modeKeyExitResetsTypedSymbols() {
    enterPanel();
    mPolicy.onChar();
    assertEquals(ModeAction.SHOW_LETTERS, mPolicy.onModeKey(false, T0 + 5_000));
    // Back on the letters: a space must not trigger anything.
    assertFalse(mPolicy.onSpaceOrEnter());
  }

  @Test
  public void symbolTypedBeforeSecondTapIsNotALock() {
    enterPanel();
    mPolicy.onChar();
    assertEquals(ModeAction.SHOW_LETTERS, mPolicy.onModeKey(false, T0 + 100));
    assertFalse(mPolicy.locked());
  }

  @Test
  public void freshEntryResetsEarlierState() {
    enterPanel();
    mPolicy.onChar();
    // Entered again without leaving through the mode key (for example via the language switch).
    assertEquals(ModeAction.SHOW_PUNCTUATION, mPolicy.onModeKey(true, T0 + 2_000));
    assertFalse(mPolicy.returnsOnSpace());
  }

  @Test
  public void resetForgetsTheSession() {
    enterPanel();
    mPolicy.onChar();
    mPolicy.reset();
    assertFalse(mPolicy.returnsOnSpace());
    assertFalse(mPolicy.locked());
  }
}
