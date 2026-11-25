package com.kothakey.ime.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutoSpacingTest {

  @Test
  public void commonPunctuationTriggersASpace() {
    for (char c : ".,;:!?".toCharArray()) {
      assertTrue("" + c, AutoSpacing.triggersSpace(c));
    }
  }

  @Test
  public void dariTriggersASpace() {
    assertTrue(AutoSpacing.triggersSpace('\u0964'));
  }

  @Test
  public void lettersAndBracketsDoNot() {
    for (char c : "a1(')-\"".toCharArray()) {
      assertFalse("" + c, AutoSpacing.triggersSpace(c));
    }
  }

  @Test
  public void decimalAndThousandsSeparatorsKeepTheNumber() {
    assertTrue(AutoSpacing.continuesNumber('.', '3'));
    assertTrue(AutoSpacing.continuesNumber(',', '5'));
    assertTrue(AutoSpacing.continuesNumber(':', '1'));
  }

  @Test
  public void bengaliDigitsCountAsDigits() {
    assertTrue(AutoSpacing.continuesNumber('.', '\u09e9'));
  }

  @Test
  public void punctuationAfterWordsIsNotANumber() {
    assertFalse(AutoSpacing.continuesNumber('.', 'a'));
    assertFalse(AutoSpacing.continuesNumber(',', ' '));
    assertFalse(AutoSpacing.continuesNumber('.', 0));
  }

  @Test
  public void questionMarksAfterDigitsStillGetASpace() {
    assertFalse(AutoSpacing.continuesNumber('?', '3'));
    assertFalse(AutoSpacing.continuesNumber('!', '3'));
  }

  @Test
  public void wordThenSpaceIsADoubleSpacePeriod() {
    assertTrue(AutoSpacing.isDoubleSpacePeriod("o "));
    assertTrue(AutoSpacing.isDoubleSpacePeriod("7 "));
  }

  @Test
  public void bengaliVowelSignEndsASentence() {
    // "\u0986\u09ae\u09bf" ends with a vowel sign (mark), as in the word ami.
    assertTrue(AutoSpacing.isDoubleSpacePeriod("\u09bf "));
    assertTrue(AutoSpacing.isDoubleSpacePeriod("\u09ae "));
  }

  @Test
  public void punctuationOrSpacesBeforeTheSpaceAreNotReplaced() {
    assertFalse(AutoSpacing.isDoubleSpacePeriod(". "));
    assertFalse(AutoSpacing.isDoubleSpacePeriod(", "));
    assertFalse(AutoSpacing.isDoubleSpacePeriod("  "));
  }

  @Test
  public void needsExactlyTwoCharactersEndingInASpace() {
    assertFalse(AutoSpacing.isDoubleSpacePeriod(null));
    assertFalse(AutoSpacing.isDoubleSpacePeriod(" "));
    assertFalse(AutoSpacing.isDoubleSpacePeriod("ab"));
  }
}
