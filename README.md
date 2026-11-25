# KothaKey (কথাকি)

Offline Android keyboard for French, English, and Bengali. Version **1.0.1** (`versionCode` 2). Package id `com.kothakey`.

French and English share one multilingual mode on AZERTY, with QWERTY available in settings. Bengali uses the Avro phonetic method: type `ami banglay gan gai` and the keyboard produces `আমি বাংলায় গান গাই`. Nothing is sent off the phone. The only permission is vibration.

Developed by S M Rakib Hasan · [GitHub](https://github.com/hasanccr92) · [Telegram](https://t.me/kothakey)

## Install

`KothaKey-debug.apk` in this repository is the 12 November 2026 debug build.

1. Install the APK, or run `install.bat` with a phone connected and `adb` on `PATH`.
2. Open **KothaKey**.
3. Tap **Enable KothaKey**, then **Switch to KothaKey**.
4. Slide the space bar to switch between `FR • EN` and `বাংলা`.

## Typing

- **Space slide** switches language.
- **?123 / ABC** opens the punctuation board. `!` and `?` sit next to backspace.
- **Shift tap** capitals the next letter. **Shift hold** turns on caps lock.
- **Long-press the top letter row** for digits, and for French accents in FR.
- **Suggestion strip** offers words. In Bengali it also offers the usual phonetic spelling when a candrabindu, khanda ta, or similar letter was missed (`উচু` offers `উঁচু`, `তড়িত` offers `তড়িৎ`). Picking a suggestion inserts it. Nothing is replaced on its own.
- **Clipboard** is the clipboard icon on the strip. History keeps the last 50 copies. Hold **C** for a second to copy, hold **V** for a second to paste. Tap a clip to paste it, long-press a clip to delete it.
- **Emoji** has its own panel. In FR, a keyword such as `coeur` can offer an emoji.
- A **double tap on space** can insert a period and a space. A period, question mark, or exclamation mark can add a space and capitalise the next sentence. Moving the cursor back into a sentence drops that capital.
- Bengali period follows Avro: `.` becomes `।`, `..` becomes `।।`, and `` .` `` becomes `.`.
- Settings also cover colorway, the Doto key font, an always-on number row, AZERTY or QWERTY, key sound, key vibration, the Bengali typing chart, and clearing learned words.
- Words you pick, or type twice, are remembered on the device.

## Rebuild

JDK 17, Android SDK 34, and Gradle 9.7.1. From this folder:

```bat
gradle :app:assembleDebug
```

Create `local.properties` with your SDK path:

```
sdk.dir=C:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

The Android SDK, emulator, and Gradle distribution used on the development machine are not in this repository.

`tools/build_wordlists.py` rebuilds `assets/dict/fr.tsv` and `en.tsv`. `tools/build_emoji.py` rebuilds the emoji tables. `tools/export_words.py` flattens the OpenBangla dictionary into `assets/bengali_words.txt`.

## Where each part comes from

The keyboard shell is KothaKey’s own code: the input-method service, the drawn keyboard, layouts, suggestion strip, clipboard, emoji panel, settings, and the Bengali chart. The pieces below were brought in from elsewhere.

| Piece | Taken from | Licence |
| --- | --- | --- |
| Avro phonetic engine, `com.kothakey.avro` | Port of [OmicronLab jsAvroPhonetic](https://github.com/omicronlab/jsAvroPhonetic) by Rifat Nabi. The Java port was written for AnySoftKeyboard (copyright 2026 AnySoftKeyboard contributors) and carried into this app. Pattern scan matches `avro-lib.js`, including the historical `isExact` off-by-one. | Original engine MPL-1.1. This Java port is Apache-2.0. |
| `avro_dict.bin` | Packed at build time from OmicronLab `avrodict.json` (the Avro phonetic pattern list). Loaded by `AvroDict`. | MPL-1.1 |
| `avro_autocorrect.bin` | Latin-to-Latin pairs from [Avro Keyboard](https://github.com/omicronlab/Avro-Keyboard) `autodict.dct` (for example `america` → `amerika` before parsing). | MPL-1.1 |
| `assets/bengali_words.txt` | Flattened from [OpenBangla Keyboard `dictionary.json`](https://github.com/OpenBangla/OpenBangla-Keyboard/blob/master/data/dictionary.json), about 159,000 Bengali words. OpenBangla stores them in phoneme buckets with no per-word counts. | GPL-3.0 |
| `assets/bengali_extra.txt` | Short extra list written for KothaKey, for letters that are easy to miss (`ৎ`, `ঁ`, `ং`, `ড়`, `য়`, and similar). | Project file |
| `assets/dict/fr.tsv`, `assets/dict/en.tsv` | Top 60,000 words from [hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords) 2018 (`fr_full.txt`, `en_full.txt`), which counts words in [OpenSubtitles](https://www.opensubtitles.org/). Built by `tools/build_wordlists.py`. | CC-BY-SA-4.0 |
| `assets/next_seed_fr.tsv`, `next_seed_en.tsv`, `next_seed_bn.tsv` | Small starter pairs for next-word suggestions, written for KothaKey. Further pairs are learned on the device. | Project files |
| `assets/emoji/emoji.tsv` | Fully-qualified emoji from [Unicode emoji-test 15.1](https://www.unicode.org/Public/emoji/15.1/emoji-test.txt), grouped into the palette categories. Skin-tone variants are attached to the base emoji. Built by `tools/build_emoji.py`. | Unicode Licence |
| `assets/emoji/keywords_en.tsv`, `keywords_fr.tsv` | Short names from Unicode CLDR 45 annotations and derived annotations, English and French: [unicode-org/cldr](https://github.com/unicode-org/cldr) `release-45`. | Unicode Licence |
| Colorways Taiyaki, Radar, Particle, Matte Orbit | Palettes described for [Orbit Board](https://github.com/fahitakrim/Orbit-Board) 1.3.9 by Fahim Takrim. Matte Orbit’s hex values come from that project’s `OrbitThemeHelper` (`#1C1D20`, `#EAE6E1`, `#E2B438`). Taiyaki, Radar, and Particle follow the published descriptions (toasted biscuit, sonar teal, a spark on obsidian). Dark and Light are KothaKey’s own. | Orbit Board is Apache-2.0 |
| Doto key font, `assets/fonts/doto.ttf` | [Doto](https://github.com/oliverlalan/Doto) by The Doto Project Authors, taken from [Google Fonts `ofl/doto`](https://github.com/google/fonts/tree/main/ofl/doto). The SIL text is in `assets/fonts/OFL.txt`. | SIL Open Font Licence 1.1 |
| Letter arrangements | Standard AZERTY and QWERTY. The symbol pages follow the familiar phone-keyboard arrangement and are drawn by `KothaKeyKeyboardView`. | Arrangement, not copied code |
| Launcher icons | Raster sizes produced by `tools/gen_launcher_icons.py` from the project logo. | Project art |

`docs/battery-usage.md` is a measurement note from a phone on 12 November 2026. It is not a third-party component.

Because `bengali_words.txt` is GPL-3.0, a build that ships that list is a combined work under GPL-3.0. The Avro Java sources keep their Apache-2.0 headers. The font stays under the SIL Open Font Licence. The French and English lists stay CC-BY-SA-4.0.
