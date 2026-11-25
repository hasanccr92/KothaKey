package com.kothakey.ime.keyboard;

/**
 * Decides when the punctuation panel hands back to the letters panel.
 *
 * <p>Typing a symbol and then pressing space or return returns to the letters. A double tap on the
 * mode key locks the panel, so it never returns on its own. The policy holds no Android state so
 * it can be unit tested; the caller passes the time and applies the results.
 */
public final class PunctuationPanelPolicy {

  /** What the mode key should do to the panel. */
  public enum ModeAction {
    SHOW_PUNCTUATION,
    SHOW_LETTERS,
    LOCK_PUNCTUATION
  }

  /** Two taps on the mode key within this window lock the panel. */
  public static final long DOUBLE_TAP_MS = 300L;

  private boolean mActive;
  private boolean mLocked;
  private boolean mTypedSymbol;
  private long mEnteredAt;

  /**
   * The mode key was tapped.
   *
   * @param onLetters whether the letters panel is showing right now
   * @param nowMs a monotonic clock reading in milliseconds
   */
  public ModeAction onModeKey(boolean onLetters, long nowMs) {
    if (onLetters) {
      reset();
      mActive = true;
      mEnteredAt = nowMs;
      return ModeAction.SHOW_PUNCTUATION;
    }
    boolean secondTap =
        mActive && !mLocked && !mTypedSymbol && nowMs - mEnteredAt <= DOUBLE_TAP_MS;
    if (secondTap) {
      mLocked = true;
      return ModeAction.LOCK_PUNCTUATION;
    }
    reset();
    return ModeAction.SHOW_LETTERS;
  }

  /** A character was typed. Only characters typed from the punctuation panel count. */
  public void onChar() {
    if (!mActive) {
      return;
    }
    mTypedSymbol = true;
  }

  /**
   * Space or return was pressed (the space or newline is committed either way).
   *
   * @return true when the panel should switch back to the letters now
   */
  public boolean onSpaceOrEnter() {
    if (!returnsOnSpace()) {
      return false;
    }
    reset();
    return true;
  }

  /** True when a space or return now would switch back to the letters. */
  public boolean returnsOnSpace() {
    return mActive && !mLocked && mTypedSymbol;
  }

  public boolean locked() {
    return mLocked;
  }

  /** Forget everything; used when the panel is left by any other route. */
  public void reset() {
    mActive = false;
    mLocked = false;
    mTypedSymbol = false;
    mEnteredAt = 0;
  }
}
