package com.kothakey.ime.clipboard;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kothakey.ime.ui.KothaKeyTheme;
import java.util.List;

public class ClipboardPanelView extends LinearLayout {

  public interface Callback {
    void onPasteItem(String text);

    void onAbc();

    void onSpace();

    void onDelete();
  }

  private final LinearLayout mList;
  private final TextView mEmpty;
  private final TextView mClear;
  private KothaKeyTheme mTheme = KothaKeyTheme.dark();
  private Callback mCallback;
  private ClipboardStore mStore;

  public ClipboardPanelView(Context context) {
    this(context, null);
  }

  public ClipboardPanelView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);

    LinearLayout header = new LinearLayout(context);
    header.setOrientation(HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = new TextView(context);
    title.setText("Clipboard");
    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    title.setPadding(dp(12), 0, dp(12), 0);
    title.setTag("title");
    mClear = new TextView(context);
    mClear.setText("Clear");
    mClear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    mClear.setPadding(dp(16), dp(8), dp(16), dp(8));
    mClear.setOnClickListener(
        v -> {
          if (mStore != null) {
            mStore.clear();
            reload();
          }
        });
    header.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
    header.addView(mClear);

    mEmpty = new TextView(context);
    mEmpty.setText("Copied text will appear here.\nHold C to copy, hold V to paste.");
    mEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    mEmpty.setPadding(dp(16), dp(24), dp(16), dp(24));
    mEmpty.setGravity(Gravity.CENTER);

    mList = new LinearLayout(context);
    mList.setOrientation(VERTICAL);
    ScrollView scroll = new ScrollView(context);
    scroll.addView(mList, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    LinearLayout bottom = new LinearLayout(context);
    bottom.setOrientation(HORIZONTAL);
    bottom.addView(action("ABC", v -> { if (mCallback != null) mCallback.onAbc(); }), lp(1.2f));
    bottom.addView(action("\u2423", v -> { if (mCallback != null) mCallback.onSpace(); }), lp(3.6f));
    bottom.addView(action("\u232b", v -> { if (mCallback != null) mCallback.onDelete(); }), lp(1.4f));

    addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(40)));
    addView(mEmpty, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
    addView(bottom, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
    applyTheme();
  }

  public void setCallback(Callback callback) {
    mCallback = callback;
  }

  public void bind(ClipboardStore store) {
    mStore = store;
    reload();
  }

  public void setTheme(KothaKeyTheme theme) {
    mTheme = theme;
    applyTheme();
    reload();
  }

  public void reload() {
    mList.removeAllViews();
    List<String> items = mStore == null ? List.of() : mStore.items();
    mEmpty.setVisibility(items.isEmpty() ? VISIBLE : GONE);
    int pad = dp(12);
    for (String item : items) {
      TextView card = new TextView(getContext());
      card.setText(item);
      card.setTextColor(mTheme.chipText);
      card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
      card.setMaxLines(3);
      card.setPadding(pad, pad, pad, pad);
      card.setBackgroundColor(mTheme.key);
      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      lp.setMargins(dp(8), dp(6), dp(8), dp(6));
      card.setLayoutParams(lp);
      card.setOnClickListener(
          v -> {
            if (mCallback != null) {
              mCallback.onPasteItem(item);
            }
          });
      card.setOnLongClickListener(
          v -> {
            if (mStore != null) {
              mStore.remove(item);
              reload();
            }
            return true;
          });
      mList.addView(card);
    }
  }

  private void applyTheme() {
    setBackgroundColor(mTheme.background);
    mEmpty.setTextColor(mTheme.hint);
    mClear.setTextColor(mTheme.spaceLabel);
    View title = findViewWithTag("title");
    if (title instanceof TextView) {
      ((TextView) title).setTextColor(mTheme.label);
    }
  }

  private TextView action(String label, OnClickListener listener) {
    TextView tv = new TextView(getContext());
    tv.setText(label);
    tv.setTextColor(mTheme.label);
    tv.setGravity(Gravity.CENTER);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    tv.setOnClickListener(listener);
    return tv;
  }

  private LayoutParams lp(float weight) {
    return new LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
  }

  private int dp(int v) {
    return Math.round(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
  }
}
