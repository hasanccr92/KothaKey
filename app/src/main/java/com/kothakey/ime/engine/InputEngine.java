package com.kothakey.ime.engine;

import android.view.inputmethod.InputConnection;
import java.util.List;

public interface InputEngine {
  void onChar(InputConnection ic, int codePoint);

  void onDelete(InputConnection ic);

  void commit(InputConnection ic);

  void reset(InputConnection ic);

  String composing();

  List<String> suggestions(int limit);

  boolean isWordChar(int codePoint);
}
