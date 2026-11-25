package com.kothakey.ime.keyboard;

public enum Language {
  FR("fr", "FR \u2022 EN"),
  BN("bn", "\u09ac\u09be\u0982\u09b2\u09be");

  public final String id;
  public final String spaceLabel;

  Language(String id, String spaceLabel) {
    this.id = id;
    this.spaceLabel = spaceLabel;
  }

  public Language other() {
    return this == FR ? BN : FR;
  }

  public static Language fromId(String id) {
    return "fr".equals(id) ? FR : BN;
  }
}
