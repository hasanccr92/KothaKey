package com.kothakey.ime.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Draws the key bubbles above the keyboard, over the strip when a top-row key is pressed, the way
 * Gboard does. A gray bubble shows the pressed key; holding it turns the bubble green and lists the
 * alternatives. It never takes touches: the keyboard view underneath keeps them.
 */
public class KeyPopupOverlay extends View {

  private static final int NONE = 0;
  private static final int PREVIEW = 1;
  private static final int OPTIONS = 2;

  /** Width of one alternative in the green bubble. */
  public static final float OPTION_CELL_DP = 40f;
  /** Space kept free at the screen edges. */
  public static final float EDGE_DP = 8f;

  private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path mPath = new Path();
  private final Path mPart = new Path();
  private final RectF mRect = new RectF();
  private final RectF mKey = new RectF();

  private KothaKeyTheme mTheme = KothaKeyTheme.dark();
  private Typeface mFont = Typeface.DEFAULT;
  private int mMode = NONE;
  private String mLabel = "";
  private String[] mChars = new String[0];
  private int mSelected;

  public KeyPopupOverlay(Context context) {
    this(context, null);
  }

  public KeyPopupOverlay(Context context, AttributeSet attrs) {
    super(context, attrs);
    mText.setTextAlign(Paint.Align.CENTER);
    mStroke.setStyle(Paint.Style.STROKE);
    mStroke.setStrokeWidth(dp(1));
  }

  public void setTheme(KothaKeyTheme theme) {
    mTheme = theme;
    invalidate();
  }

  public void setKeyFont(Typeface face) {
    mFont = face == null ? Typeface.DEFAULT : face;
    invalidate();
  }

  /** Shows the gray bubble above a pressed key; coordinates are in this view's space. */
  public void showPreview(float x, float y, float w, float h, String label) {
    mKey.set(x, y, x + w, y + h);
    mLabel = label;
    mMode = PREVIEW;
    invalidate();
  }

  /** Turns the bubble green and lists the alternatives of the held key. */
  public void showOptions(float x, float y, float w, float h, String[] chars, int selected) {
    mKey.set(x, y, x + w, y + h);
    mChars = chars;
    mSelected = selected;
    mMode = OPTIONS;
    invalidate();
  }

  public void setSelected(int selected) {
    if (mMode == OPTIONS && mSelected != selected) {
      mSelected = selected;
      invalidate();
    }
  }

  public void hide() {
    if (mMode != NONE) {
      mMode = NONE;
      invalidate();
    }
  }

  /** Left edge of the green bubble; the keyboard uses it to map a finger to an alternative. */
  public static float optionsLeft(float keyCenterX, float total, float width, float edge) {
    return Math.max(edge, Math.min(keyCenterX - total / 2f, width - total - edge));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (mMode == PREVIEW) {
      drawPreview(canvas);
    } else if (mMode == OPTIONS) {
      drawOptions(canvas);
    }
  }

  private void drawPreview(Canvas canvas) {
    float neck = dp(6);
    float bodyW = Math.max(mKey.width() + dp(14), dp(54));
    // A top-row key has only the strip above it, so the bubble gets shorter there.
    float bodyH = Math.min(dp(54), mKey.top - dp(2) - neck);
    bodyH = Math.max(bodyH, dp(28));
    float left = clamp(mKey.centerX() - bodyW / 2f, dp(2), getWidth() - bodyW - dp(2));
    float bottom = mKey.top - neck;
    mRect.set(left, bottom - bodyH, left + bodyW, bottom);
    buildBubble(dp(10));

    int fill = mTheme.dark ? mTheme.keyPressed : mTheme.popupBg;
    drawBubble(canvas, fill);
    mText.setColor(mTheme.dark ? 0xFFFFFFFF : mTheme.popupLabel);
    mText.setTypeface(mFont);
    mText.setTextSize(dp(26));
    float cy = mRect.centerY() - (mText.descent() + mText.ascent()) / 2f;
    canvas.drawText(mLabel, mRect.centerX(), cy, mText);
  }

  private void drawOptions(Canvas canvas) {
    float neck = dp(6);
    float cell = dp(OPTION_CELL_DP);
    float total = mChars.length * cell;
    float left = optionsLeft(mKey.centerX(), total, getWidth(), dp(EDGE_DP));
    float pad = dp(4);
    float boxH = Math.min(dp(54), mKey.top - dp(1) - neck);
    boxH = Math.max(boxH, dp(30));
    float bottom = mKey.top - neck;
    mRect.set(left - pad, bottom - boxH, left + total + pad, bottom);
    buildBubble(dp(12));

    int green = mTheme.dark ? 0xFF3E9A7C : mTheme.enter;
    int selectedFill = mTheme.dark ? 0xFF1F5C48 : 0xFF00695C;
    drawBubble(canvas, green);

    mText.setTextSize(dp(20));
    mText.setTypeface(mFont);
    mText.setColor(0xFFFFFFFF);
    float top = mRect.top + pad;
    float cellH = boxH - pad * 2;
    for (int i = 0; i < mChars.length; i++) {
      float x = left + i * cell;
      if (i == mSelected) {
        mFill.setColor(selectedFill);
        canvas.drawRoundRect(x + dp(1), top, x + cell - dp(1), top + cellH, dp(8), dp(8), mFill);
      }
      float cy = top + cellH / 2f - (mText.descent() + mText.ascent()) / 2f;
      canvas.drawText(mChars[i], x + cell / 2f, cy, mText);
    }
  }

  /** Rounded body in mRect plus a short neck down to the pressed key, as one outline. */
  private void buildBubble(float radius) {
    mPath.reset();
    mPath.addRoundRect(mRect, radius, radius, Path.Direction.CW);
    mPart.reset();
    float inset = dp(1);
    mPart.addRect(
        mKey.left + inset, mRect.bottom - radius, mKey.right - inset, mKey.top + dp(2),
        Path.Direction.CW);
    mPath.op(mPart, Path.Op.UNION);
  }

  private void drawBubble(Canvas canvas, int fill) {
    mFill.setColor(fill);
    mFill.setStyle(Paint.Style.FILL);
    canvas.drawPath(mPath, mFill);
    mStroke.setColor(mTheme.dark ? 0x33FFFFFF : 0x33000000);
    canvas.drawPath(mPath, mStroke);
  }

  private static float clamp(float v, float lo, float hi) {
    return Math.max(lo, Math.min(v, Math.max(lo, hi)));
  }

  private float dp(float v) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
  }
}
