package com.kothakey.ime.dict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class NextWordModelTest {

  @Test
  public void aPairSuggestsTheUsualNextWord() {
    NextWordModel model = new NextWordModel();
    model.learn("", "I", "can");
    model.learn("", "I", "can");
    model.learn("", "I", "will");
    List<String> next = model.predict("", "I", 5);
    assertEquals("can", next.get(0));
    assertTrue(next.contains("will"));
  }

  @Test
  public void aTripleBeatsAPair() {
    NextWordModel model = new NextWordModel();
    model.learn("in", "sha", "Allah");
    model.learn("", "sha", "nothing");
    assertEquals("Allah", model.predict("in", "sha", 5).get(0));
  }

  @Test
  public void seedsDoNotStack() {
    NextWordModel model = new NextWordModel();
    model.seed("in sha", "Allah");
    model.seed("in sha", "Allah");
    model.learn("in", "sha", "Allah");
    assertEquals("Allah", model.predict("in", "sha", 3).get(0));
  }

  @Test
  public void wordsBeforeReadsTheTail() {
    List<String> words = NextWordModel.wordsBefore("In Sha Allah ", 3);
    assertEquals(3, words.size());
    assertEquals("In", words.get(0));
    assertEquals("Sha", words.get(1));
    assertEquals("Allah", words.get(2));
  }
}
