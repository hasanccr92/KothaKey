package com.kothakey.ime.dict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BengaliSkeletonTest {

  private static void same(String a, String b) {
    assertEquals(a + " vs " + b, BengaliSkeleton.of(a), BengaliSkeleton.of(b));
  }

  @Test
  public void missingChandrabinduSharesTheKey() {
    same("উচু", "উঁচু");
  }

  @Test
  public void khandaTaSharesTheKeyWithTa() {
    same("তড়িত", "তড়িৎ");
  }

  @Test
  public void longAndShortVowelsCollapse() {
    same("দিন", "দীন");
    same("পুর", "পূর");
    same("ইচ্ছা", "ঈচ্ছা");
  }

  @Test
  public void decomposedDotBelowLettersMatchPrecomposed() {
    // ড + nukta must key like the single letter ড়.
    same("তড\u09bcিৎ", "তড়িৎ");
    same("তরিত", "তড়িত");
  }

  @Test
  public void retroflexAndDentalStopsCollapse() {
    same("টান", "তান");
    same("ঠাণ্ডা", "থান্দা");
  }

  @Test
  public void sibilantsCollapse() {
    same("শহর", "সহর");
    same("ষাট", "সাত");
  }

  @Test
  public void anusvaraAndVisargaAreForgiving() {
    same("সংসদ", "সঙসদ");
    same("দুঃখ", "দুখ");
  }

  @Test
  public void unrelatedWordsKeepDifferentKeys() {
    assertFalse(BengaliSkeleton.of("আমি").equals(BengaliSkeleton.of("তুমি")));
    assertFalse(BengaliSkeleton.of("বই").equals(BengaliSkeleton.of("কই")));
  }

  @Test
  public void prefixOfAKeyIsAPrefixOfTheLongerKey() {
    assertTrue(BengaliSkeleton.of("উঁচুতলা").startsWith(BengaliSkeleton.of("উচু")));
  }
}
