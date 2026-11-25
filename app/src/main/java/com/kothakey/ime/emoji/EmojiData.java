package com.kothakey.ime.emoji;

import android.content.Context;
import android.graphics.Paint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EmojiData {

  public static final String[] CATEGORIES = {
    "recent",
    "smileys",
    "people",
    "animals",
    "food",
    "travel",
    "activities",
    "objects",
    "symbols",
    "flags"
  };

  public static final String[] CATEGORY_ICONS = {
    "\u23f3", "\u263a", "\u270b", "\ud83d\udc3b", "\ud83c\udf4e",
    "\u2708", "\u26bd", "\ud83d\udca1", "\u2764", "\ud83c\udff3"
  };

  public static final class Emoji {
    public final String category;
    public final String glyph;
    public final String[] skins;

    public Emoji(String category, String glyph, String[] skins) {
      this.category = category;
      this.glyph = glyph;
      this.skins = skins;
    }
  }

  private final Map<String, List<Emoji>> mByCategory = new LinkedHashMap<>();
  private final Paint mGlyphPaint = new Paint();
  private volatile boolean mReady;

  public void loadAsync(Context context) {
    new Thread(
            () -> {
              for (String cat : CATEGORIES) {
                mByCategory.put(cat, new ArrayList<>());
              }
              try (BufferedReader reader =
                  new BufferedReader(
                      new InputStreamReader(
                          context.getAssets().open("emoji/emoji.tsv"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                  String[] parts = line.split("\t", -1);
                  if (parts.length < 2) {
                    continue;
                  }
                  String[] skins =
                      parts.length > 2 && !parts[2].isEmpty() ? parts[2].split(",") : new String[0];
                  Emoji emoji = new Emoji(parts[0], parts[1], skins);
                  List<Emoji> list = mByCategory.get(parts[0]);
                  if (list != null) {
                    list.add(emoji);
                  }
                }
              } catch (IOException ignored) {
              }
              mReady = true;
            },
            "kothakey-emoji")
        .start();
  }

  public boolean ready() {
    return mReady;
  }

  public List<Emoji> category(String id, boolean filterGlyphs) {
    List<Emoji> src = mByCategory.get(id);
    if (src == null) {
      return List.of();
    }
    if (!filterGlyphs) {
      return src;
    }
    List<Emoji> out = new ArrayList<>();
    for (Emoji e : src) {
      if (mGlyphPaint.hasGlyph(e.glyph)) {
        out.add(e);
      }
    }
    return out;
  }

  public boolean hasGlyph(String emoji) {
    return mGlyphPaint.hasGlyph(emoji);
  }
}
