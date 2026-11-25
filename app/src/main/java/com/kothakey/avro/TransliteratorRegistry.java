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

import java.util.concurrent.atomic.AtomicReference;

/** Resolves {@code ask:transliteratorId} values to shared, lazily-loaded instances. */
public final class TransliteratorRegistry {

  private static final AtomicReference<AvroPhoneticTransliterator> AVRO = new AtomicReference<>();

  private TransliteratorRegistry() {}

  public static Transliterator get(String id) {
    if (id == null || id.isEmpty()) {
      return null;
    }
    if (AvroPhoneticTransliterator.ID.equals(id)) {
      AvroPhoneticTransliterator existing = AVRO.get();
      if (existing != null) {
        return existing;
      }
      AvroPhoneticTransliterator created = AvroPhoneticTransliterator.createDefault();
      if (AVRO.compareAndSet(null, created)) {
        return created;
      }
      return AVRO.get();
    }
    return null;
  }
}
