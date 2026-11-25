package com.kothakey.ime.engine;

import android.view.inputmethod.InputConnection;
import com.kothakey.avro.AvroPhoneticTransliterator;
import com.kothakey.avro.Transliterator;
import com.kothakey.ime.dict.BengaliLexicon;
import java.util.List;

/** Avro phonetic engine. Period stays in the Latin buffer so `.` becomes dari. */
public final class AvroEngine implements InputEngine {

  private final Transliterator mAvro;
  private final BengaliLexicon mDictionary;
  private final StringBuilder mLatin = new StringBuilder(64);

  public AvroEngine(Transliterator avro, BengaliLexicon dictionary) {
    mAvro = avro;
    mDictionary = dictionary;
  }

  public static AvroEngine createDefault(BengaliLexicon dictionary) {
    return new AvroEngine(AvroPhoneticTransliterator.createDefault(), dictionary);
  }

  @Override
  public void onChar(InputConnection ic, int codePoint) {
    if (isWordChar(codePoint)) {
      mLatin.appendCodePoint(codePoint);
      ic.setComposingText(mAvro.parse(mLatin), 1);
    } else {
      commit(ic);
      ic.commitText(new String(Character.toChars(codePoint)), 1);
    }
  }

  @Override
  public void onDelete(InputConnection ic) {
    if (mLatin.length() > 0) {
      mLatin.deleteCharAt(mLatin.length() - 1);
      if (mLatin.length() == 0) {
        ic.setComposingText("", 1);
      } else {
        ic.setComposingText(mAvro.parse(mLatin), 1);
      }
      return;
    }
    ic.deleteSurroundingText(1, 0);
  }

  @Override
  public void commit(InputConnection ic) {
    if (mLatin.length() == 0) {
      return;
    }
    String word = mAvro.parse(mLatin).toString();
    ic.commitText(word, 1);
    mLatin.setLength(0);
    mDictionary.learn(word, 1);
  }

  @Override
  public void reset(InputConnection ic) {
    if (ic != null && mLatin.length() > 0) {
      ic.finishComposingText();
    }
    mLatin.setLength(0);
  }

  @Override
  public String composing() {
    return mLatin.length() == 0 ? "" : mAvro.parse(mLatin).toString();
  }

  @Override
  public List<String> suggestions(int limit) {
    String typed = composing();
    return mDictionary.suggest(typed, limit);
  }

  @Override
  public boolean isWordChar(int codePoint) {
    return mAvro.isInputCharacter(codePoint) || codePoint == '.';
  }

  public void pick(InputConnection ic, String word) {
    ic.commitText(word + " ", 1);
    mLatin.setLength(0);
    mDictionary.learn(word, 2);
  }
}
