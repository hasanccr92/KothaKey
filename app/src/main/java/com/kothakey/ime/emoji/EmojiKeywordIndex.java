package com.kothakey.ime.emoji;

import android.content.Context;
import com.kothakey.ime.dict.WordDictionary;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Keyword lookup for FR-mode emoji suggestions. */
public final class EmojiKeywordIndex {

  private static final class Pair implements Comparable<Pair> {
    final String key;
    final String emoji;

    Pair(String key, String emoji) {
      this.key = key;
      this.emoji = emoji;
    }

    @Override
    public int compareTo(Pair o) {
      int c = key.compareTo(o.key);
      return c != 0 ? c : emoji.compareTo(o.emoji);
    }
  }

  private Pair[] mPairs = new Pair[0];
  private volatile boolean mReady;

  public void loadAsync(Context context) {
    new Thread(
            () -> {
              List<Pair> list = new ArrayList<>();
              loadFile(context, "emoji/keywords_en.tsv", list);
              loadFile(context, "emoji/keywords_fr.tsv", list);
              Collections.sort(list);
              mPairs = list.toArray(new Pair[0]);
              mReady = true;
            },
            "kothakey-emoji-kw")
        .start();
  }

  public List<String> suggest(String typed, int limit) {
    if (!mReady || typed == null || typed.isEmpty()) {
      return Collections.emptyList();
    }
    String key = WordDictionary.fold(typed);
    LinkedHashSet<String> exact = new LinkedHashSet<>();
    LinkedHashSet<String> prefix = new LinkedHashSet<>();
    Pair[] pairs = mPairs;
    int lo = 0;
    int hi = pairs.length;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (pairs[mid].key.compareTo(key) < 0) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    for (int i = lo; i < pairs.length; i++) {
      Pair p = pairs[i];
      if (!p.key.startsWith(key)) {
        break;
      }
      if (p.key.equals(key)) {
        exact.add(p.emoji);
      } else if (key.length() >= 3) {
        prefix.add(p.emoji);
      }
    }
    List<String> out = new ArrayList<>(limit);
    for (String e : exact) {
      if (out.size() >= limit) {
        return out;
      }
      out.add(e);
    }
    if (key.length() >= 3) {
      for (String e : prefix) {
        if (out.size() >= limit) {
          break;
        }
        out.add(e);
      }
    }
    return out;
  }

  private static void loadFile(Context context, String asset, List<Pair> dest) {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(context.getAssets().open(asset), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        int tab = line.indexOf('\t');
        if (tab <= 0) {
          continue;
        }
        String emoji = line.substring(0, tab);
        for (String raw : line.substring(tab + 1).split(",")) {
          String word = WordDictionary.fold(raw.trim());
          if (!word.isEmpty()) {
            dest.add(new Pair(word, emoji));
          }
        }
      }
    } catch (IOException ignored) {
    }
  }
}
