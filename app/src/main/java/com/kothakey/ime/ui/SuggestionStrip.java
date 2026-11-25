package com.kothakey.ime.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class SuggestionStrip extends LinearLayout {

  public interface Listener {
    void onPick(String word);

    void onSettings();

    void onClipboard();

    void onEmoji();

    void onClipPick(String text);
  }

  private static final int CLIP_PREVIEW_CHARS = 40;

  private final HorizontalScrollView mScroll;
  private final LinearLayout mChips;
  private final TextView mEmoji;
  private final TextView mClipboard;
  private final TextView mSettings;
  private Listener mListener;
  private KothaKeyTheme mTheme = KothaKeyTheme.dark();
  private Typeface mFont = Typeface.DEFAULT;

  public SuggestionStrip(Context context) {
    this(context, null);
  }

  public SuggestionStrip(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);

    mScroll = new HorizontalScrollView(context);
    mScroll.setHorizontalScrollBarEnabled(false);
    mScroll.setFillViewport(true);
    mChips = new LinearLayout(context);
    mChips.setOrientation(HORIZONTAL);
    mChips.setGravity(Gravity.CENTER_VERTICAL);
    mScroll.addView(
        mChips, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

    int pad = dp(10);
    mEmoji = icon(context, "\u263a", pad, v -> {
      if (mListener != null) {
        mListener.onEmoji();
      }
    });
    mClipboard = icon(context, "\ud83d\udccb", pad, v -> {
      if (mListener != null) {
        mListener.onClipboard();
      }
    });
    mSettings = icon(context, "\u2699", pad, v -> {
      if (mListener != null) {
        mListener.onSettings();
      }
    });

    addView(mEmoji, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    addView(mScroll, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
    addView(mClipboard, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    addView(mSettings, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    applyTheme();
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  public void setTheme(KothaKeyTheme theme) {
    mTheme = theme;
    applyTheme();
  }

  public void setKeyFont(Typeface face) {
    mFont = face == null ? Typeface.DEFAULT : face;
    applyTheme();
  }

  public void show(List<String> words) {
    int count = words == null ? 0 : words.size();
    int padH = dp(14);
    int padV = dp(6);
    // Reuse the chip and divider views so a keystroke does not build a new row from scratch.
    while (mChips.getChildCount() > count * 2 - (count == 0 ? 0 : 1)) {
      mChips.removeViewAt(mChips.getChildCount() - 1);
    }
    for (int i = 0; i < count; i++) {
      String word = words.get(i);
      int chipAt = i * 2;
      TextView chip;
      if (chipAt < mChips.getChildCount() && mChips.getChildAt(chipAt) instanceof TextView) {
        chip = (TextView) mChips.getChildAt(chipAt);
      } else {
        chip = new TextView(getContext());
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        chip.setPadding(padH, padV, padH, padV);
        mChips.addView(chip, chipAt);
      }
      chip.setText(word);
      chip.setTextColor(mTheme.chipText);
      chip.setTypeface(mFont);
      Typeface face = chip.getPaint().hasGlyph(word) ? mFont : Typeface.DEFAULT;
      chip.setTypeface(i == 0 ? Typeface.create(face, Typeface.BOLD) : face);
      chip.setOnClickListener(
          v -> {
            if (mListener != null) {
              mListener.onPick(word);
            }
          });
      if (i + 1 < count) {
        int dividerAt = chipAt + 1;
        View divider;
        if (dividerAt < mChips.getChildCount() && !(mChips.getChildAt(dividerAt) instanceof TextView)) {
          divider = mChips.getChildAt(dividerAt);
        } else {
          divider = new View(getContext());
          LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(18));
          lp.gravity = Gravity.CENTER_VERTICAL;
          divider.setLayoutParams(lp);
          mChips.addView(divider, dividerAt);
        }
        divider.setBackgroundColor(mTheme.hint);
      }
    }
  }

  /** Shows the most recent copied text as a single tap-to-paste chip. */
  public void showClip(String text) {
    mChips.removeAllViews();
    if (text == null || text.isEmpty()) {
      return;
    }
    String flat = text.replaceAll("\\s+", " ").trim();
    if (flat.length() > CLIP_PREVIEW_CHARS) {
      flat = flat.substring(0, CLIP_PREVIEW_CHARS).trim() + "\u2026";
    }
    TextView chip = new TextView(getContext());
    chip.setText("\ud83d\udccb  " + flat);
    chip.setTextColor(mTheme.chipText);
    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    chip.setSingleLine(true);
    chip.setEllipsize(TextUtils.TruncateAt.END);
    chip.setGravity(Gravity.CENTER_VERTICAL);
    chip.setPadding(dp(14), 0, dp(14), 0);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(mTheme.chip);
    bg.setCornerRadius(dp(16));
    bg.setStroke(dp(1), mTheme.hint);
    chip.setBackground(bg);
    chip.setOnClickListener(
        v -> {
          if (mListener != null) {
            mListener.onClipPick(text);
          }
        });
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(32));
    lp.gravity = Gravity.CENTER_VERTICAL;
    lp.leftMargin = dp(4);
    lp.rightMargin = dp(4);
    mChips.addView(chip, lp);
  }

  private void applyTheme() {
    setBackgroundColor(mTheme.strip);
    mEmoji.setTextColor(mTheme.spaceLabel);
    mClipboard.setTextColor(mTheme.spaceLabel);
    mSettings.setTextColor(mTheme.spaceLabel);
    show(null);
  }

  private static TextView icon(Context context, String label, int pad, OnClickListener listener) {
    TextView tv = new TextView(context);
    tv.setText(label);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    tv.setGravity(Gravity.CENTER);
    tv.setPadding(pad, 0, pad, 0);
    tv.setOnClickListener(listener);
    return tv;
  }

  private int dp(int v) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
  }
}
