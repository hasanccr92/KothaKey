package com.kothakey.ime.ui;

import android.graphics.Color;

/**
 * Keyboard colorways. Dark and Light are KothaKey's own. Taiyaki, Radar, Particle and Matte Orbit
 * use the palettes described for Orbit Board 1.3.9 (Apache-2.0, Fahim Takrim). Matte Orbit's hex
 * values come from that project's {@code OrbitThemeHelper}. The other three follow the published
 * color descriptions: toasted biscuit, sonar teal, and a spark on obsidian.
 */
public final class KothaKeyTheme {

  public final String id;
  public final String name;
  public final boolean dark;
  public final int background;
  public final int strip;
  public final int key;
  public final int keyPressed;
  public final int fnKey;
  public final int enter;
  /** Accent used for the enter key and the settings preview. */
  public final int accent;
  public final int label;
  public final int hint;
  public final int spaceLabel;
  public final int popupBg;
  public final int popupLabel;
  public final int chip;
  public final int chipText;

  private KothaKeyTheme(
      String id,
      String name,
      boolean dark,
      int background,
      int strip,
      int key,
      int keyPressed,
      int fnKey,
      int enter,
      int label,
      int hint,
      int spaceLabel,
      int popupBg,
      int popupLabel,
      int chip,
      int chipText) {
    this.id = id;
    this.name = name;
    this.dark = dark;
    this.background = background;
    this.strip = strip;
    this.key = key;
    this.keyPressed = keyPressed;
    this.fnKey = fnKey;
    this.enter = enter;
    this.accent = enter;
    this.label = label;
    this.hint = hint;
    this.spaceLabel = spaceLabel;
    this.popupBg = popupBg;
    this.popupLabel = popupLabel;
    this.chip = chip;
    this.chipText = chipText;
  }

  public static KothaKeyTheme[] all() {
    return new KothaKeyTheme[] {dark(), light(), taiyaki(), radar(), particle(), matte()};
  }

  public static KothaKeyTheme byId(String id) {
    for (KothaKeyTheme theme : all()) {
      if (theme.id.equals(id)) {
        return theme;
      }
    }
    return dark();
  }

  public static KothaKeyTheme dark() {
    return new KothaKeyTheme(
        "dark",
        "Dark",
        true,
        Color.parseColor("#263238"),
        Color.parseColor("#1C2529"),
        Color.parseColor("#424A50"),
        Color.parseColor("#5C656C"),
        Color.parseColor("#374046"),
        Color.parseColor("#6FB3A8"),
        Color.parseColor("#F5F5F5"),
        Color.parseColor("#9E9E9E"),
        Color.parseColor("#B0BEC5"),
        Color.parseColor("#ECEFF1"),
        Color.parseColor("#212121"),
        Color.parseColor("#374046"),
        Color.parseColor("#EEEEEE"));
  }

  public static KothaKeyTheme light() {
    return new KothaKeyTheme(
        "light",
        "Light",
        false,
        Color.parseColor("#DCE1E5"),
        Color.parseColor("#ECEFF1"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#CFD8DC"),
        Color.parseColor("#C5CCD1"),
        Color.parseColor("#00897B"),
        Color.parseColor("#212121"),
        Color.parseColor("#78909C"),
        Color.parseColor("#546E7A"),
        Color.parseColor("#37474F"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#212121"));
  }

  /** Warm toasted-biscuit keys on a creamy background. */
  public static KothaKeyTheme taiyaki() {
    return new KothaKeyTheme(
        "taiyaki",
        "Taiyaki Bakery",
        false,
        Color.parseColor("#F6E7D4"),
        Color.parseColor("#EFE0CC"),
        Color.parseColor("#F3D7A1"),
        Color.parseColor("#E7C48A"),
        Color.parseColor("#E8C9A4"),
        Color.parseColor("#C9864A"),
        Color.parseColor("#3E2C22"),
        Color.parseColor("#8C6B52"),
        Color.parseColor("#5C4336"),
        Color.parseColor("#FFF8EE"),
        Color.parseColor("#3E2C22"),
        Color.parseColor("#E8C9A4"),
        Color.parseColor("#3E2C22"));
  }

  /** Deep water with a sonar-teal enter key. */
  public static KothaKeyTheme radar() {
    return new KothaKeyTheme(
        "radar",
        "Radar Wave",
        true,
        Color.parseColor("#071820"),
        Color.parseColor("#06141B"),
        Color.parseColor("#12343A"),
        Color.parseColor("#1A4A52"),
        Color.parseColor("#0E2A30"),
        Color.parseColor("#2EC4B6"),
        Color.parseColor("#E7FFFB"),
        Color.parseColor("#7EAAA4"),
        Color.parseColor("#B7E6E0"),
        Color.parseColor("#0C2428"),
        Color.parseColor("#E7FFFB"),
        Color.parseColor("#12343A"),
        Color.parseColor("#E7FFFB"));
  }

  /** Obsidian keys with a spark-coloured enter key. */
  public static KothaKeyTheme particle() {
    return new KothaKeyTheme(
        "particle",
        "Particle Burst",
        true,
        Color.parseColor("#120E16"),
        Color.parseColor("#0E0B12"),
        Color.parseColor("#2B2433"),
        Color.parseColor("#3A3144"),
        Color.parseColor("#221C2A"),
        Color.parseColor("#FF5A36"),
        Color.parseColor("#F7F2FF"),
        Color.parseColor("#B3A6C4"),
        Color.parseColor("#E4DCF2"),
        Color.parseColor("#1A1520"),
        Color.parseColor("#F7F2FF"),
        Color.parseColor("#2B2433"),
        Color.parseColor("#F7F2FF"));
  }

  /**
   * Nothing-style matte: obsidian {@code #1C1D20}, chalk {@code #EAE6E1}, ochre {@code #E2B438}.
   */
  public static KothaKeyTheme matte() {
    return new KothaKeyTheme(
        "matte",
        "Matte Orbit",
        true,
        Color.parseColor("#1C1D20"),
        Color.parseColor("#141517"),
        Color.parseColor("#2A2C31"),
        Color.parseColor("#3A3D44"),
        Color.parseColor("#232428"),
        Color.parseColor("#E2B438"),
        Color.parseColor("#EAE6E1"),
        Color.parseColor("#8E9094"),
        Color.parseColor("#EAE6E1"),
        Color.parseColor("#121212"),
        Color.parseColor("#EAE6E1"),
        Color.parseColor("#2A2C31"),
        Color.parseColor("#EAE6E1"));
  }
}
