package com.kothakey.ime.dict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BengaliLexiconTest {

  private BengaliLexicon lexicon;

  @Before
  public void setUp() {
    lexicon = new BengaliLexicon();
    lexicon.load(
        Arrays.asList(
            "উঁচু", "উঁচুতলা", "উচ্চ", "তড়িৎ", "তড়িতাহত", "বিদ্যুৎ", "দিন", "দীন", "দিনক্ষণ",
            "আমি", "আমার", "আম", "তুমি"));
  }

  @Test
  public void missingChandrabinduSuggestsTheCorrectSpelling() {
    List<String> s = lexicon.suggest("উচু", 5);
    assertEquals("উঁচু", s.get(0));
  }

  @Test
  public void easyTaSuggestsKhandaTa() {
    List<String> s = lexicon.suggest("তড়িত", 5);
    assertEquals("তড়িৎ", s.get(0));
    assertTrue(s.contains("তড়িতাহত"));
  }

  @Test
  public void closerSpellingBeatsAnotherWordWithTheSameKey() {
    lexicon.load(Arrays.asList("তরিত", "তড়িৎ", "তড়িতাহত"));
    assertEquals("তড়িৎ", lexicon.suggest("তড়িত", 5).get(0));
  }

  @Test
  public void decomposedTypingStillFindsTheWord() {
    assertEquals("তড়িৎ", lexicon.suggest("তড\u09bcিত", 5).get(0));
  }

  @Test
  public void plainPrefixCompletionStillWorks() {
    List<String> s = lexicon.suggest("আ", 5);
    assertTrue(s.contains("আমি"));
    assertTrue(s.contains("আমার"));
    assertFalse(s.contains("তুমি"));
  }

  @Test
  public void aKnownWordIsNotSuggestedToItself() {
    assertFalse(lexicon.suggest("আমি", 5).contains("আমি"));
  }

  @Test
  public void vowelSignAtTheEndIsNotTreatedAsPunctuation() {
    assertTrue(lexicon.suggest("আমি", 5).isEmpty() || !lexicon.suggest("আমি", 5).contains("আম"));
  }

  @Test
  public void trailingDariIsIgnored() {
    assertEquals("উঁচু", lexicon.suggest("উচু।", 5).get(0));
  }

  @Test
  public void completionsOfAKnownWordComeBeforeLookAlikes() {
    List<String> s = lexicon.suggest("দিন", 5);
    assertEquals("দিনক্ষণ", s.get(0));
    assertTrue(s.contains("দীন"));
  }

  @Test
  public void unknownInputGivesNothing() {
    assertTrue(lexicon.suggest("ঝঝঝ", 5).isEmpty());
    assertTrue(lexicon.suggest("", 5).isEmpty());
  }

  @Test
  public void aWordUsedTwiceIsLearned() {
    assertTrue(lexicon.suggest("কথাকি", 5).isEmpty());
    lexicon.learn("কথাকি", 1);
    assertTrue(lexicon.suggest("কথা", 5).isEmpty());
    lexicon.learn("কথাকি", 1);
    assertTrue(lexicon.suggest("কথা", 5).contains("কথাকি"));
  }

  @Test
  public void aPickedWordIsLearnedAtOnce() {
    lexicon.learn("কথাকি", 2);
    assertTrue(lexicon.suggest("কথা", 5).contains("কথাকি"));
  }

  @Test
  public void learnedWordsRankFirstAmongEquals() {
    lexicon.learn("আমরা", 2);
    assertEquals("আমরা", lexicon.suggest("আ", 5).get(0));
  }

  @Test
  public void notWordsAreNotLearned() {
    lexicon.learn("abc", 2);
    lexicon.learn("১২৩", 2);
    lexicon.learn("ক", 2);
    assertTrue(lexicon.learnedSnapshot().isEmpty());
  }

  @Test
  public void clearForgetsLearnedWords() {
    lexicon.learn("কথাকি", 2);
    lexicon.clearLearned();
    assertTrue(lexicon.suggest("কথা", 5).isEmpty());
  }
}
