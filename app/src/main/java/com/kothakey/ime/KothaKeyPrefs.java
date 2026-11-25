package com.kothakey.ime;

import android.content.Context;
import android.content.SharedPreferences;
import com.kothakey.ime.keyboard.Arrangement;
import com.kothakey.ime.keyboard.Language;
import com.kothakey.ime.ui.KeyType;
import com.kothakey.ime.ui.KothaKeyTheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent keyboard settings. */
public final class KothaKeyPrefs {

  private static final String PREFS = "kothakey";
  private static final String KEY_DARK = "dark";
  private static final String KEY_COLORWAY = "colorway";
  private static final String KEY_FONT = "key_font";
  private static final String KEY_NUMBER_ROW = "number_row";
  private static final String KEY_SOUND = "sound";
  private static final String KEY_VIBRATE = "vibrate";
  private static final String KEY_AUTO_PUNCT = "auto_punctuation";
  private static final String KEY_DOUBLE_SPACE = "double_space_period";
  private static final String KEY_LEARNED_EPOCH = "learned_epoch";
  private static final String KEY_LANG = "lang";
  private static final String KEY_ARRANGEMENT = "arrangement";
  private static final String KEY_RECENTS = "emoji_recents";
  private static final int RECENT_LIMIT = 40;

  private final SharedPreferences mPrefs;

  public KothaKeyPrefs(Context context) {
    mPrefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public boolean isDark() {
    return mPrefs.getBoolean(KEY_DARK, true);
  }

  public void setDark(boolean dark) {
    mPrefs.edit().putBoolean(KEY_DARK, dark).apply();
  }

  /** Dark or Light when the user has not picked a colorway yet. */
  public String colorway() {
    String id = mPrefs.getString(KEY_COLORWAY, "");
    if (id == null || id.isEmpty()) {
      return isDark() ? "dark" : "light";
    }
    return id;
  }

  public void setColorway(String id) {
    mPrefs.edit().putString(KEY_COLORWAY, id).putBoolean(KEY_DARK, KothaKeyTheme.byId(id).dark).apply();
  }

  public KothaKeyTheme theme() {
    return KothaKeyTheme.byId(colorway());
  }

  public String keyFont() {
    String id = mPrefs.getString(KEY_FONT, KeyType.DEFAULT);
    return KeyType.DOT.equals(id) ? KeyType.DOT : KeyType.DEFAULT;
  }

  public void setKeyFont(String id) {
    mPrefs.edit().putString(KEY_FONT, KeyType.DOT.equals(id) ? KeyType.DOT : KeyType.DEFAULT).apply();
  }

  public boolean numberRow() {
    return mPrefs.getBoolean(KEY_NUMBER_ROW, false);
  }

  public void setNumberRow(boolean on) {
    mPrefs.edit().putBoolean(KEY_NUMBER_ROW, on).apply();
  }

  public boolean sound() {
    return mPrefs.getBoolean(KEY_SOUND, true);
  }

  public void setSound(boolean on) {
    mPrefs.edit().putBoolean(KEY_SOUND, on).apply();
  }

  public boolean vibrate() {
    return mPrefs.getBoolean(KEY_VIBRATE, true);
  }

  public void setVibrate(boolean on) {
    mPrefs.edit().putBoolean(KEY_VIBRATE, on).apply();
  }

  /** Adds a space after punctuation and capitalises the next sentence. */
  public boolean autoPunctuation() {
    return mPrefs.getBoolean(KEY_AUTO_PUNCT, true);
  }

  public void setAutoPunctuation(boolean on) {
    mPrefs.edit().putBoolean(KEY_AUTO_PUNCT, on).apply();
  }

  /** Two quick taps on the space bar type a period (or dari) and a space. */
  public boolean doubleSpacePeriod() {
    return mPrefs.getBoolean(KEY_DOUBLE_SPACE, true);
  }

  public void setDoubleSpacePeriod(boolean on) {
    mPrefs.edit().putBoolean(KEY_DOUBLE_SPACE, on).apply();
  }

  /** Bumped by Settings when the learned Bengali words are cleared, so the keyboard reloads. */
  public int learnedEpoch() {
    return mPrefs.getInt(KEY_LEARNED_EPOCH, 0);
  }

  public void bumpLearnedEpoch() {
    mPrefs.edit().putInt(KEY_LEARNED_EPOCH, learnedEpoch() + 1).apply();
  }

  public Language language() {
    return Language.fromId(mPrefs.getString(KEY_LANG, Language.BN.id));
  }

  public void setLanguage(Language language) {
    mPrefs.edit().putString(KEY_LANG, language.id).apply();
  }

  public Arrangement arrangement() {
    return Arrangement.fromId(mPrefs.getString(KEY_ARRANGEMENT, Arrangement.AZERTY.id));
  }

  public void setArrangement(Arrangement arrangement) {
    mPrefs.edit().putString(KEY_ARRANGEMENT, arrangement.id).apply();
  }

  public List<String> recentEmoji() {
    String raw = mPrefs.getString(KEY_RECENTS, "");
    if (raw.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> out = new ArrayList<>();
    for (String part : raw.split("\u001f")) {
      if (!part.isEmpty()) {
        out.add(part);
      }
    }
    return out;
  }

  public void pushRecentEmoji(String emoji) {
    List<String> list = new ArrayList<>(recentEmoji());
    list.remove(emoji);
    list.add(0, emoji);
    while (list.size() > RECENT_LIMIT) {
      list.remove(list.size() - 1);
    }
    mPrefs.edit().putString(KEY_RECENTS, String.join("\u001f", list)).apply();
  }
}
