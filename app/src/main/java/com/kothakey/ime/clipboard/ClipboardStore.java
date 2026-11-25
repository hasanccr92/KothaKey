package com.kothakey.ime.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent clipboard history, fed by the system clipboard and by hold-C. */
public final class ClipboardStore {

  public interface Listener {
    void onClipboardChanged();
  }

  private static final String PREFS = "kothakey_clipboard";
  private static final String KEY_ITEMS = "items";
  private static final char SEP = '\u001e';
  private static final int MAX = 50;
  private static final int MAX_CHARS = 8000;

  private final Context mApp;
  private final ClipboardManager mClipboard;
  private final SharedPreferences mPrefs;
  private final List<String> mItems = new ArrayList<>();
  private final ClipboardManager.OnPrimaryClipChangedListener mClipListener =
      this::onPrimaryClipChanged;
  private Listener mListener;
  private boolean mSelfWrite;

  public ClipboardStore(Context context) {
    mApp = context.getApplicationContext();
    mClipboard = (ClipboardManager) mApp.getSystemService(Context.CLIPBOARD_SERVICE);
    mPrefs = mApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    mItems.addAll(load());
    String current = systemText();
    if (!current.isEmpty() && (mItems.isEmpty() || !current.equals(mItems.get(0)))) {
      mItems.remove(current);
      mItems.add(0, current);
      while (mItems.size() > MAX) {
        mItems.remove(mItems.size() - 1);
      }
      persist();
    }
    if (mClipboard != null) {
      mClipboard.addPrimaryClipChangedListener(mClipListener);
    }
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  public void close() {
    if (mClipboard != null) {
      mClipboard.removePrimaryClipChangedListener(mClipListener);
    }
  }

  public List<String> items() {
    return Collections.unmodifiableList(mItems);
  }

  public String latest() {
    return mItems.isEmpty() ? "" : mItems.get(0);
  }

  public void push(String raw) {
    String text = sanitize(raw);
    if (text.isEmpty()) {
      return;
    }
    mItems.remove(text);
    mItems.add(0, text);
    while (mItems.size() > MAX) {
      mItems.remove(mItems.size() - 1);
    }
    persist();
    writeSystemClip(text);
    notifyChanged();
  }

  public void remove(String text) {
    if (mItems.remove(text)) {
      persist();
      notifyChanged();
    }
  }

  public void clear() {
    mItems.clear();
    persist();
    notifyChanged();
  }

  public String systemText() {
    if (mClipboard == null || !mClipboard.hasPrimaryClip()) {
      return "";
    }
    ClipData clip = mClipboard.getPrimaryClip();
    if (clip == null || clip.getItemCount() == 0) {
      return "";
    }
    CharSequence text = clip.getItemAt(0).coerceToText(mApp);
    return text == null ? "" : sanitize(text.toString());
  }

  private void onPrimaryClipChanged() {
    if (mSelfWrite) {
      return;
    }
    String text = systemText();
    if (text.isEmpty()) {
      return;
    }
    if (!mItems.isEmpty() && text.equals(mItems.get(0))) {
      return;
    }
    mItems.remove(text);
    mItems.add(0, text);
    while (mItems.size() > MAX) {
      mItems.remove(mItems.size() - 1);
    }
    persist();
    notifyChanged();
  }

  private void writeSystemClip(String text) {
    if (mClipboard == null) {
      return;
    }
    mSelfWrite = true;
    try {
      mClipboard.setPrimaryClip(ClipData.newPlainText("kothakey", text));
    } finally {
      mSelfWrite = false;
    }
  }

  private List<String> load() {
    String raw = mPrefs.getString(KEY_ITEMS, "");
    if (raw.isEmpty()) {
      return new ArrayList<>();
    }
    List<String> out = new ArrayList<>();
    for (String part : raw.split(String.valueOf(SEP), -1)) {
      if (!part.isEmpty()) {
        out.add(part);
      }
    }
    return out;
  }

  private void persist() {
    mPrefs.edit().putString(KEY_ITEMS, String.join(String.valueOf(SEP), mItems)).apply();
  }

  private void notifyChanged() {
    if (mListener != null) {
      mListener.onClipboardChanged();
    }
  }

  private static String sanitize(String raw) {
    if (raw == null) {
      return "";
    }
    String text = raw.replace(SEP, ' ').trim();
    if (text.length() > MAX_CHARS) {
      text = text.substring(0, MAX_CHARS);
    }
    return text;
  }
}
