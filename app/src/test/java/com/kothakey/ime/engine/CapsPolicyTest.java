package com.kothakey.ime.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CapsPolicyTest {

  @Test
  public void emptyFieldStartsASentence() {
    assertTrue(CapsPolicy.shouldCapitalize(""));
    assertTrue(CapsPolicy.shouldCapitalize(null));
    assertTrue(CapsPolicy.shouldCapitalize("   "));
  }

  @Test
  public void middleOfASentenceStaysLowercase() {
    assertFalse(CapsPolicy.shouldCapitalize("hello"));
    assertFalse(CapsPolicy.shouldCapitalize("hello "));
    assertFalse(CapsPolicy.shouldCapitalize("I went home"));
  }

  @Test
  public void spaceAfterSentencePunctuationStartsASentence() {
    assertTrue(CapsPolicy.shouldCapitalize("Done. "));
    assertTrue(CapsPolicy.shouldCapitalize("Yes! "));
    assertTrue(CapsPolicy.shouldCapitalize("Why? "));
    assertTrue(CapsPolicy.shouldCapitalize("Wait\u2026 "));
  }

  @Test
  public void punctuationWithoutASpaceDoesNotCapitaliseYet() {
    assertFalse(CapsPolicy.shouldCapitalize("Done."));
    assertFalse(CapsPolicy.shouldCapitalize("3.14"));
  }

  @Test
  public void aNewlineStartsASentence() {
    assertTrue(CapsPolicy.shouldCapitalize("hello\n"));
    assertTrue(CapsPolicy.shouldCapitalize("hello\n  "));
  }

  @Test
  public void abbreviationFollowedByASpaceIsNotANewSentence() {
    assertFalse(CapsPolicy.shouldCapitalize("Mr "));
    assertFalse(CapsPolicy.shouldCapitalize("See Dr "));
  }
}
