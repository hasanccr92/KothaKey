package com.kothakey.ime;

import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import com.kothakey.R;
import com.kothakey.ime.dict.BengaliLexicon;
import com.kothakey.ime.dict.KeyProximity;
import com.kothakey.ime.dict.LearnedWordsStore;
import com.kothakey.ime.dict.NextWordModel;
import com.kothakey.ime.dict.WordDictionary;
import com.kothakey.ime.clipboard.ClipboardPanelView;
import com.kothakey.ime.clipboard.ClipboardStore;
import com.kothakey.ime.emoji.EmojiKeywordIndex;
import com.kothakey.ime.emoji.EmojiPaletteView;
import com.kothakey.ime.engine.AutoSpacing;
import com.kothakey.ime.engine.AvroEngine;
import com.kothakey.ime.engine.CapsPolicy;
import com.kothakey.ime.engine.InputEngine;
import com.kothakey.ime.engine.LatinEngine;
import com.kothakey.ime.keyboard.KothaKeyKeyboardView;
import com.kothakey.ime.keyboard.Language;
import com.kothakey.ime.keyboard.Layouts;
import com.kothakey.ime.keyboard.PunctuationPanelPolicy;
import com.kothakey.ime.ui.KeyPopupOverlay;
import com.kothakey.ime.ui.KothaKeyTheme;
import com.kothakey.ime.ui.SuggestionStrip;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KothaKeyImeService extends InputMethodService
    implements KothaKeyKeyboardView.Callback,
        SuggestionStrip.Listener,
        EmojiPaletteView.Callback,
        ClipboardPanelView.Callback {

  private KothaKeyPrefs mPrefs;
  private WordDictionary mFr;
  private WordDictionary mEn;
  private BengaliLexicon mBn;
  private int mLearnedEpoch;
  private final android.os.Handler mSaveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
  private final Runnable mSaveLearned = this::saveLearnedNow;
  private EmojiKeywordIndex mEmojiKw;
  private LatinEngine mLatin;
  private AvroEngine mAvro;
  private InputEngine mEngine;

  private SuggestionStrip mStrip;
  private KothaKeyKeyboardView mKeyboard;
  private KeyPopupOverlay mPopupOverlay;
  private EmojiPaletteView mEmoji;
  private ClipboardPanelView mClipboardPanel;
  private ClipboardStore mClipboard;
  private View mRoot;

  /** Latest copy currently offered as a chip in the strip, or null. */
  private String mClipOffered;
  /** Last copy the user pasted or ignored by typing; it is not offered again. */
  private String mClipConsumed;

  private static final long TRIPLE_TAP_MS = 1000;
  private long mTripleSetAt;
  private final android.os.Handler mMain = new android.os.Handler(android.os.Looper.getMainLooper());
  private long mWordSelAt;
  private int mWordSelStart;
  private int mWordSelEnd;
  private int[] mTripleTarget;

  private final PunctuationPanelPolicy mPunctuation = new PunctuationPanelPolicy();

  /** Whether the current field is ordinary text, so auto-spacing and capitalising apply. */
  private boolean mTextField = true;
  /** The character before the cursor is a space the keyboard added after punctuation. */
  private boolean mAutoSpaced;
  /** When the last plain space was typed, for the double-space period. */
  private long mLastSpaceAt;
  /** A suggestion pick or an automatic space must not become a period on the next tap. */
  private boolean mBlockDoubleSpace;
  /** The user chose shift by hand. Auto-caps leaves it until the next character or cursor move. */
  private boolean mManualShift;
  private int mSel = -1;
  /** A short copy of the text before the cursor, so each key does not ask the field again. */
  private final StringBuilder mBefore = new StringBuilder();
  private NextWordModel mNextLatin;
  private NextWordModel mNextBn;
  private int mSuggestGen;
  private final ExecutorService mSuggest =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "kothakey-suggest");
            thread.setDaemon(true);
            return thread;
          });

  private Language mLanguage = Language.BN;
  private Vibrator mVibrator;
  private AudioManager mAudio;

  @Override
  public void onCreate() {
    super.onCreate();
    mPrefs = new KothaKeyPrefs(this);
    mFr = new WordDictionary();
    mEn = new WordDictionary();
    mBn = new BengaliLexicon();
    mEmojiKw = new EmojiKeywordIndex();
    mFr.loadAsync(this, "dict/fr.tsv");
    mEn.loadAsync(this, "dict/en.tsv");
    mBn.loadAsync(this, "bengali_words.txt", "bengali_extra.txt");
    mLearnedEpoch = mPrefs.learnedEpoch();
    mBn.setLearned(LearnedWordsStore.load(LearnedWordsStore.file(this)));
    mNextLatin = new NextWordModel();
    mNextBn = new NextWordModel();
    mNextLatin.load(LearnedWordsStore.named(this, "next_latin.txt"));
    mNextBn.load(LearnedWordsStore.named(this, "next_bn.txt"));
    loadSeeds(mNextLatin, "next_seed_en.tsv");
    loadSeeds(mNextLatin, "next_seed_fr.tsv");
    loadSeeds(mNextBn, "next_seed_bn.tsv");
    mBn.setLearnedListener(
        () -> {
          mSaveHandler.removeCallbacks(mSaveLearned);
          mSaveHandler.postDelayed(mSaveLearned, 5000);
        });
    mEmojiKw.loadAsync(this);
    mLatin = new LatinEngine(mFr, mEn, mEmojiKw);
    mLatin.setLearned(LearnedWordsStore.load(LearnedWordsStore.named(this, "latin_learned.txt")));
    mAvro = AvroEngine.createDefault(mBn);
    mLanguage = mPrefs.language();
    mEngine = mLanguage == Language.FR ? mLatin : mAvro;
    mClipboard = new ClipboardStore(this);
    mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    if (Build.VERSION.SDK_INT >= 31) {
      VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
      mVibrator = vm != null ? vm.getDefaultVibrator() : null;
    } else {
      mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }
  }

  @Override
  public View onCreateInputView() {
    mRoot = getLayoutInflater().inflate(R.layout.input, null);
    mStrip = mRoot.findViewById(R.id.suggestions);
    mKeyboard = mRoot.findViewById(R.id.keyboard);
    mEmoji = mRoot.findViewById(R.id.emoji);
    mClipboardPanel = mRoot.findViewById(R.id.clipboard);
    mPopupOverlay = mRoot.findViewById(R.id.popup_overlay);
    mKeyboard.setPopupOverlay(mPopupOverlay);
    mStrip.setListener(this);
    mKeyboard.setCallback(this);
    mEmoji.setCallback(this);
    mClipboardPanel.setCallback(this);
    mClipboardPanel.bind(mClipboard);
    mClipboard.setListener(() -> {
      if (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE) {
        mClipboardPanel.reload();
      }
      maybeOfferClip();
      refreshSuggestions();
    });
    applyChrome();
    return mRoot;
  }

  private void saveLearnedNow() {
    final java.util.Map<String, Integer> bengali = mBn.learnedSnapshot();
    final java.util.Map<String, Integer> latin = mLatin.learnedSnapshot();
    final java.io.File bengaliFile = LearnedWordsStore.file(this);
    final java.io.File latinFile = LearnedWordsStore.named(this, "latin_learned.txt");
    final java.io.File nextLatin = LearnedWordsStore.named(this, "next_latin.txt");
    final java.io.File nextBn = LearnedWordsStore.named(this, "next_bn.txt");
    new Thread(
            () -> {
              LearnedWordsStore.save(bengaliFile, bengali);
              LearnedWordsStore.save(latinFile, latin);
              mNextLatin.save(nextLatin);
              mNextBn.save(nextBn);
            },
            "kothakey-learned-save")
        .start();
  }

  /** Forgets learned words in memory when Settings has cleared them. */
  private void syncLearnedWords() {
    int epoch = mPrefs.learnedEpoch();
    if (epoch != mLearnedEpoch) {
      mLearnedEpoch = epoch;
      mSaveHandler.removeCallbacks(mSaveLearned);
      mBn.clearLearned();
      mLatin.clearLearned();
      mNextLatin.clear();
      mNextBn.clear();
      loadSeeds(mNextLatin, "next_seed_en.tsv");
      loadSeeds(mNextLatin, "next_seed_fr.tsv");
      loadSeeds(mNextBn, "next_seed_bn.tsv");
    }
  }

  @Override
  public void onDestroy() {
    if (mBn != null) {
      syncLearnedWords();
      mSaveHandler.removeCallbacks(mSaveLearned);
      LearnedWordsStore.save(LearnedWordsStore.file(this), mBn.learnedSnapshot());
    }
    if (mClipboard != null) {
      mClipboard.close();
    }
    mSuggest.shutdownNow();
    super.onDestroy();
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    if (mEngine != null) {
      mEngine.reset(getCurrentInputConnection());
    }
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    syncLearnedWords();
    mTextField = isPlainTextField(info);
    mAutoSpaced = false;
    mBlockDoubleSpace = false;
    mLastSpaceAt = 0;
    mManualShift = false;
    applyChrome();
    showKeyboard();
    maybeOfferClip();
    pullBefore(getCurrentInputConnection());
    refreshSuggestions();
    updateAutoShift();
  }

  /** False for passwords, addresses, URLs, numbers and other fields where auto-spacing hurts. */
  private static boolean isPlainTextField(EditorInfo info) {
    if (info == null) {
      return true;
    }
    int type = info.inputType;
    if ((type & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) {
      return (type & InputType.TYPE_MASK_CLASS) == 0;
    }
    switch (type & InputType.TYPE_MASK_VARIATION) {
      case InputType.TYPE_TEXT_VARIATION_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
      case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
      case InputType.TYPE_TEXT_VARIATION_URI:
      case InputType.TYPE_TEXT_VARIATION_FILTER:
        return false;
      default:
        return true;
    }
  }

  @Override
  public void onFinishInput() {
    if (mEngine != null) {
      mEngine.reset(getCurrentInputConnection());
    }
    super.onFinishInput();
  }

  private void applyChrome() {
    if (mKeyboard == null) {
      return;
    }
    KothaKeyTheme theme = mPrefs.theme();
    mLanguage = mPrefs.language();
    mEngine = mLanguage == Language.FR ? mLatin : mAvro;
    mStrip.setTheme(theme);
    mKeyboard.setTheme(theme);
    mPopupOverlay.setTheme(theme);
    android.graphics.Typeface font = com.kothakey.ime.ui.KeyType.typeface(this, mPrefs.keyFont());
    mKeyboard.setKeyFont(font);
    mPopupOverlay.setKeyFont(font);
    mStrip.setKeyFont(font);
    mKeyboard.setNumberRow(mPrefs.numberRow());
    mKeyboard.setArrangement(mPrefs.arrangement());
    mKeyboard.setLanguage(mLanguage);
    mPunctuation.reset();
    mKeyboard.setPage(Layouts.Page.LETTERS);
    mEmoji.setTheme(theme);
    if (mClipboardPanel != null) {
      mClipboardPanel.setTheme(theme);
    }
    if (mRoot != null) {
      mRoot.setBackgroundColor(theme.background);
    }
  }

  private void showKeyboard() {
    if (mKeyboard == null) {
      return;
    }
    mKeyboard.setVisibility(View.VISIBLE);
    mEmoji.setVisibility(View.GONE);
    if (mClipboardPanel != null) {
      mClipboardPanel.setVisibility(View.GONE);
    }
  }

  private void showEmoji() {
    sizeOverlay(mEmoji);
    mKeyboard.setVisibility(View.GONE);
    mEmoji.setVisibility(View.VISIBLE);
    if (mClipboardPanel != null) {
      mClipboardPanel.setVisibility(View.GONE);
    }
    mEmoji.showRecent();
  }

  private void showClipboard() {
    sizeOverlay(mClipboardPanel);
    mKeyboard.setVisibility(View.GONE);
    mEmoji.setVisibility(View.GONE);
    mClipboardPanel.setVisibility(View.VISIBLE);
    mClipboardPanel.reload();
  }

  private void sizeOverlay(View overlay) {
    if (overlay != null && overlay.getLayoutParams() != null && mKeyboard.getHeight() > 0) {
      overlay.getLayoutParams().height = mKeyboard.getHeight();
      overlay.requestLayout();
    }
  }

  @Override
  public void onChar(int codePoint) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    dismissClip();
    mManualShift = false;
    mBlockDoubleSpace = false;
    mPunctuation.onChar();
    int previous = cachedLast();
    boolean spaced = false;
    if (autoPunctuationOn() && AutoSpacing.triggersSpace(codePoint)) {
      // A second mark right after an automatic space ("wait." then ".") takes the space back.
      if (mAutoSpaced && previous == ' ') {
        ic.deleteSurroundingText(1, 0);
        if (mBefore.length() > 0) {
          mBefore.deleteCharAt(mBefore.length() - 1);
        }
        previous = cachedLast();
      }
      spaced = !AutoSpacing.continuesNumber(codePoint, previous);
    }
    mEngine.onChar(ic, codePoint);
    if (spaced) {
      mEngine.commit(ic);
      ic.commitText(" ", 1);
    }
    mAutoSpaced = spaced;
    mBlockDoubleSpace = spaced;
    mLastSpaceAt = 0;
    pullBefore(ic);
    if (spaced) {
      learnContext(1);
    }
    refreshSuggestions();
    updateAutoShift();
  }

  /** Auto-spacing and auto-capitalisation apply to ordinary text fields only. */
  private boolean autoPunctuationOn() {
    return mTextField && mPrefs.autoPunctuation();
  }

  @Override
  public void onDelete() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    dismissClip();
    mManualShift = false;
    mAutoSpaced = false;
    mBlockDoubleSpace = false;
    mLastSpaceAt = 0;
    CharSequence selected = ic.getSelectedText(0);
    if (selected != null && selected.length() > 0) {
      mEngine.reset(ic);
      ic.commitText("", 1);
    } else {
      mEngine.onDelete(ic);
    }
    pullBefore(ic);
    refreshSuggestions();
    updateAutoShift();
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
      int candidatesStart, int candidatesEnd) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
        candidatesStart, candidatesEnd);
    detectTripleTap(oldSelStart, oldSelEnd, newSelStart, newSelEnd);
    boolean cursorMoved = newSelStart != mSel;
    if (cursorMoved) {
      mManualShift = false;
    }
    mSel = newSelStart;
    pullBefore(getCurrentInputConnection());
    updateAutoShift();
    if (cursorMoved && mEngine != null && mEngine.composing().isEmpty()) {
      refreshSuggestions();
    }
    if (mEngine == null || mEngine.composing().isEmpty()) {
      return;
    }
    if (newSelStart != candidatesEnd || newSelEnd != candidatesEnd) {
      mEngine.reset(getCurrentInputConnection());
      refreshSuggestions();
    }
  }

  /**
   * The text field handles taps, not the keyboard, so a triple tap is recognised from the
   * selection changes it causes: tap 2 selects a word, tap 3 collapses the cursor back inside it.
   * That pattern within a short window selects the whole line (up to the surrounding returns).
   */
  private void detectTripleTap(int oldStart, int oldEnd, int newStart, int newEnd) {
    long now = SystemClock.uptimeMillis();
    if (mTripleTarget != null) {
      boolean ours = newStart == mTripleTarget[0] && newEnd == mTripleTarget[1];
      boolean stale = now - mTripleSetAt <= 400;
      if (ours) {
        mTripleTarget = null;
        mWordSelAt = 0;
        return;
      }
      if (stale) {
        // Late reports of the tap itself can arrive after our selection was requested.
        return;
      }
      mTripleTarget = null;
      mWordSelAt = 0;
    }
    boolean collapsedNow = newStart == newEnd;
    if (!collapsedNow) {
      boolean wasCollapsed = oldStart == oldEnd;
      if (wasCollapsed) {
        mWordSelAt = now;
        mWordSelStart = newStart;
        mWordSelEnd = newEnd;
      } else {
        mWordSelAt = 0;
      }
      return;
    }
    boolean thirdTap =
        mWordSelAt != 0
            && now - mWordSelAt <= TRIPLE_TAP_MS
            && newStart >= mWordSelStart
            && newStart <= mWordSelEnd;
    mWordSelAt = 0;
    if (thirdTap) {
      // Let the field finish settling its own tap handling before reading it back.
      mMain.postDelayed(() -> selectParagraphAround(newStart), 80);
    }
  }

  private void selectParagraphAround(int cursor) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    // Ask the field for its text and its real, current selection; the tap's own reports can be
    // a moment behind.
    ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
    if (et == null || et.text == null) {
      return;
    }
    CharSequence text = et.text;
    int offset = et.startOffset;
    int at = Math.max(0, Math.min(text.length(), et.selectionStart));
    if (et.selectionStart != et.selectionEnd) {
      return;
    }
    int from = at;
    while (from > 0 && text.charAt(from - 1) != '\n') {
      from--;
    }
    int to = at;
    while (to < text.length() && text.charAt(to) != '\n') {
      to++;
    }
    int start = offset + from;
    int end = offset + to;
    if (start == end) {
      return;
    }
    mTripleTarget = new int[] {start, end};
    mTripleSetAt = SystemClock.uptimeMillis();
    ic.setSelection(start, end);
  }

  @Override
  public void onSpace() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    dismissClip();
    mManualShift = false;
    long now = SystemClock.uptimeMillis();
    boolean composing = !mEngine.composing().isEmpty();
    if (mAutoSpaced && !composing && cachedLast() == ' ') {
      // The space after the punctuation is already there; do not type a second one.
      returnFromPunctuation();
      updateAutoShift();
      return;
    }
    if (!composing
        && !mBlockDoubleSpace
        && mTextField
        && mPrefs.doubleSpacePeriod()
        && mLastSpaceAt != 0
        && now - mLastSpaceAt <= AutoSpacing.DOUBLE_SPACE_MS
        && AutoSpacing.isDoubleSpacePeriod(tail(2))) {
      ic.deleteSurroundingText(1, 0);
      ic.commitText(mLanguage == Language.BN ? "\u0964 " : ". ", 1);
      mLastSpaceAt = 0;
      mAutoSpaced = true;
      mBlockDoubleSpace = true;
      pullBefore(ic);
      returnFromPunctuation();
      refreshSuggestions();
      updateAutoShift();
      return;
    }
    String typed = mEngine.composing();
    mEngine.commit(ic);
    ic.commitText(" ", 1);
    mAutoSpaced = false;
    mBlockDoubleSpace = false;
    mLastSpaceAt = now;
    pullBefore(ic);
    if (!typed.isEmpty()) {
      learnContext(1);
    }
    returnFromPunctuation();
    refreshSuggestions();
    updateAutoShift();
  }

  @Override
  public void onEnter() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    dismissClip();
    mManualShift = false;
    mAutoSpaced = false;
    mBlockDoubleSpace = false;
    mLastSpaceAt = 0;
    String typed = mEngine.composing();
    mEngine.commit(ic);
    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
    pullBefore(ic);
    if (mBefore.length() == 0 || mBefore.charAt(mBefore.length() - 1) != '\n') {
      mBefore.append('\n');
    }
    if (!typed.isEmpty()) {
      learnContext(1);
    }
    returnFromPunctuation();
    refreshSuggestions();
    updateAutoShift();
  }

  @Override
  public void onSymbols() {
    boolean onLetters = mKeyboard.page() == Layouts.Page.LETTERS;
    switch (mPunctuation.onModeKey(onLetters, SystemClock.uptimeMillis())) {
      case SHOW_PUNCTUATION:
        mKeyboard.setPage(Layouts.Page.SYMBOLS1);
        break;
      case SHOW_LETTERS:
        mKeyboard.setPage(Layouts.Page.LETTERS);
        break;
      case LOCK_PUNCTUATION:
        // Double tap: stay on the punctuation panel until the mode key is tapped again.
        break;
    }
  }

  /** Returns to the letters after a symbol was typed and space or return was pressed. */
  private void returnFromPunctuation() {
    if (mPunctuation.onSpaceOrEnter()) {
      mKeyboard.setPage(Layouts.Page.LETTERS);
    }
  }

  @Override
  public void onPage(Layouts.Page page) {
    mKeyboard.setPage(page);
  }

  @Override
  public void onEmoji() {
    showEmoji();
  }

  @Override
  public void onLanguageSwitch() {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
      mEngine.commit(ic);
    }
    mLanguage = mLanguage.other();
    mPrefs.setLanguage(mLanguage);
    mEngine = mLanguage == Language.FR ? mLatin : mAvro;
    mKeyboard.setLanguage(mLanguage);
    mPunctuation.reset();
    mKeyboard.setPage(Layouts.Page.LETTERS);
    mKeyboard.setShift(KothaKeyKeyboardView.SHIFT_OFF);
    mManualShift = false;
    pullBefore(ic);
    refreshSuggestions();
    updateAutoShift();
  }

  @Override
  public void onPick(String word) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    dismissClip();
    feedback();
    mManualShift = false;
    mBlockDoubleSpace = true;
    mAutoSpaced = false;
    mLastSpaceAt = 0;
    if (mLanguage == Language.FR) {
      mLatin.pick(ic, word);
    } else {
      mAvro.pick(ic, word);
    }
    pullBefore(ic);
    learnContext(2);
    refreshSuggestions();
    updateAutoShift();
  }

  @Override
  public void onPress() {
    feedback();
  }

  @Override
  public void onManualShift() {
    mManualShift = true;
  }

  @Override
  public void onClipboard() {
    feedback();
    showClipboard();
  }

  @Override
  public void onCopy() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    String text = textToCopy(ic);
    if (text.isEmpty()) {
      ic.performContextMenuAction(android.R.id.copy);
      text = mClipboard.systemText();
    }
    if (!text.isEmpty()) {
      // The user just copied this themselves, so do not offer it back as a chip.
      mClipConsumed = text;
      mClipOffered = null;
      mClipboard.push(text);
      feedback();
    }
  }

  @Override
  public void onPaste() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    String text = mClipboard.latest();
    if (text.isEmpty()) {
      text = mClipboard.systemText();
    }
    if (text.isEmpty()) {
      ic.performContextMenuAction(android.R.id.paste);
      return;
    }
    dismissClip();
    feedback();
    mEngine.commit(ic);
    ic.commitText(text, 1);
    refreshSuggestions();
  }

  @Override
  public void onPasteItem(String text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null || text == null || text.isEmpty()) {
      return;
    }
    // Pasting from history also settles the chip, even if a different clip was offered.
    mClipConsumed = text;
    mClipOffered = null;
    feedback();
    mEngine.commit(ic);
    ic.commitText(text, 1);
    mClipboard.push(text);
    showKeyboard();
    refreshSuggestions();
  }

  @Override
  public void onClipPick(String text) {
    onPasteItem(text);
  }

  @Override
  public void onSettings() {
    Intent intent = new Intent(this, SettingsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @Override
  public void onEmoji(String emoji) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
      return;
    }
    dismissClip();
    feedback();
    mEngine.commit(ic);
    ic.commitText(emoji, 1);
    refreshSuggestions();
  }

  @Override
  public void onAbc() {
    showKeyboard();
  }

  private String textToCopy(InputConnection ic) {
    CharSequence selected = ic.getSelectedText(0);
    if (selected != null && selected.length() > 0) {
      return selected.toString();
    }
    String composing = mEngine.composing();
    if (!composing.isEmpty()) {
      return composing;
    }
    CharSequence before = ic.getTextBeforeCursor(256, 0);
    CharSequence after = ic.getTextAfterCursor(256, 0);
    String left = before == null ? "" : before.toString();
    String right = after == null ? "" : after.toString();
    int start = left.length();
    while (start > 0 && !Character.isWhitespace(left.charAt(start - 1))) {
      start--;
    }
    int end = 0;
    while (end < right.length() && !Character.isWhitespace(right.charAt(end))) {
      end++;
    }
    return (left.substring(start) + right.substring(0, end)).trim();
  }

  private void refreshSuggestions() {
    if (mStrip == null || mEngine == null) {
      return;
    }
    final int generation = ++mSuggestGen;
    final String composing = mEngine.composing();
    final String before = mBefore.toString();
    final Language language = mLanguage;
    final KeyProximity proximity = KeyProximity.of(mPrefs.arrangement());
    if (composing.isEmpty() && mClipOffered != null) {
      mStrip.showClip(mClipOffered);
      return;
    }
    mSuggest.execute(
        () -> {
          List<String> words = buildSuggestions(language, composing, before, proximity);
          mMain.post(
              () -> {
                if (generation != mSuggestGen || mStrip == null) {
                  return;
                }
                mStrip.show(words);
              });
        });
  }

  /** Completions, typo fixes and the word that usually comes next. Runs off the UI thread. */
  private List<String> buildSuggestions(
      Language language, String composing, String before, KeyProximity proximity) {
    String context = before;
    if (!composing.isEmpty() && before.endsWith(composing)) {
      context = before.substring(0, before.length() - composing.length());
    }
    List<String> previous = NextWordModel.wordsBefore(context, 2);
    String second = previous.isEmpty() ? "" : previous.get(previous.size() - 1);
    String first = previous.size() >= 2 ? previous.get(previous.size() - 2) : "";
    boolean sentence = CapsPolicy.shouldCapitalize(context);
    if (language == Language.BN) {
      List<String> predicted = mNextBn.predict(first, second, 6);
      if (composing.isEmpty()) {
        return predicted;
      }
      List<String> out = new ArrayList<>();
      for (String word : predicted) {
        if (word.startsWith(composing) && !out.contains(word)) {
          out.add(word);
        }
      }
      for (String word : mBn.suggest(composing, 8)) {
        if (out.size() >= 8) {
          break;
        }
        if (!out.contains(word)) {
          out.add(word);
        }
      }
      return out;
    }
    if (composing.isEmpty()) {
      List<String> predicted = mNextLatin.predict(first, second, 8);
      if (!sentence) {
        return predicted;
      }
      List<String> capped = new ArrayList<>();
      for (String word : predicted) {
        capped.add(capitalise(word));
      }
      return capped;
    }
    return mLatin.suggestFor(composing, first, second, proximity, mNextLatin, 8);
  }

  private static String capitalise(String word) {
    if (word == null || word.isEmpty()) {
      return word;
    }
    int cp = word.codePointAt(0);
    if (!Character.isLetter(cp) || Character.isUpperCase(cp)) {
      return word;
    }
    return new String(Character.toChars(Character.toUpperCase(cp)))
        + word.substring(Character.charCount(cp));
  }

  /** Records the word that was just finished, and the word before it. */
  private void learnContext(int latinWeight) {
    List<String> words = NextWordModel.wordsBefore(mBefore, 3);
    if (words.isEmpty()) {
      return;
    }
    String next = words.get(words.size() - 1);
    if (mLanguage == Language.FR && latinWeight > 0) {
      mLatin.learn(next, latinWeight);
    }
    if (words.size() < 2) {
      return;
    }
    String second = words.get(words.size() - 2);
    String first = words.size() >= 3 ? words.get(words.size() - 3) : "";
    NextWordModel model = mLanguage == Language.FR ? mNextLatin : mNextBn;
    model.learn(first, second, next);
    mSaveHandler.removeCallbacks(mSaveLearned);
    mSaveHandler.postDelayed(mSaveLearned, 5000);
  }

  private void pullBefore(InputConnection ic) {
    if (ic == null) {
      return;
    }
    CharSequence text = ic.getTextBeforeCursor(160, 0);
    mBefore.setLength(0);
    if (text != null) {
      mBefore.append(text);
    }
  }

  private int cachedLast() {
    if (mBefore.length() == 0) {
      return 0;
    }
    return Character.codePointBefore(mBefore, mBefore.length());
  }

  private CharSequence tail(int n) {
    if (mBefore.length() <= n) {
      return mBefore;
    }
    return mBefore.subSequence(mBefore.length() - n, mBefore.length());
  }

  private void loadSeeds(NextWordModel model, String asset) {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(getAssets().open(asset), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty() || line.charAt(0) == '#') {
          continue;
        }
        int tab = line.indexOf('\t');
        if (tab <= 0) {
          continue;
        }
        model.seed(line.substring(0, tab).trim(), line.substring(tab + 1).trim());
      }
    } catch (java.io.IOException ignored) {
    }
  }

  /** Offers the latest copy in the strip unless it was already pasted or ignored. */
  private void maybeOfferClip() {
    String latest = mClipboard == null ? "" : mClipboard.latest();
    if (!latest.isEmpty() && !latest.equals(mClipConsumed)) {
      mClipOffered = latest;
    } else {
      mClipOffered = null;
    }
  }

  /** Typing or pasting retires the offered copy so it does not come back. */
  private void dismissClip() {
    if (mClipOffered != null) {
      mClipConsumed = mClipOffered;
      mClipOffered = null;
    }
  }

  private void updateAutoShift() {
    if (mKeyboard == null || mLanguage != Language.FR || !autoPunctuationOn()) {
      return;
    }
    if (mKeyboard.shift() == KothaKeyKeyboardView.SHIFT_CAPS || mManualShift) {
      return;
    }
    int want =
        CapsPolicy.shouldCapitalize(mBefore)
            ? KothaKeyKeyboardView.SHIFT_ONESHOT
            : KothaKeyKeyboardView.SHIFT_OFF;
    if (mKeyboard.shift() != want) {
      mKeyboard.setShift(want);
    }
  }

  private void feedback() {
    if (mPrefs.sound() && mAudio != null) {
      mAudio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
    }
    if (mPrefs.vibrate() && mVibrator != null && mVibrator.hasVibrator()) {
      if (Build.VERSION.SDK_INT >= 26) {
        mVibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
      } else {
        mVibrator.vibrate(12);
      }
    }
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return false;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && isInputViewShown()) {
      if ((mEmoji != null && mEmoji.getVisibility() == View.VISIBLE)
          || (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE)) {
        showKeyboard();
        return true;
      }
      requestHideSelf(0);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }
}
