package com.kothakey.ime.keyboard;

public enum Arrangement {
  AZERTY("azerty"),
  QWERTY("qwerty");

  public final String id;

  Arrangement(String id) {
    this.id = id;
  }

  public static Arrangement fromId(String id) {
    return QWERTY.id.equals(id) ? QWERTY : AZERTY;
  }
}
