package com.kothakey.ime.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import com.kothakey.ime.ui.KeyPopupOverlay;
import com.kothakey.ime.ui.KeyType;
import com.kothakey.ime.ui.KothaKeyTheme;

public class KothaKeyKeyboardView extends View {

  public interface Callback {
    void onChar(int codePoint);

    void onDelete();

    void onSpace();

    void onEnter();

    void onSymbols();

    void onPage(Layouts.Page page);

    void onEmoji();

    void onLanguageSwitch();

    void onCopy();

    void onPaste();

    /** Finger went down on a key. Feedback belongs here, not on release. */
    void onPress();

    /** The user tapped or held shift themselves, so auto-caps should leave it alone for now. */
    void onManualShift();
  }

  /** Shown at the end of the c-key popup so a hold can still copy once accents are listed. */
  static final String COPY_OPTION = "copy";

  public static final int SHIFT_OFF = 0;
  public static final int SHIFT_ONESHOT = 1;
  public static final int SHIFT_CAPS = 2;

  private static final long LONG_PRESS_MS = 400L;
  private static final long CLIP_HOLD_MS = 1000L;
  private static final long REPEAT_START_MS = 400L;
  private static final long REPEAT_MS = 50L;
  private static final long PREVIEW_MS = 50L;

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private final Paint mKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mHintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF mRect = new RectF();

  private KothaKeyTheme mTheme = KothaKeyTheme.dark();
  private Typeface mKeyFont = Typeface.DEFAULT;
  private KeyboardLayout mLayout = new KeyboardLayout();
  private Layouts.Page mPage = Layouts.Page.LETTERS;
  private Language mLanguage = Language.BN;
  private boolean mNumberRow;
  private Arrangement mArrangement = Arrangement.AZERTY;
  private Callback mCallback;

  private int mShift = SHIFT_OFF;
  private static final int INVALID_POINTER = -1;

  private Key mDownKey;
  private int mActiveId = INVALID_POINTER;
  private boolean mGestureUsed;
  private boolean mLongFired;
  private float mDownX;
  private float mDownY;
  private float mLastX;
  private int mPopupIndex = -1;
  private String[] mPopupChars;
  private KeyPopupOverlay mOverlay;
  private final int[] mLoc = new int[2];
  private final int[] mOverlayLoc = new int[2];
  private float mRadius;
  private float mSwipeSlop;
  private float mPadH;
  private float mPadV;
  private float mGapH;
  private float mGapV;

  private final Runnable mLongPress = this::onLongPress;
  private final Runnable mShiftHold = this::onShiftHold;
  private final Runnable mClipHold = this::onClipHold;
  private final Runnable mRepeat =
      new Runnable() {
        @Override
        public void run() {
          if (mDownKey != null && mDownKey.code == Codes.DELETE && mCallback != null) {
            mCallback.onPress();
            mCallback.onDelete();
            mHandler.postDelayed(this, REPEAT_MS);
          }
        }
      };
  private final Runnable mHidePreview = this::hideBubble;

  public KothaKeyKeyboardView(Context context) {
    this(context, null);
  }

  public KothaKeyKeyboardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public KothaKeyKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mLabelPaint.setTextAlign(Paint.Align.CENTER);
    mHintPaint.setTextAlign(Paint.Align.CENTER);
    mRadius = dp(6);
    mSwipeSlop = dp(40);
    mPadH = dp(4);
    mPadV = dp(6);
    mGapH = dp(6);
    mGapV = dp(10);
    rebuild();
  }

  public void setCallback(Callback callback) {
    mCallback = callback;
  }

  public void setTheme(KothaKeyTheme theme) {
    mTheme = theme;
    invalidate();
  }

  public void setKeyFont(Typeface face) {
    mKeyFont = face == null ? Typeface.DEFAULT : face;
    invalidate();
  }

  public void setNumberRow(boolean on) {
    if (mNumberRow != on) {
      mNumberRow = on;
      rebuild();
    }
  }

  public void setArrangement(Arrangement arrangement) {
    if (arrangement != null && mArrangement != arrangement) {
      mArrangement = arrangement;
      rebuild();
    }
  }

  public Language language() {
    return mLanguage;
  }

  public void setLanguage(Language language) {
    if (mLanguage != language) {
      mLanguage = language;
      if (mPage == Layouts.Page.LETTERS) {
        rebuild();
      } else {
        invalidate();
      }
    }
  }

  public Layouts.Page page() {
    return mPage;
  }

  public void setPage(Layouts.Page page) {
    mPage = page;
    rebuild();
  }

  public int shift() {
    return mShift;
  }

  public void setShift(int shift) {
    if (mShift != shift) {
      mShift = shift;
      invalidate();
    }
  }

  public void rebuild() {
    clearPreview();
    mLayout = Layouts.create(mPage, mLanguage, mNumberRow, mArrangement);
    if (getWidth() > 0 && getHeight() > 0) {
      mLayout.layout(getWidth(), getHeight(), mPadH, mPadV, mGapH, mGapV);
    }
    requestLayout();
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int rows = Math.max(4, mLayout.rows.size());
    int height = (int) (rows * dp(56) + mPadV * 2);
    setMeasuredDimension(width, height);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    mLayout.layout(w, h, mPadH, mPadV, mGapH, mGapV);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawColor(mTheme.background);
    for (Key key : mLayout.keys) {
      drawKey(canvas, key, key == mDownKey && !mGestureUsed);
    }
  }

  private void drawKey(Canvas canvas, Key key, boolean pressed) {
    mRect.set(key.x, key.y, key.x + key.w, key.y + key.h);
    int fill = mTheme.key;
    if (key.kind == Key.ENTER) {
      fill = mTheme.enter;
    } else if (key.kind == Key.FUNCTION || key.kind == Key.SPACE) {
      fill = mTheme.fnKey;
    }
    if (key.code == Codes.SHIFT && mShift != SHIFT_OFF) {
      fill = mTheme.keyPressed;
    }
    if (pressed) {
      fill = key.kind == Key.ENTER ? mTheme.enter : mTheme.keyPressed;
    }
    mKeyPaint.setColor(fill);
    canvas.drawRoundRect(mRect, mRadius, mRadius, mKeyPaint);

    String label = keyLabel(key);
    KeyType.apply(mLabelPaint, mKeyFont, label);
    mLabelPaint.setColor(key.kind == Key.ENTER ? 0xFFFFFFFF : mTheme.label);
    mLabelPaint.setTextSize(key.kind == Key.SPACE ? dp(13) : key.kind == Key.FUNCTION ? dp(16) : dp(20));
    if (key.kind == Key.SPACE) {
      mLabelPaint.setColor(mTheme.spaceLabel);
    }
    float cx = key.x + key.w / 2f;
    float cy = key.y + key.h / 2f - (mLabelPaint.descent() + mLabelPaint.ascent()) / 2f;
    canvas.drawText(label, cx, cy, mLabelPaint);

    if (key.hint != null && !key.hint.isEmpty() && mPage == Layouts.Page.LETTERS) {
      mHintPaint.setColor(mTheme.hint);
      KeyType.apply(mHintPaint, mKeyFont, key.hint);
      mHintPaint.setTextSize(dp(10));
      canvas.drawText(key.hint, key.x + key.w - dp(10), key.y + dp(14), mHintPaint);
    }
    if (key.popup != null && key.popup.length > 1 && key.hint == null) {
      mHintPaint.setColor(mTheme.hint);
      mHintPaint.setTextSize(dp(8));
      canvas.drawText("\u25b2", key.x + key.w - dp(10), key.y + dp(12), mHintPaint);
    }
    if (key.code == Codes.SHIFT && mShift == SHIFT_CAPS) {
      mKeyPaint.setColor(mTheme.label);
      float barW = dp(12);
      float barY = key.y + key.h - dp(10);
      canvas.drawRoundRect(
          cx - barW / 2f, barY, cx + barW / 2f, barY + dp(3), dp(1), dp(1), mKeyPaint);
    }
  }

  private String keyLabel(Key key) {
    if (key.code == Codes.SHIFT) {
      return "\u21e7";
    }
    if (key.kind == Key.SPACE) {
      return mLanguage.spaceLabel;
    }
    if (key.isLetter() && mShift != SHIFT_OFF) {
      return KeyCase.apply(key.label, true);
    }
    return key.label;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        // A fresh gesture: drop any stale state without firing it.
        cancelHeldKey();
        beginPress(event, event.getActionIndex());
        return true;
      case MotionEvent.ACTION_POINTER_DOWN:
        // Another finger landed while one is still down (fast two-thumb typing).
        // Commit the held key right now so no letter is lost, then track the new finger.
        commitHeldKey();
        beginPress(event, event.getActionIndex());
        return true;
      case MotionEvent.ACTION_MOVE:
        if (mActiveId != INVALID_POINTER) {
          int idx = event.findPointerIndex(mActiveId);
          if (idx >= 0) {
            handleMove(event.getX(idx));
          }
        }
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        // Fingers that were already rolled over have no active key left; ignore them.
        if (event.getPointerId(event.getActionIndex()) == mActiveId) {
          finishPress();
        }
        return true;
      case MotionEvent.ACTION_CANCEL:
        cancelHeldKey();
        clearPreview();
        invalidate();
        return true;
      default:
        return super.onTouchEvent(event);
    }
  }

  private void beginPress(MotionEvent event, int index) {
    float x = event.getX(index);
    float y = event.getY(index);
    mActiveId = event.getPointerId(index);
    mDownKey = mLayout.hit(x, y);
    mDownX = x;
    mDownY = y;
    mLastX = x;
    mGestureUsed = false;
    mLongFired = false;
    mPopupChars = null;
    mPopupIndex = -1;
    if (mDownKey != null && mCallback != null) {
      mCallback.onPress();
    }
    if (mDownKey != null && mDownKey.isLetter()) {
      showPreview(mDownKey);
    } else {
      clearPreview();
    }
    if (mDownKey != null) {
      if (mDownKey.code == Codes.DELETE) {
        fireKey(mDownKey);
        mHandler.postDelayed(mRepeat, REPEAT_START_MS);
      } else if (mDownKey.code == Codes.SHIFT) {
        mHandler.postDelayed(mShiftHold, LONG_PRESS_MS);
      } else if (mDownKey.kind == Key.SPACE) {
        // wait for slide or tap
      } else {
        mHandler.postDelayed(mLongPress, LONG_PRESS_MS);
        if (wantsClipHold(mDownKey)) {
          mHandler.postDelayed(mClipHold, CLIP_HOLD_MS);
        }
      }
    }
    invalidate();
  }

  private void handleMove(float x) {
    mLastX = x;
    if (mPopupChars != null) {
      mPopupIndex = popupIndex(x);
      if (mOverlay != null) {
        mOverlay.setSelected(mPopupIndex);
      }
      invalidate();
      return;
    }
    if (mDownKey != null && mDownKey.kind == Key.SPACE && !mGestureUsed) {
      if (Math.abs(x - mDownX) > mSwipeSlop) {
        mGestureUsed = true;
        mHandler.removeCallbacks(mLongPress);
        if (mCallback != null) {
          mCallback.onLanguageSwitch();
        }
        invalidate();
      }
    }
  }

  /** The active finger lifted: fire its key. */
  private void finishPress() {
    cancelHolds();
    Key key = mDownKey;
    if (key != null && !mGestureUsed) {
      if (mPopupChars != null) {
        int idx = mPopupIndex >= 0 ? mPopupIndex : 0;
        commitPopup(mPopupChars[idx]);
      } else if (key.code == Codes.SHIFT) {
        toggleShiftTap();
      } else if (key.code != Codes.DELETE) {
        fireKey(key);
      }
    }
    if (key != null && key.isLetter() && mPopupChars == null && !mGestureUsed) {
      schedulePreviewHide();
    } else {
      clearPreview();
    }
    resetPress();
    invalidate();
  }

  /** A second finger landed: fire the key the previous finger is still holding. */
  private void commitHeldKey() {
    Key key = mDownKey;
    cancelHolds();
    if (key != null && !mGestureUsed) {
      if (mPopupChars != null) {
        int idx = mPopupIndex >= 0 ? mPopupIndex : 0;
        commitPopup(mPopupChars[idx]);
      } else if (key.code == Codes.SHIFT) {
        // Shift held by one thumb while the other types: capitalise the next letter.
        if (mShift == SHIFT_OFF) {
          setShift(SHIFT_ONESHOT);
        }
      } else if (key.code != Codes.DELETE) {
        fireKey(key);
      }
    }
    resetPress();
  }

  /** Drop the held key without firing it (new gesture or cancelled touch). */
  private void cancelHeldKey() {
    cancelHolds();
    resetPress();
  }

  private void cancelHolds() {
    mHandler.removeCallbacks(mLongPress);
    mHandler.removeCallbacks(mShiftHold);
    mHandler.removeCallbacks(mClipHold);
    mHandler.removeCallbacks(mRepeat);
  }

  private void resetPress() {
    mDownKey = null;
    mPopupChars = null;
    mPopupIndex = -1;
    mGestureUsed = false;
    mActiveId = INVALID_POINTER;
  }

  public void setPopupOverlay(KeyPopupOverlay overlay) {
    mOverlay = overlay;
  }

  /** Where this view sits inside the overlay, so key rectangles can be handed over. */
  private void overlayOffset(float[] out) {
    getLocationInWindow(mLoc);
    mOverlay.getLocationInWindow(mOverlayLoc);
    out[0] = mLoc[0] - mOverlayLoc[0];
    out[1] = mLoc[1] - mOverlayLoc[1];
  }

  private final float[] mOffset = new float[2];

  /** Gray bubble above the key while the finger is down. */
  private void showPreview(Key key) {
    cancelPreviewHide();
    if (mOverlay == null) {
      return;
    }
    overlayOffset(mOffset);
    mOverlay.showPreview(
        key.x + mOffset[0], key.y + mOffset[1], key.w, key.h, keyLabel(key));
  }

  /** The held key opens its alternatives in a green bubble. */
  private void showOptions(Key key, String[] chars, int selected) {
    cancelPreviewHide();
    if (mOverlay == null) {
      return;
    }
    overlayOffset(mOffset);
    mOverlay.showOptions(
        key.x + mOffset[0], key.y + mOffset[1], key.w, key.h, chars, selected);
  }

  private void cancelPreviewHide() {
    mHandler.removeCallbacks(mHidePreview);
  }

  /** Hides the bubble a moment after release so quick taps still show it. */
  private void schedulePreviewHide() {
    mHandler.removeCallbacks(mHidePreview);
    mHandler.postDelayed(mHidePreview, PREVIEW_MS);
  }

  private void hideBubble() {
    if (mOverlay != null) {
      mOverlay.hide();
    }
  }

  private void clearPreview() {
    cancelPreviewHide();
    hideBubble();
  }

  @Override
  protected void onDetachedFromWindow() {
    clearPreview();
    super.onDetachedFromWindow();
  }

  private void onLongPress() {
    if (mDownKey == null || mGestureUsed) {
      return;
    }
    if (mDownKey.popup != null && mDownKey.popup.length > 0) {
      mLongFired = true;
      mPopupChars = withCopyOption(shiftedPopup(mDownKey.popup));
      mPopupIndex = 0;
      showOptions(mDownKey, mPopupChars, mPopupIndex);
      invalidate();
    }
  }

  private void onShiftHold() {
    if (mDownKey != null && mDownKey.code == Codes.SHIFT) {
      mGestureUsed = true;
      setShift(SHIFT_CAPS);
      if (mCallback != null) {
        mCallback.onManualShift();
      }
    }
  }

  private void onClipHold() {
    if (mDownKey == null || mGestureUsed || mCallback == null) {
      return;
    }
    int code = mDownKey.code;
    if (code == 'c' || code == 'C') {
      mGestureUsed = true;
      mPopupChars = null;
      clearPreview();
      mCallback.onCopy();
      invalidate();
    } else if (code == 'v' || code == 'V') {
      mGestureUsed = true;
      mPopupChars = null;
      clearPreview();
      mCallback.onPaste();
      invalidate();
    }
  }

  /** Paste still uses a one-second hold. Copy does too, unless the key lists accents. */
  private static boolean wantsClipHold(Key key) {
    if (key == null) {
      return false;
    }
    if (key.code == 'v' || key.code == 'V') {
      return true;
    }
    boolean copy = key.code == 'c' || key.code == 'C';
    return copy && (key.popup == null || key.popup.length == 0);
  }

  private void toggleShiftTap() {
    if (mShift == SHIFT_OFF) {
      setShift(SHIFT_ONESHOT);
    } else {
      setShift(SHIFT_OFF);
    }
    if (mCallback != null) {
      mCallback.onManualShift();
    }
  }

  private void fireKey(Key key) {
    if (mCallback == null) {
      return;
    }
    switch (key.code) {
      case Codes.DELETE:
        mCallback.onDelete();
        return;
      case Codes.SPACE:
        mCallback.onSpace();
        return;
      case Codes.ENTER:
        mCallback.onEnter();
        return;
      case Codes.SYMBOLS:
        mCallback.onSymbols();
        return;
      case Codes.EMOJI:
        mCallback.onEmoji();
        return;
      case Codes.SYM1:
        mCallback.onPage(Layouts.Page.SYMBOLS1);
        return;
      case Codes.SYM2:
        mCallback.onPage(Layouts.Page.SYMBOLS2);
        return;
      case Codes.NUMPAD:
        mCallback.onPage(Layouts.Page.NUMPAD);
        return;
      case Codes.SHIFT:
        return;
      default:
        int cp = KeyCase.apply(key.code, key.isLetter() && mShift != SHIFT_OFF);
        if (key.isLetter() && mShift == SHIFT_ONESHOT) {
          setShift(SHIFT_OFF);
        }
        mCallback.onChar(cp);
    }
  }

  private void commitPopup(String text) {
    if (mCallback == null || text == null || text.isEmpty()) {
      return;
    }
    if (COPY_OPTION.equals(text)) {
      if (mShift == SHIFT_ONESHOT) {
        setShift(SHIFT_OFF);
      }
      mCallback.onCopy();
      return;
    }
    int cp = KeyCase.apply(text.codePointAt(0), mShift != SHIFT_OFF);
    if (mShift == SHIFT_ONESHOT) {
      setShift(SHIFT_OFF);
    }
    mCallback.onChar(cp);
  }

  /** French c lists its accents, and copy sits on the end of that list instead of replacing it. */
  private String[] withCopyOption(String[] popup) {
    if (mDownKey == null || (mDownKey.code != 'c' && mDownKey.code != 'C')) {
      return popup;
    }
    String[] with = new String[popup.length + 1];
    System.arraycopy(popup, 0, with, 0, popup.length);
    with[popup.length] = COPY_OPTION;
    return with;
  }

  private String[] shiftedPopup(String[] popup) {
    if (mShift == SHIFT_OFF) {
      return popup;
    }
    String[] out = new String[popup.length];
    for (int i = 0; i < popup.length; i++) {
      out[i] = KeyCase.apply(popup[i], true);
    }
    return out;
  }

  private int popupIndex(float x) {
    if (mPopupChars == null || mDownKey == null) {
      return 0;
    }
    float cell = dp(KeyPopupOverlay.OPTION_CELL_DP);
    float total = mPopupChars.length * cell;
    float left =
        KeyPopupOverlay.optionsLeft(
            mDownKey.x + mDownKey.w / 2f, total, getWidth(), dp(KeyPopupOverlay.EDGE_DP));
    int idx = (int) ((x - left) / cell);
    return Math.max(0, Math.min(mPopupChars.length - 1, idx));
  }

  private float dp(float v) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
  }
}
