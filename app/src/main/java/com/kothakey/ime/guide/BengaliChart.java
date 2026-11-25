package com.kothakey.ime.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What to type for each Bengali letter on the phonetic keyboard. Every row is checked against the
 * real transliterator in {@code BengaliGuideTest}, so the guide cannot drift from the keyboard.
 */
public final class BengaliChart {

  public enum Kind {
    VOWELS(4),
    VOWEL_SIGNS(3),
    CONSONANTS(4),
    SPECIAL(1),
    CONJUNCTS(3),
    NUMBERS(5),
    EXAMPLES(1);

    /** How many entries share one row on screen. */
    public final int columns;

    Kind(int columns) {
      this.columns = columns;
    }
  }

  /** One row: type {@code latin} to get {@code bengali}. */
  public static final class Entry {
    public final String latin;
    public final String bengali;
    /** Short hint, or empty. */
    public final String note;

    Entry(String latin, String bengali, String note) {
      this.latin = latin;
      this.bengali = bengali;
      this.note = note;
    }
  }

  public static final class Section {
    public final Kind kind;
    public final List<Entry> entries;

    Section(Kind kind, List<Entry> entries) {
      this.kind = kind;
      this.entries = Collections.unmodifiableList(entries);
    }
  }

  private BengaliChart() {}

  private static final List<Section> SECTIONS = build();

  public static List<Section> sections() {
    return SECTIONS;
  }

  private static List<Section> build() {
    List<Section> out = new ArrayList<>();

    out.add(
        section(
            Kind.VOWELS,
            "o", "অ", "a", "আ", "i", "ই", "I", "ঈ", "u", "উ", "U", "ঊ", "rri", "ঋ", "e", "এ",
            "OI", "ঐ", "O", "ও", "OU", "ঔ"));

    out.add(
        section(
            Kind.VOWEL_SIGNS,
            "ka", "কা", "ki", "কি", "kI", "কী", "ku", "কু", "kU", "কূ", "krri", "কৃ", "ke", "কে",
            "kOI", "কৈ", "kO", "কো", "kOU", "কৌ"));

    out.add(
        section(
            Kind.CONSONANTS,
            "k", "ক", "kh", "খ", "g", "গ", "gh", "ঘ", "c", "চ", "ch", "ছ", "j", "জ", "jh", "ঝ",
            "T", "ট", "Th", "ঠ", "D", "ড", "Dh", "ঢ", "N", "ণ", "t", "ত", "th", "থ", "d", "দ",
            "dh", "ধ", "n", "ন", "p", "প", "ph", "ফ", "b", "ব", "bh", "ভ", "m", "ম", "z", "য",
            "r", "র", "l", "ল", "sh", "শ", "Sh", "ষ", "s", "স", "h", "হ"));

    List<Entry> special = new ArrayList<>();
    special.add(
        new Entry("t``", "ৎ", "khanda ta: type t then two backticks (`). Example: jogot`` → জগৎ"));
    special.add(new Entry("^", "ঁ", "chandrabindu, after the letter. Example: ca^d → চাঁদ"));
    special.add(new Entry("ng", "ং", "anusvara. Example: banglay → বাংলায়"));
    special.add(new Entry(":", "ঃ", "visarga. Example: du:kho → দুঃখ"));
    special.add(new Entry("Ng", "ঙ", "capital N, small g"));
    special.add(new Entry("NG", "ঞ", "capital N, capital G"));
    special.add(new Entry("R", "ড়", "capital R. Example: baRi → বাড়ি"));
    special.add(new Entry("Rh", "ঢ়", "capital R then h. Example: aShaRh → আষাঢ়"));
    special.add(new Entry("Y", "য়", "capital Y. Example: moYla → ময়লা"));
    special.add(new Entry("$", "৳", "taka sign"));
    special.add(new Entry(".", "।", "dari (full stop)"));
    out.add(new Section(Kind.SPECIAL, special));

    out.add(
        section(
            Kind.CONJUNCTS,
            "kk", "ক্ক", "kkh", "ক্ষ", "gg", "জ্ঞ", "jj", "জ্জ", "cch", "চ্ছ", "nt", "ন্ত",
            "ndr", "ন্দ্র", "str", "স্ত্র", "tr", "ত্র", "ShT", "ষ্ট", "ShN", "ষ্ণ", "bZ", "ব্য",
            "bd", "ব্দ", "mp", "ম্প"));

    out.add(
        section(
            Kind.NUMBERS,
            "0", "০", "1", "১", "2", "২", "3", "৩", "4", "৪", "5", "৫", "6", "৬", "7", "৭", "8",
            "৮", "9", "৯"));

    out.add(
        section(
            Kind.EXAMPLES,
            "u^cu", "উঁচু", "toRit``", "তড়িৎ", "bidyut", "বিদ্যুৎ", "ut``sob", "উৎসব", "ca^d",
            "চাঁদ", "du:kho", "দুঃখ", "baRi", "বাড়ি", "bZakul", "ব্যাকুল"));
    return out;
  }

  /** Builds a section from alternating latin / bengali values. */
  private static Section section(Kind kind, String... pairs) {
    List<Entry> entries = new ArrayList<>(pairs.length / 2);
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      entries.add(new Entry(pairs[i], pairs[i + 1], ""));
    }
    return new Section(kind, entries);
  }
}
