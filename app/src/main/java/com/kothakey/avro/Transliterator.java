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

/** A per-keyboard live transliterator selected by {@code ask:transliteratorId}. */
public interface Transliterator {

  String id();

  /** Convert the current Latin keystroke buffer to the target script. */
  CharSequence parse(CharSequence latin);

  /** Optional Latin-to-Latin autocorrect applied before {@link #parse}. */
  default CharSequence autocorrect(CharSequence latin) {
    return latin;
  }

  /** Phonetic (or other) alternatives for dictionary fuzzy matching. {@code codes[0] == cp}. */
  int[] alternativesFor(int codePoint);

  /** Characters that belong in the Latin buffer rather than as word separators. */
  default boolean isInputCharacter(int codePoint) {
    return Character.isLetterOrDigit(codePoint);
  }
}
