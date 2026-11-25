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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime loader for the packed Avro phonetic dictionary ({@code avro_dict.bin}).
 *
 * <p>The binary is generated at build time from OmicronLab's {@code avrodict.json} so the IME never
 * parses JSON on device.
 */
public final class AvroDict {

  public static final int SCOPE_VOWEL = 0;
  public static final int SCOPE_CONSONANT = 1;
  public static final int SCOPE_PUNCTUATION = 2;
  public static final int SCOPE_EXACT = 3;

  public static final int TYPE_PREFIX = 0;
  public static final int TYPE_SUFFIX = 1;

  public static final class Match {
    public final int type;
    public final int scope;
    public final boolean negative;
    public final String value;

    Match(int type, int scope, boolean negative, String value) {
      this.type = type;
      this.scope = scope;
      this.negative = negative;
      this.value = value;
    }
  }

  public static final class Rule {
    public final String replace;
    public final List<Match> matches;

    Rule(String replace, List<Match> matches) {
      this.replace = replace;
      this.matches = matches;
    }
  }

  public static final class Pattern {
    public final String find;
    public final String replace;
    public final List<Rule> rules;
    public final int index;

    Pattern(String find, String replace, List<Rule> rules, int index) {
      this.find = find;
      this.replace = replace;
      this.rules = rules;
      this.index = index;
    }

    public boolean hasRules() {
      return !rules.isEmpty();
    }
  }

  public final String vowel;
  public final String consonant;
  public final String casesensitive;
  public final String number;
  public final List<Pattern> patterns;

  AvroDict(
      String vowel, String consonant, String casesensitive, String number, List<Pattern> patterns) {
    this.vowel = vowel;
    this.consonant = consonant;
    this.casesensitive = casesensitive;
    this.number = number;
    this.patterns = Collections.unmodifiableList(patterns);
  }

  public static AvroDict loadDefault() {
    try (InputStream in = AvroDict.class.getResourceAsStream("/avro_dict.bin")) {
      if (in == null) {
        throw new IllegalStateException("avro_dict.bin is missing from the classpath");
      }
      return load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load avro_dict.bin", e);
    }
  }

  public static AvroDict load(InputStream raw) throws IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
    byte[] magic = in.readNBytes(4);
    if (magic.length != 4
        || magic[0] != 'A'
        || magic[1] != 'V'
        || magic[2] != 'R'
        || magic[3] != '1') {
      throw new IOException("Bad avro_dict.bin magic");
    }
    int version = in.readUnsignedByte();
    if (version != 1) {
      throw new IOException("Unsupported avro dict version: " + version);
    }
    String vowel = readUtf8(in);
    String consonant = readUtf8(in);
    String casesensitive = readUtf8(in);
    String number = readUtf8(in);
    int patternCount = in.readInt();
    List<Pattern> patterns = new ArrayList<>(patternCount);
    for (int i = 0; i < patternCount; i++) {
      String find = readUtf8(in);
      String replace = readUtf8(in);
      int ruleCount = in.readInt();
      List<Rule> rules = new ArrayList<>(ruleCount);
      for (int r = 0; r < ruleCount; r++) {
        String ruleReplace = readUtf8(in);
        int matchCount = in.readInt();
        List<Match> matches = new ArrayList<>(matchCount);
        for (int m = 0; m < matchCount; m++) {
          int flags = in.readUnsignedByte();
          int type = flags & 0x01;
          int scope = (flags >> 1) & 0x07;
          boolean negative = (flags & 0x10) != 0;
          String value = readUtf8(in);
          matches.add(new Match(type, scope, negative, value));
        }
        rules.add(new Rule(ruleReplace, matches));
      }
      patterns.add(new Pattern(find, replace, rules, i));
    }
    return new AvroDict(vowel, consonant, casesensitive, number, patterns);
  }

  static String readUtf8(DataInputStream in) throws IOException {
    int length = in.readUnsignedShort();
    byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) {
      throw new IOException("Truncated UTF-8 string");
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
