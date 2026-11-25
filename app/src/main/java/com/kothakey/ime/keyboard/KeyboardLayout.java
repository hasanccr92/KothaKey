package com.kothakey.ime.keyboard;

import java.util.ArrayList;
import java.util.List;

public final class KeyboardLayout {
  public final List<List<Key>> rows = new ArrayList<>();
  public final List<Key> keys = new ArrayList<>();

  public KeyboardLayout add(Key... row) {
    List<Key> list = new ArrayList<>();
    for (Key key : row) {
      list.add(key);
      keys.add(key);
    }
    rows.add(list);
    return this;
  }

  public void layout(float width, float height, float padH, float padV, float gapH, float gapV) {
    int n = rows.size();
    if (n == 0) {
      return;
    }
    float innerH = height - padV * 2;
    float rowH = (innerH - gapV * (n - 1)) / n;
    float y = padV;
    for (List<Key> row : rows) {
      float sum = 0f;
      for (Key key : row) {
        sum += key.weight;
      }
      float innerW = width - padH * 2 - gapH * (row.size() - 1);
      float x = padH;
      for (Key key : row) {
        key.x = x;
        key.y = y;
        key.w = innerW * (key.weight / sum);
        key.h = rowH;
        x += key.w + gapH;
      }
      y += rowH + gapV;
    }
  }

  /**
   * Returns the key under the point. Taps in the gaps resolve to the nearest key, except the
   * period and comma give their inner edge (the side facing the space bar) to the space bar, the
   * gap just above them goes to the letter row, and anything below the keyboard is space.
   */
  public Key hit(float x, float y) {
    if (rows.isEmpty()) {
      return null;
    }
    List<Key> bottom = rows.get(rows.size() - 1);
    Key space = find(bottom, Codes.SPACE);
    if (space != null && y >= space.y + space.h) {
      return space;
    }
    if (space != null && inSpaceEdge(x, y, space)) {
      return space;
    }
    for (Key key : keys) {
      if (sidePunct(key) && space != null) {
        if (inPunctTouch(key, space, x, y)) {
          return key;
        }
        continue;
      }
      if (key.contains(x, y)) {
        return key;
      }
    }
    if (rows.size() >= 2 && inGapAbovePunct(x, y, bottom)) {
      return nearest(rows.get(rows.size() - 2), x);
    }
    List<Key> bestRow = null;
    float bestRowDist = Float.MAX_VALUE;
    for (List<Key> row : rows) {
      if (row.isEmpty()) {
        continue;
      }
      Key first = row.get(0);
      float d = span(y, first.y, first.y + first.h);
      if (d < bestRowDist) {
        bestRowDist = d;
        bestRow = row;
      }
    }
    return bestRow == null ? null : nearest(bestRow, x);
  }

  /** The 30% of a period or comma that faces the space bar. Those taps belong to space. */
  private boolean inSpaceEdge(float x, float y, Key space) {
    for (Key key : keys) {
      if (!sidePunct(key) || y < key.y || y >= key.y + key.h) {
        continue;
      }
      float shrink = key.w * 0.3f;
      if (key.x + key.w <= space.x + 1f) {
        if (x >= key.x + key.w - shrink && x < key.x + key.w) {
          return true;
        }
      } else if (key.x >= space.x) {
        if (x >= key.x && x < key.x + shrink) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean inPunctTouch(Key key, Key space, float x, float y) {
    if (y < key.y || y >= key.y + key.h) {
      return false;
    }
    float shrink = key.w * 0.3f;
    float left = key.x;
    float right = key.x + key.w;
    if (key.x + key.w <= space.x + 1f) {
      right -= shrink;
    } else if (key.x >= space.x) {
      left += shrink;
    }
    return x >= left && x < right;
  }

  /** True when the tap sits in the gap above the period or the comma. */
  private boolean inGapAbovePunct(float x, float y, List<Key> bottom) {
    if (rows.size() < 2) {
      return false;
    }
    List<Key> letters = rows.get(rows.size() - 2);
    if (letters.isEmpty() || bottom.isEmpty()) {
      return false;
    }
    float gapTop = letters.get(0).y + letters.get(0).h;
    float gapBottom = bottom.get(0).y;
    if (y < gapTop || y >= gapBottom) {
      return false;
    }
    for (Key key : bottom) {
      if (sidePunct(key) && x >= key.x && x < key.x + key.w) {
        return true;
      }
    }
    return false;
  }

  private static Key nearest(List<Key> row, float x) {
    Key best = null;
    float bestDist = Float.MAX_VALUE;
    for (Key key : row) {
      float d = span(x, key.x, key.x + key.w);
      if (d < bestDist) {
        bestDist = d;
        best = key;
      }
    }
    return best;
  }

  private static Key find(List<Key> row, int code) {
    for (Key key : row) {
      if (key.code == code) {
        return key;
      }
    }
    return null;
  }

  private static boolean sidePunct(Key key) {
    return key.code == '.' || key.code == ',';
  }

  private static float span(float v, float lo, float hi) {
    if (v < lo) {
      return lo - v;
    }
    if (v > hi) {
      return v - hi;
    }
    return 0f;
  }
}
