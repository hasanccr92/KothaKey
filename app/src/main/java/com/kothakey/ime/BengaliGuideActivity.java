package com.kothakey.ime;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kothakey.R;
import com.kothakey.ime.guide.BengaliChart;
import com.kothakey.ime.ui.KothaKeyTheme;
import java.util.List;

/** Native Bengali typing chart: what to type in Latin letters to get each Bengali letter. */
public class BengaliGuideActivity extends Activity {

  private KothaKeyTheme mTheme;
  private float mDensity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mTheme = new KothaKeyPrefs(this).theme();
    mDensity = getResources().getDisplayMetrics().density;
    setTitle(R.string.guide_title);

    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(mTheme.background);
    scroll.setFillViewport(true);
    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(16), dp(20), dp(16), dp(28));
    scroll.addView(
        page,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView title = text(getString(R.string.guide_title), 22, mTheme.label, true);
    page.addView(title);
    TextView intro = text(getString(R.string.guide_intro), 14, mTheme.hint, false);
    intro.setLineSpacing(dp(2), 1f);
    page.addView(intro, topMargin(8));

    for (BengaliChart.Section section : BengaliChart.sections()) {
      page.addView(
          text(getString(sectionTitle(section.kind)), 16, mTheme.label, true), topMargin(24));
      addSection(page, section);
    }

    page.addView(text(getString(R.string.guide_section_tips), 16, mTheme.label, true), topMargin(24));
    TextView tips = text(getString(R.string.guide_tips), 14, mTheme.hint, false);
    tips.setLineSpacing(dp(3), 1f);
    page.addView(tips, topMargin(8));

    setContentView(scroll);
  }

  private static int sectionTitle(BengaliChart.Kind kind) {
    switch (kind) {
      case VOWELS:
        return R.string.guide_section_vowels;
      case VOWEL_SIGNS:
        return R.string.guide_section_vowel_signs;
      case CONSONANTS:
        return R.string.guide_section_consonants;
      case SPECIAL:
        return R.string.guide_section_special;
      case CONJUNCTS:
        return R.string.guide_section_conjuncts;
      case NUMBERS:
        return R.string.guide_section_numbers;
      default:
        return R.string.guide_section_examples;
    }
  }

  private void addSection(LinearLayout page, BengaliChart.Section section) {
    List<BengaliChart.Entry> entries = section.entries;
    int columns = section.kind.columns;
    boolean wide = columns == 1;
    for (int start = 0; start < entries.size(); start += columns) {
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.HORIZONTAL);
      for (int c = 0; c < columns; c++) {
        int index = start + c;
        View cell;
        if (index < entries.size()) {
          BengaliChart.Entry e = entries.get(index);
          cell = wide ? wideCell(e) : cell(e);
        } else {
          cell = new View(this);
        }
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(cell, lp);
      }
      page.addView(
          row,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
  }

  /** A small tile: the Bengali letter above what to type. */
  private View cell(BengaliChart.Entry e) {
    LinearLayout box = tile();
    box.setGravity(Gravity.CENTER);
    TextView bn = text(e.bengali, 24, mTheme.label, false);
    bn.setGravity(Gravity.CENTER);
    TextView latin = text(e.latin, 14, mTheme.hint, false);
    latin.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    latin.setGravity(Gravity.CENTER);
    box.addView(bn);
    box.addView(latin);
    return box;
  }

  /** A full-width row: what to type, the result and a hint. */
  private View wideCell(BengaliChart.Entry e) {
    LinearLayout box = tile();
    box.setOrientation(LinearLayout.HORIZONTAL);
    box.setGravity(Gravity.CENTER_VERTICAL);
    TextView latin = text(e.latin, 16, mTheme.hint, false);
    latin.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    latin.setMinWidth(dp(72));
    TextView arrow = text("→", 16, mTheme.hint, false);
    arrow.setPadding(dp(6), 0, dp(10), 0);
    TextView bn = text(e.bengali, 26, mTheme.label, false);
    bn.setMinWidth(dp(48));
    box.addView(latin);
    box.addView(arrow);
    box.addView(bn);
    if (!e.note.isEmpty()) {
      TextView note = text(e.note, 12, mTheme.hint, false);
      note.setPadding(dp(8), 0, 0, 0);
      box.addView(
          note, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }
    return box;
  }

  private LinearLayout tile() {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(dp(8), dp(8), dp(8), dp(8));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(mTheme.key);
    bg.setCornerRadius(dp(10));
    box.setBackground(bg);
    return box;
  }

  private TextView text(String value, int sp, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    t.setTextColor(color);
    if (bold) {
      t.setTypeface(Typeface.DEFAULT_BOLD);
    }
    return t;
  }

  private LinearLayout.LayoutParams topMargin(int dp) {
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.topMargin = dp(dp);
    return lp;
  }

  private int dp(int value) {
    return Math.round(value * mDensity);
  }
}
