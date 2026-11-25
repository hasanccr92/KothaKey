/*
 * Copyright (c) 2026 AnySoftKeyboard contributors
 *
 * Autocorrect entries originate from OmicronLab Avro Keyboard (MPL-1.1).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.kothakey.avro;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Latin-to-Latin pre-pass used by Avro PC. If the current typed word exactly matches a dictionary
 * key (e.g. {@code america} → {@code amerika}), the replacement is parsed instead.
 */
public final class AvroAutocorrect {

  private final Map<String, String> mEntries;

  public AvroAutocorrect(Map<String, String> entries) {
    mEntries = entries;
  }

  public static AvroAutocorrect loadDefault() {
    try (InputStream in = AvroAutocorrect.class.getResourceAsStream("/avro_autocorrect.bin")) {
      if (in == null) {
        return new AvroAutocorrect(Map.of());
      }
      return load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load avro_autocorrect.bin", e);
    }
  }

  public static AvroAutocorrect load(InputStream raw) throws IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
    byte[] magic = in.readNBytes(4);
    if (magic.length != 4
        || magic[0] != 'A'
        || magic[1] != 'C'
        || magic[2] != 'A'
        || magic[3] != '1') {
      throw new IOException("Bad avro_autocorrect.bin magic");
    }
    int version = in.readUnsignedByte();
    if (version != 1) {
      throw new IOException("Unsupported autocorrect version: " + version);
    }
    int count = in.readInt();
    Map<String, String> entries = new HashMap<>(count * 2);
    for (int i = 0; i < count; i++) {
      String src = AvroDict.readUtf8(in);
      String dest = AvroDict.readUtf8(in);
      entries.put(src, dest);
    }
    return new AvroAutocorrect(entries);
  }

  public int size() {
    return mEntries.size();
  }

  /** Returns the autocorrected Latin string, or {@code latin} unchanged. */
  public CharSequence apply(CharSequence latin) {
    if (latin.length() == 0) {
      return latin;
    }
    String replacement = mEntries.get(latin.toString());
    return replacement != null ? replacement : latin;
  }
}
