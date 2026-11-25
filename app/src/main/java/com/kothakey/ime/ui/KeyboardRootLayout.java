package com.kothakey.ime.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Root of the input view: the keyboard content with a {@link KeyPopupOverlay} laid over it. The
 * overlay is sized to the content, so it never makes the keyboard window taller.
 */
public class KeyboardRootLayout extends FrameLayout {

  public KeyboardRootLayout(Context context) {
    super(context);
  }

  public KeyboardRootLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    View content = getChildAt(0);
    View overlay = getChildAt(1);
    measureChild(content, widthMeasureSpec, heightMeasureSpec);
    int width = resolveSize(content.getMeasuredWidth(), widthMeasureSpec);
    int height = content.getMeasuredHeight();
    setMeasuredDimension(width, height);
    overlay.measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
  }
}
