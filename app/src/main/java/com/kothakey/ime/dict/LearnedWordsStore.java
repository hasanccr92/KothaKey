package com.kothakey.ime.dict;

import android.content.Context;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Saves the Bengali words the user has typed, as {@code word<TAB>count} lines in app storage. */
public final class LearnedWordsStore {

  private static final String FILE_NAME = "bn_learned.txt";

  private LearnedWordsStore() {}

  public static File file(Context context) {
    return named(context, FILE_NAME);
  }

  public static File named(Context context, String name) {
    return new File(context.getFilesDir(), name);
  }

  public static Map<String, Integer> load(File file) {
    Map<String, Integer> out = new HashMap<>();
    if (!file.exists()) {
      return out;
    }
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        int tab = line.indexOf('\t');
        if (tab <= 0) {
          continue;
        }
        try {
          out.put(line.substring(0, tab), Integer.parseInt(line.substring(tab + 1).trim()));
        } catch (NumberFormatException ignored) {
        }
      }
    } catch (IOException ignored) {
    }
    return out;
  }

  public static void save(File file, Map<String, Integer> counts) {
    File tmp = new File(file.getPath() + ".tmp");
    try (BufferedWriter writer =
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))) {
      for (Map.Entry<String, Integer> e : counts.entrySet()) {
        writer.write(e.getKey());
        writer.write('\t');
        writer.write(Integer.toString(e.getValue()));
        writer.write('\n');
      }
    } catch (IOException e) {
      return;
    }
    if (!tmp.renameTo(file)) {
      file.delete();
      tmp.renameTo(file);
    }
  }

  public static void delete(File file) {
    file.delete();
  }
}
