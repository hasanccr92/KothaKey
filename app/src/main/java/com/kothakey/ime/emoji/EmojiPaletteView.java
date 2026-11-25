package com.kothakey.ime.emoji;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.kothakey.ime.KothaKeyPrefs;
import com.kothakey.ime.ui.KothaKeyTheme;
import java.util.ArrayList;
import java.util.List;

public class EmojiPaletteView extends LinearLayout {

  public interface Callback {
    void onEmoji(String emoji);

    void onAbc();

    void onSpace();

    void onDelete();
  }

  private final EmojiData mData;
  private final KothaKeyPrefs mPrefs;
  private final LinearLayout mTabs;
  private final GridView mGrid;
  private final Adapter mAdapter = new Adapter();
  private KothaKeyTheme mTheme = KothaKeyTheme.dark();
  private Callback mCallback;
  private String mCategory = "recent";
  private PopupWindow mSkinPopup;

  public EmojiPaletteView(Context context) {
    this(context, null);
  }

  public EmojiPaletteView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    mData = new EmojiData();
    mPrefs = new KothaKeyPrefs(context);
    mData.loadAsync(context);

    HorizontalScrollView tabScroll = new HorizontalScrollView(context);
    tabScroll.setHorizontalScrollBarEnabled(false);
    mTabs = new LinearLayout(context);
    mTabs.setOrientation(HORIZONTAL);
    tabScroll.addView(mTabs);

    mGrid = new GridView(context);
    mGrid.setNumColumns(8);
    mGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
    mGrid.setAdapter(mAdapter);
    mGrid.setOnItemClickListener(
        (parent, view, position, id) -> {
          EmojiData.Emoji emoji = mAdapter.getItem(position);
          if (emoji != null && mCallback != null) {
            mCallback.onEmoji(emoji.glyph);
            mPrefs.pushRecentEmoji(emoji.glyph);
            if ("recent".equals(mCategory)) {
              reload();
            }
          }
        });
    mGrid.setOnItemLongClickListener(
        (parent, view, position, id) -> {
          EmojiData.Emoji emoji = mAdapter.getItem(position);
          if (emoji != null && emoji.skins.length > 0) {
            showSkins(view, emoji);
            return true;
          }
          return false;
        });

    LinearLayout bottom = new LinearLayout(context);
    bottom.setOrientation(HORIZONTAL);
    bottom.addView(action("ABC", v -> { if (mCallback != null) mCallback.onAbc(); }), lp(1.2f));
    bottom.addView(action("\u2423", v -> { if (mCallback != null) mCallback.onSpace(); }), lp(3.6f));
    bottom.addView(action("\u232b", v -> { if (mCallback != null) mCallback.onDelete(); }), lp(1.4f));

    addView(tabScroll, new LayoutParams(LayoutParams.MATCH_PARENT, dp(40)));
    addView(mGrid, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
    addView(bottom, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
    buildTabs();
    applyTheme();
    postDelayed(this::reload, 250);
  }

  public void setCallback(Callback callback) {
    mCallback = callback;
  }

  public void setTheme(KothaKeyTheme theme) {
    mTheme = theme;
    applyTheme();
    reload();
  }

  public void showRecent() {
    select("recent");
  }

  private void buildTabs() {
    mTabs.removeAllViews();
    for (int i = 0; i < EmojiData.CATEGORIES.length; i++) {
      String id = EmojiData.CATEGORIES[i];
      TextView tab = new TextView(getContext());
      tab.setText(EmojiData.CATEGORY_ICONS[i]);
      tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      tab.setGravity(Gravity.CENTER);
      int pad = dp(10);
      tab.setPadding(pad, 0, pad, 0);
      tab.setOnClickListener(v -> select(id));
      tab.setTag(id);
      mTabs.addView(tab, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }
  }

  private void select(String id) {
    mCategory = id;
    highlightTabs();
    reload();
  }

  private void reload() {
    List<EmojiData.Emoji> items = new ArrayList<>();
    if ("recent".equals(mCategory)) {
      for (String glyph : mPrefs.recentEmoji()) {
        if (mData.hasGlyph(glyph)) {
          items.add(new EmojiData.Emoji("recent", glyph, new String[0]));
        }
      }
    } else {
      items.addAll(mData.category(mCategory, true));
    }
    mAdapter.setItems(items);
  }

  private void showSkins(View anchor, EmojiData.Emoji emoji) {
    dismissSkin();
    LinearLayout row = new LinearLayout(getContext());
    row.setOrientation(HORIZONTAL);
    row.setBackgroundColor(mTheme.popupBg);
    int pad = dp(8);
    row.setPadding(pad, pad, pad, pad);
    addSkin(row, emoji.glyph);
    for (String skin : emoji.skins) {
      addSkin(row, skin);
    }
    row.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    mSkinPopup = new PopupWindow(row, row.getMeasuredWidth(), row.getMeasuredHeight(), true);
    mSkinPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    mSkinPopup.setOutsideTouchable(true);
    mSkinPopup.showAsDropDown(anchor, 0, -anchor.getHeight() - row.getMeasuredHeight());
  }

  private void addSkin(LinearLayout row, String glyph) {
    TextView tv = new TextView(getContext());
    tv.setText(glyph);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
    tv.setPadding(dp(8), 0, dp(8), 0);
    tv.setOnClickListener(
        v -> {
          if (mCallback != null) {
            mCallback.onEmoji(glyph);
            mPrefs.pushRecentEmoji(glyph);
          }
          dismissSkin();
        });
    row.addView(tv);
  }

  private void dismissSkin() {
    if (mSkinPopup != null) {
      mSkinPopup.dismiss();
      mSkinPopup = null;
    }
  }

  private void applyTheme() {
    setBackgroundColor(mTheme.background);
    mGrid.setBackgroundColor(mTheme.background);
    highlightTabs();
    mAdapter.notifyDataSetChanged();
  }

  private void highlightTabs() {
    for (int i = 0; i < mTabs.getChildCount(); i++) {
      TextView tab = (TextView) mTabs.getChildAt(i);
      boolean on = mCategory.equals(tab.getTag());
      tab.setTextColor(on ? mTheme.label : mTheme.hint);
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

  private final class Adapter extends BaseAdapter {
    private List<EmojiData.Emoji> mItems = new ArrayList<>();

    void setItems(List<EmojiData.Emoji> items) {
      mItems = items;
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return mItems.size();
    }

    @Override
    public EmojiData.Emoji getItem(int position) {
      return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TextView tv = convertView instanceof TextView ? (TextView) convertView : new TextView(getContext());
      tv.setText(mItems.get(position).glyph);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
      tv.setGravity(Gravity.CENTER);
      tv.setTextColor(mTheme.label);
      tv.setMinHeight(dp(40));
      return tv;
    }
  }
}
