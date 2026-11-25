package com.kothakey.ime.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kothakey.avro.AvroPhoneticTransliterator;
import com.kothakey.avro.Transliterator;
import org.junit.Test;

public class BengaliGuideTest {

  @Test
  public void everyChartRowMatchesWhatTheKeyboardTypes() {
    Transliterator avro = AvroPhoneticTransliterator.createDefault();
    int rows = 0;
    for (BengaliChart.Section section : BengaliChart.sections()) {
      for (BengaliChart.Entry e : section.entries) {
        assertEquals(
            "typing " + e.latin + " in " + section.kind,
            e.bengali,
            avro.parse(e.latin).toString());
        rows++;
      }
    }
    assertTrue(rows > 80);
  }

  @Test
  public void hardLettersAreInTheChart() {
    StringBuilder all = new StringBuilder();
    for (BengaliChart.Section section : BengaliChart.sections()) {
      for (BengaliChart.Entry e : section.entries) {
        all.append(e.bengali);
      }
    }
    String text = all.toString();
    for (String letter : new String[] {"ৎ", "ঁ", "ং", "ঃ", "ঙ", "ঞ", "ড়", "ঢ়", "য়", "ঋ"}) {
      assertTrue("missing " + letter, text.contains(letter));
    }
  }
}
