package com.kothakey.ime.dict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kothakey.ime.keyboard.Arrangement;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class FuzzyMatcherTest {

  private final WordDictionary dictionary = new WordDictionary();
  private final KeyProximity azerty = KeyProximity.of(Arrangement.AZERTY);

  @Before
  public void setUp() {
    dictionary.loadLines(
        Arrays.asList(
            "hello\t1000",
            "help\t400",
            "bureau\t800",
            "the\t9000",
            "français\t500",
            "france\t200"));
  }

  @Test
  public void neighbourSlipSuggestsTheRealWord() {
    assertEquals("hello", dictionary.near("hellp", azerty, 3).get(0).word);
  }

  @Test
  public void swappedLettersSuggestTheRealWord() {
    assertEquals("the", dictionary.near("teh", azerty, 3).get(0).word);
  }

  @Test
  public void jumbledSpellingSuggestsBureau() {
    assertEquals("bureau", dictionary.near("bearou", azerty, 3).get(0).word);
  }

  @Test
  public void aMissingAccentIsACloseMatch() {
    List<WordDictionary.Hit> hits = dictionary.near("francais", azerty, 3);
    assertEquals("français", hits.get(0).word);
  }

  @Test
  public void aRealWordIsNotANearMissOfItself() {
    assertTrue(dictionary.contains("hello"));
    assertTrue(dictionary.near("hello", azerty, 3).isEmpty() || !dictionary.near("hello", azerty, 3).get(0).word.equals("hello"));
  }

  @Test
  public void prefixKeepsAccentedSpellings() {
    WordDictionary accents = new WordDictionary();
    accents.loadLines(Arrays.asList("ça\t50", "car\t40", "ca\t10"));
    boolean found = false;
    for (WordDictionary.Hit hit : accents.prefix("ca", 10)) {
      if ("ça".equals(hit.word)) {
        found = true;
      }
    }
    assertTrue(found);
  }
}
