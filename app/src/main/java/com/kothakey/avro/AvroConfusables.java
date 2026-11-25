/*
 * Copyright (c) 2026 AnySoftKeyboard contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.kothakey.avro;

import java.util.HashMap;
import java.util.Map;

/**
 * Phonetic near-misses for Bengali letters, derived from Avro's own documented ambiguities ({@code
 * t}/{@code T}, {@code s}/{@code sh}/{@code Sh}, {@code i}/{@code I}, …).
 *
 * <p>ASK's dictionary traversal already walks {@code WordComposer.getCodesAt(i)}; feeding these
 * arrays in place of physically adjacent keys turns that walk into a Bengali phonetic fuzzy matcher.
 */
public final class AvroConfusables {

  private static final int[] EMPTY = new int[0];

  private final Map<Integer, int[]> mTable;

  public AvroConfusables() {
    mTable = build();
  }

  public int[] alternativesFor(int codePoint) {
    int[] group = mTable.get(codePoint);
    if (group == null) {
      return new int[] {codePoint};
    }
    return group;
  }

  static Map<Integer, int[]> build() {
    Map<Integer, int[]> table = new HashMap<>();
    // Dental / retroflex stops
    group(table, 'ত', 'ট');
    group(table, 'থ', 'ঠ');
    group(table, 'দ', 'ড');
    group(table, 'ধ', 'ঢ');
    group(table, 'ন', 'ণ');
    // Sibilants
    group(table, 'স', 'শ', 'ষ');
    // R / ড় / ঢ়
    group(table, 'র', 'ড়', 'ঢ়');
    // J / Y
    group(table, 'জ', 'য');
    // B / BH
    group(table, 'ব', 'ভ');
    // Independent vowels
    group(table, 'ই', 'ঈ');
    group(table, 'উ', 'ঊ');
    group(table, 'অ', 'ও');
    // Dependent vowel signs (kars)
    group(table, 'ি', 'ী');
    group(table, 'ু', 'ূ');
    group(table, 'ো', 'অ');
    return table;
  }

  private static void group(Map<Integer, int[]> table, int... chars) {
    for (int i = 0; i < chars.length; i++) {
      int[] alts = new int[chars.length];
      alts[0] = chars[i];
      int dest = 1;
      for (int j = 0; j < chars.length; j++) {
        if (j != i) {
          alts[dest++] = chars[j];
        }
      }
      table.put(chars[i], alts);
    }
  }

  int[] empty() {
    return EMPTY;
  }
}
