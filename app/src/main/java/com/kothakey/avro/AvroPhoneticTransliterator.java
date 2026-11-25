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

/** Avro phonetic transliterator: autocorrect → parse → Bengali, with phonetic confusables. */
public final class AvroPhoneticTransliterator implements Transliterator {

  public static final String ID = "avro_phonetic";

  /**
   * Avro treats these as part of the current word, not as separators: conjunct breaker, chandrabindu,
   * visarga, taka.
   */
  private static final String WORD_CHARS = "`^:$";

  private final AvroPhonetic mEngine;
  private final AvroAutocorrect mAutocorrect;
  private final AvroConfusables mConfusables;

  public AvroPhoneticTransliterator(
      AvroPhonetic engine, AvroAutocorrect autocorrect, AvroConfusables confusables) {
    mEngine = engine;
    mAutocorrect = autocorrect;
    mConfusables = confusables;
  }

  public static AvroPhoneticTransliterator createDefault() {
    return new AvroPhoneticTransliterator(
        AvroPhonetic.createDefault(), AvroAutocorrect.loadDefault(), new AvroConfusables());
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public CharSequence autocorrect(CharSequence latin) {
    return mAutocorrect.apply(latin);
  }

  @Override
  public CharSequence parse(CharSequence latin) {
    return mEngine.parse(mAutocorrect.apply(latin));
  }

  @Override
  public int[] alternativesFor(int codePoint) {
    return mConfusables.alternativesFor(codePoint);
  }

  @Override
  public boolean isInputCharacter(int codePoint) {
    if (Character.isLetterOrDigit(codePoint)) {
      return true;
    }
    return WORD_CHARS.indexOf(codePoint) >= 0;
  }

  public AvroPhonetic engine() {
    return mEngine;
  }
}
