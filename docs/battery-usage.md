# KothaKey battery & memory usage

Measured on a physical phone over USB debugging, **2026-11-12**.

| Field | Value |
| --- | --- |
| App | `com.kothakey` |
| Version | **1.0.1** (`versionCode=2`) |
| Last update | 2026-11-12 16:54:59 |
| UID | `u0a258` |
| Device battery | ~82%, **USB powered** (charging) |
| Tools | `adb dumpsys meminfo`, `batterystats`, `procstats`, `cpuinfo`, `ps` |

> **Note:** While USB charging, Android reports `Time on battery: 0ms` and `Discharge: 0 mAh`. Absolute mAh/hour drain rates cannot be measured until the phone is unplugged. Memory, process lifetime, CPU snapshots, permissions, and code-path analysis below are still valid.

---

## Verdict (lightweight)

KothaKey is built to stay light:

- Only declared permission: **`VIBRATE`** (no `INTERNET`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`).
- No app-owned WakeLocks observed; no WorkManager / network sync / continuous background jobs in code.
- When the IME process is gone, idle/background drain is effectively **zero**.
- Work (suggestions, next-word, fuzzy) runs on keystroke/commit paths, not as a poller.
- Next-word prediction adds a small **active-typing** cost only; it does not keep the device awake when the keyboard is unused.

---

## States

### 1. Not in use / idle (process absent)

| Check | Result |
| --- | --- |
| After `am force-stop com.kothakey` | **No process** |
| After force-stop with keyboard still default | **No process** |
| `dumpsys meminfo com.kothakey` | `No process found` |
| Battery contribution | **~0** (nothing scheduled to wake the UID) |

IME processes are unbound and can be killed when the keyboard is not shown. With no sticky services, alarms, or network, **not in use ≈ no drain**.

### 2. Background (process still alive, keyboard hidden)

After showing the keyboard then hiding it (Back / hide IME), the process **can linger** until the system reclaims it:

| Moment | Process | Notes |
| --- | --- | --- |
| Keyboard shown | Alive | ~61–69 MB TOTAL PSS |
| Hide + 5 s | Still alive | Same PID |
| Hide + 30 s | Still alive | Same PID (~182 MB RSS in one sample) |
| Later / force-stop | Gone | Back to idle |

While the process is alive:

- Clipboard history registers `OnPrimaryClipChangedListener` from IME `onCreate` until `onDestroy` / `ClipboardStore.close()`.
- That listener can briefly wake the process on **clipboard change** events only (not a timer).
- No continuous CPU spin: after typing settled, `dumpsys cpuinfo` showed **`com.kothakey: 0%`**.

### 3. Active (keyboard visible / typing)

Point-in-time `dumpsys meminfo` on the phone:

| Scenario | TOTAL PSS | TOTAL RSS | Java heap (approx) | Native heap (approx) |
| --- | --- | --- | --- | --- |
| Keyboard shown (cold-ish) | **~61.4 MB** | ~177 MB | ~10 MB | ~16 MB |
| Before typing burst | **~63.5 MB** | ~176 MB | ~10 MB | ~16 MB |
| After next-word warm typing (`the quick brown fox jumped over`) | **~68.9 MB** | ~189 MB | ~10 MB | ~17 MB |

Delta after a short prediction-heavy typing burst: roughly **+5 MB PSS**.

`procstats` (device aggregate for this package):

| Build | Runtime share (sample window) | PSS range (Top) |
| --- | --- | --- |
| v1 (`versionCode=1`) | High in older window (~83–85% of that slice while Top) | ~95–100 MB (older samples higher) |
| v2 (`versionCode=2` / 1.0.1) | **~2.6%** of measured Top time in that window | **60–68 MB PSS** (39–49 MB USS) over 9 samples |
| Combined summary | ~47m45s Top across samples | **60–70–100 MB** PSS band over 11 samples |

v2 sits in a tighter, lower memory band than the earlier v1 samples on this device.

---

## Does next-word prediction use more battery?

**Slightly, and only while typing — not as background drain.**

| Aspect | Behavior |
| --- | --- |
| When it runs | On compose / commit via `ExecutorService` (`mSuggest`), not a background loop |
| Persist | Debounced disk save (`postDelayed` **5 s**) for learned / next-word model |
| Idle cost | None once the process is idle or killed |
| Measured memory | ~5 MB PSS rise after a prediction-heavy phrase |
| Measured CPU | 0% shortly after typing stopped |

Fuzzy / proximity corrections follow the same pattern: work on the keystroke path, not continuous.

Keeping next-word enabled is compatible with a lightweight keyboard; the cost is incremental CPU/RAM during active input, not standby.

---

## Comparison to other keyboards (esp. Gboard)

| Factor | KothaKey 1.0.1 | Typical Gboard |
| --- | --- | --- |
| Permissions | Vibrate only | Network, accounts, voice, etc. (product-dependent) |
| Cloud / sync / speech | None in this app | Often present |
| Resident when unused | Process often absent | Often stays resident longer |
| Active PSS (this phone) | **~60–70 MB** | Not captured in same session (process not running when sampled); generally larger when warm |
| Discharge mAh vs Gboard | **Not measured** (device on USB charge) | Same limitation this session |

**Architecture comparison (confident):** KothaKey should consume less standby and less average battery than full-featured boards (Gboard, SwiftKey, etc.) because it has no network stack, no always-on learning sync, and a smaller working set.

**Numeric mAh/hour comparison:** needs an **unplugged** side-by-side session (same typing script, same screen time, `dumpsys batterystats` per UID). Not available while USB debugging with charge current.

---

## Batterystats session notes

After `dumpsys batterystats --reset` during USB charge:

```
currently on battery: false
Time on battery: 0ms
Discharge: 0 mAh
Estimated battery capacity: 5000 mAh
Last learned battery capacity: 3858 mAh
```

History around testing showed normal `pkgactive` / `pkginactive` / `top` transitions for `u0a258:"com.kothakey"` — no kothakey-owned wake locks in the sampled power dump.

Daily device discharge steps (whole phone, not app-specific) around the prior day were on the order of **~4–12 minutes per 1%** depending on screen on/off — context only, not attributable to KothaKey.

---

## Code paths that matter for power

| Component | Path | Power relevance |
| --- | --- | --- |
| Dict load | `WordDictionary` / `BengaliLexicon` / emoji `loadAsync` | One-shot background load when IME starts |
| Suggestions | `mSuggest` executor + `LatinEngine.suggestFor` | Per keystroke / commit |
| Next-word | `NextWordModel` + delayed save | Per commit + 5 s debounce to disk |
| Clipboard | `ClipboardStore` listener | Event-driven while process alive |
| Key repeat / long-press | `Handler.postDelayed` on keyboard view | Only while touching keys |
| Permissions | `AndroidManifest` | `VIBRATE` only |

---

## How to re-measure mAh drain (unplugged)

1. Unplug USB (or use wireless ADB / charge briefly then unplug).
2. `adb shell dumpsys batterystats --reset`
3. Idle 15–30 min with KothaKey as default IME, screen off → note UID `u0a258`.
4. Active typing 15–30 min → note UID CPU / computed power.
5. Switch default IME to Gboard, repeat the same script.
6. `adb shell dumpsys batterystats --charged com.kothakey` and same for `com.google.android.inputmethod.latin`.

Append new numbers to this file under a dated section.

---

## Snapshot log (2026-11-12)

```
Keyboard shown:     TOTAL PSS ~61414 KB, RSS ~176780 KB
Before typing:      TOTAL PSS ~63482 KB, RSS ~176116 KB
After next-word:    TOTAL PSS ~68854 KB, RSS ~189128 KB
CPU after settle:   com.kothakey 0%
Force-stop idle:    NO_PROCESS
Default IME after restore: com.kothakey/.ime.KothaKeyImeService
Gboard mem sample:  No process found (at sample time)
Charging:           USB powered=true, status=2 (charging), level=82
```
