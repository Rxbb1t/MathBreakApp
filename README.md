# Brain Break

A small, calm Android app that taps you on the shoulder every few hours with one math problem.

No accounts, no network, no ads, no analytics. Everything it knows stays on the phone.

- **Adaptive difficulty.** A hidden 0-100 level moves with how you answer. Getting one wrong never drops it, and answering quickly carries you further than answering slowly.
- **Eleven kinds of exercise.** Arithmetic, equations, geometry with diagrams, money, clocks, word problems, riddles, set theory, estimation, and tap-to-answer cards. Each can be switched off on its own.
- **Estimating, where close counts.** "Roughly how much is 297 × 4?" Round it, get near, and the app tells you what it was near.
- **A daily challenge.** One five-step chain per day, where each step feeds the next.
- **Worked solutions.** "Show me why" after any problem you finish.
- **No countdown.** Nothing is ever timed. Solving something quickly still counts for more, quietly.
- **Home-screen widget** and gentle reminder notifications.
- **English and Romanian**, switchable in Settings. Adding a third language means re-authoring the problem templates rather than translating strings, since the app generates its own content. See the note at the bottom.

---

## What you need

| | |
|---|---|
| Android Studio | A recent version (this project builds with Android Gradle Plugin 9.2.1) |
| JDK | 17 or newer. Android Studio ships with one, so you usually do not install anything |
| Android SDK | API 36 |
| A phone or emulator | Android 8.0 (API 26) or newer |

Gradle itself downloads on first build. You do not install it.

---

## Run it

1. **Clone the repo.**

   ```bash
   git clone https://github.com/Rxbb1t/MathBreakApp.git
   cd MathBreakApp
   ```

2. **Open the folder in Android Studio.** Choose *Open*, not *New Project*. Wait for the first Gradle sync to finish. This takes a few minutes the first time because it downloads Gradle and every dependency.

3. **Pick a device** in the dropdown at the top. Either plug in a phone with USB debugging turned on, or create an emulator through *Device Manager*.

4. **Press the green ▶ Run button.**

That is it. Android Studio writes `local.properties` with your SDK path automatically on first sync, so there is nothing to configure.

### From the command line instead

```bash
./gradlew installDebug     # macOS / Linux
.\gradlew.bat installDebug  # Windows
```

This builds and installs the debug app on whatever device is connected.

---

## Run the tests

There are around 295 unit tests. They cover every problem generator, the adaptive level ladder, storage, and both languages.

In Android Studio: right-click `app/src/test` and choose *Run 'Tests in ...'*.

From the command line:

```bash
./gradlew test
```

They run in a couple of minutes and need no device.

---

## Build an installable APK

The debug app that Android Studio installs is signed with a throwaway key. To build a real APK you can hand to someone, you need a signing key.

### One-time setup

1. **Make a keystore** if you do not have one. In Android Studio: *Build → Generate Signed App Bundle / APK → APK → Create new...*. Keep the file somewhere outside the project, and back it up.

2. **Create `keystore.properties`** in the project root:

   ```properties
   storeFile=C:\\Users\\you\\.android\\my-release.jks
   storePassword=yourStorePassword
   keyAlias=yourAlias
   keyPassword=yourKeyPassword
   ```

   On macOS or Linux use plain forward slashes in the path.

> **Keep this file and the keystore safe, and never commit them.** Both are already in `.gitignore`. An update only installs over an existing app when it is signed with the *same* key. Lose the key and the only way forward is uninstall and reinstall, which wipes all saved progress.

### Every release

1. **Bump the version** in `app/build.gradle.kts`:

   ```kotlin
   versionCode = 10      // must always increase
   versionName = "1.5.4" // the label people see
   ```

   Android refuses to install an APK whose `versionCode` is lower than the one already on the phone.

2. **Build:**

   ```bash
   ./gradlew assembleRelease
   ```

3. **Collect the APK** from `app/build/outputs/apk/release/app-release.apk` and rename it, for example `BrainBreak-1.5.4.apk`.

In Android Studio you can do the same thing by setting the *Build Variants* panel to **release**, then *Build → Build Bundle(s) / APK(s) → Build APK(s)*. Signing is already wired into the Gradle build, so a plain release build comes out signed. You do not need the *Generate Signed APK* wizard.

### Check what you built

```bash
# adjust the build-tools version to one you have installed
SDK=$ANDROID_HOME/build-tools/37.0.0

$SDK/aapt2 dump badging BrainBreak-1.5.4.apk | head -1   # version
$SDK/apksigner verify --print-certs BrainBreak-1.5.4.apk  # signing key
```

The certificate digest must match your previous release. If it does not, the APK will not install as an update.

---

## Put it on a phone

Pick whichever is easiest:

- **USB.** Copy the APK to the phone's Downloads folder, then open it in the Files app and tap *Install*.
- **Cloud.** Upload it to Google Drive, open the link on the phone, download, tap it. Android asks permission to install from that app the first time.
- **adb.** `adb install -r BrainBreak-1.5.4.apk`. The `-r` reinstalls over the existing app and keeps its data.

If the phone already has a *debug* build installed, uninstall that first. Debug and release are signed with different keys, so Android will not swap one for the other.

---

## Permissions the app asks for

| Permission | Why |
|---|---|
| Notifications | To send the break reminder |
| Alarms & reminders | So a break arrives at the time you chose rather than whenever the system feels like it |
| Run at startup | Alarms do not survive a reboot, so the app re-schedules them |

All three are optional. Reminders simply get less punctual without them, and Settings points you at the right system screen when one is missing.

There is **no internet permission at all**. The app cannot phone home, and there is no analytics or crash reporting. The trade is that a crash on your phone is invisible to everyone else, so *Settings → Report a problem* keeps the last one and gives you a button to copy it, along with your app version and device. Paste it into an issue if you hit something.

---

## A note on adding a language

Harder than it looks, and worth knowing before you start.

The UI strings all live in `i18n/Strings.kt`, which is the easy half. The hard half is that the app **generates** its problems, so a language needs the word-problem templates, the logic riddles, the geometry and money and clock stories, the thirty showpiece puzzles, and the helper-sheet notes all *re-authored* rather than translated. The tests depend on it: several assert per-language marker phrases and a fixed order of numbers inside each template.

Each language also brings its own grammar rule that only a native speaker will catch. Romanian needs "de" before a counted noun from 20 up ("12 mere" but "45 **de** mere"), which is what `i18n/RomanianCountingTest.kt` exists to enforce, and four cases still slipped through several releases. `i18n/LanguagePurityTest.kt` catches one language leaking into the other, but nothing can tell you that generated text simply reads badly.

---

## License

MIT. See [LICENSE](LICENSE). Do what you like with it, just keep the copyright notice.
