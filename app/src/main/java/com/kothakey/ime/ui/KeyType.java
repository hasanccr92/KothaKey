package com.kothakey.ime.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

/** The two key lettering styles: the system font, or the Doto dot-matrix face. */
public final class KeyType {

  public static final String DEFAULT = "default";
  public static final String DOT = "dot";

  private static Typeface sDot;

  private KeyType() {}

  public static String[] ids() {
    return new String[] {DEFAULT, DOT};
  }

  public static String name(String id) {
    return DOT.equals(id) ? "Dot matrix" : "Default";
  }

  public static Typeface typeface(Context context, String id) {
    if (!DOT.equals(id)) {
      return Typeface.DEFAULT;
    }
    if (sDot == null) {
      try {
        sDot = Typeface.createFromAsset(context.getAssets(), "fonts/doto.ttf");
      } catch (RuntimeException ignored) {
        sDot = Typeface.MONOSPACE;
      }
    }
    return sDot == null ? Typeface.DEFAULT : sDot;
  }

  /** Doto has no Bengali, so those labels stay on the system font. */
  public static void apply(Paint paint, Typeface face, String text) {
    if (face == null || text == null || text.isEmpty() || paint.hasGlyph(text)) {
      paint.setTypeface(face == null ? Typeface.DEFAULT : face);
    } else {
      paint.setTypeface(Typeface.DEFAULT);
    }
  }
}
