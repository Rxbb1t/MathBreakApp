# Modern Skin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a second visual skin (Modern) selectable against the preserved current look (Legacy), fix language being frozen into generated problems, and replace the system keyboard with an app-drawn keypad on a bottom-anchored control dock.

**Architecture:** One `UiSkin` setting drives typography, shapes and colour roles through `MomAppTheme`. Screens read theme tokens and never branch on the skin except where layout genuinely differs. The keypad, the dock and the language fix are *features*, so they ship to both skins and are styled by tokens.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Preferences DataStore, JUnit 4. No new dependencies except the bundled Inter font files.

**Spec:** `docs/superpowers/specs/2026-08-14-modern-skin-design.md`

## Global Constraints

- **A skin changes how the app looks, never what it can do.** Every feature ships to both skins.
- **Legacy must not drift.** `UiSkin.LEGACY` renders exactly today's values: `FontFamily.Default`, `bodyLarge` 18sp/26sp/0.5sp, shapes 10/14/20/28/32dp, `TextScale` 0.99f, `MaxChromeFontScale` 1.5f, emoji icons, `outlineVariant` left undefined.
- **Modern `TextScale` = 0.75f.** Legacy stays 0.99f.
- **Control scale** = `clamp(1.0f, systemFontScale, 1.45f)`. Touch targets never follow the skin's text baseline.
- **Inter is SIL OFL.** Must include `latin-ext` so `ș`/`ț` render; `LanguagePurityTest` must stay green.
- **No em-dashes in any user-facing string**, and no listy triplets. Existing project rule.
- **Romanian strings must be written with the Edit tool**, never via `perl -i -pe` with raw UTF-8, which has corrupted diacritics twice in this project's history.
- **Romanian counting:** any new counted noun needs `de` from 20 up. `RomanianCountingTest` enforces this.
- **Answers are always non-negative `Int`.** `Problem.answer: Int`, `Problem.tolerance: Int`. No minus key, no decimal point.
- Test command: `./gradlew :app:testDebugUnitTest`. Single test: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.problem.ProblemGeneratorTest"`.
- Build check: `./gradlew :app:assembleDebug` and `./gradlew :app:assembleRelease` must both succeed.

---

## File Structure

**Phase 1 — language (no skin dependency)**
- Modify `problem/ProblemGenerator.kt` — add `ProblemSpec`, seed each candidate, add `replay`.
- Modify `problem/Problem.kt` — add `spec: ProblemSpec?`.
- Modify `ui/problem/ProblemViewModel.kt` — replay the live problem when language changes.
- Modify `ui/challenge/ChallengeViewModel.kt` — collect language instead of reading it once.
- Create `app/src/test/java/com/ak/momapp/problem/LanguageReplayTest.kt`.

**Phase 2 — skin foundation**
- Create `ui/theme/UiSkin.kt` — the enum plus `LocalSkin`.
- Create `ui/theme/Scales.kt` — `LocalControlScale` and the `Dp.scaled` helper.
- Create `app/src/main/res/font/` — Inter files plus `inter.xml` family.
- Modify `ui/theme/Type.kt` — `LegacyTypography` and `ModernTypography`.
- Modify `ui/theme/Theme.kt` — skin parameter, per-skin scales and shapes.
- Modify `ui/theme/Palettes.kt` — `colors(dark, skin)` adding container and outline roles for Modern.
- Modify `data/SettingsRepository.kt` — `UI_SKIN` key, `decodeSkin`, `setSkin`.
- Modify `ui/AppRoot.kt` — collect the skin, pass it to the theme.

**Phase 3 — shared components (both skins)**
- Create `ui/problem/Keypad.kt`.
- Create `ui/problem/ProblemDock.kt`.
- Create `ui/problem/StartContent.kt` — extracted from `ProblemScreen.kt`.
- Create `ui/icons/AppIcons.kt` — vector icons and the Start mark.
- Modify `ui/problem/ProblemViewModel.kt` — input lockout.
- Modify `ui/problem/ProblemScreen.kt` — consume the dock, drop `imePadding`.

**Phase 4 — screens**
- Modify `ui/settings/SettingsScreen.kt`, `ui/stats/StatsScreen.kt`, `ui/exercises/ExercisesScreen.kt`, `ui/challenge/ChallengeScreen.kt`.

`ProblemScreen.kt` is 1047 lines before this work. Extracting `StartContent`, `Keypad` and `ProblemDock` is part of the plan, not a follow-up.

---

# Phase 1 — Language goes live

Independently shippable. Do not start Phase 2 until Phase 1 is committed and green.

### Task 1: Give every problem a replayable seed

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/problem/Problem.kt`
- Modify: `app/src/main/java/com/ak/momapp/problem/ProblemGenerator.kt:148-184`
- Test: `app/src/test/java/com/ak/momapp/problem/LanguageReplayTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `ProblemSpec(seed: Long, level: Level, topics: Set<ProblemTopic>, levels: Map<ProblemTopic, Level>, review: ReviewPick?)`; `Problem.spec: ProblemSpec?`; `ProblemGenerator.Companion.replay(spec: ProblemSpec, language: AppLanguage): Problem`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/ak/momapp/problem/LanguageReplayTest.kt`:

```kotlin
package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageReplayTest {

    @Test
    fun `every generated problem carries a spec that reproduces it`() {
        val generator = ProblemGenerator(Random(20260814))
        repeat(200) {
            val problem = generator.generate(Level(50), AppLanguage.ENGLISH)
            val spec = problem.spec
            assertNotNull("problem had no spec: ${problem.text}", spec)
            val again = ProblemGenerator.replay(spec!!, AppLanguage.ENGLISH)
            assertEquals(problem.text, again.text)
            assertEquals(problem.answer, again.answer)
            assertEquals(problem.kind, again.kind)
            assertEquals(problem.hints, again.hints)
            assertEquals(problem.solution, again.solution)
        }
    }

    @Test
    fun `replaying in the other language keeps the maths and changes the words`() {
        val generator = ProblemGenerator(Random(4242))
        var differed = 0
        repeat(200) {
            val english = generator.generate(Level(50), AppLanguage.ENGLISH)
            val romanian = ProblemGenerator.replay(english.spec!!, AppLanguage.ROMANIAN)
            assertEquals("answer moved for: ${english.text}", english.answer, romanian.answer)
            assertEquals(english.kind, romanian.kind)
            assertEquals(english.cards, romanian.cards)
            assertEquals(english.correctCards, romanian.correctCards)
            assertEquals(english.tolerance, romanian.tolerance)
            assertEquals(english.answerUnit, romanian.answerUnit)
            assertEquals(english.diagram, romanian.diagram)
            if (english.text != romanian.text) differed++
        }
        // Language-blind kinds (COMPARE, TARGET) share their text, so this is
        // a floor rather than a total. Most problems must actually translate.
        assertTrue("only $differed of 200 problems changed wording", differed > 120)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.problem.LanguageReplayTest"`
Expected: FAIL, unresolved reference `spec` and unresolved reference `replay`.

- [ ] **Step 3: Add `ProblemSpec` and the `spec` field**

In `problem/Problem.kt`, above the `Problem` data class:

```kotlin
/**
 * Everything needed to build one particular problem again.
 *
 * A problem's words are baked in at generation time, so changing the
 * language cannot re-word one that already exists. Replaying its seed can:
 * every sub-generator draws from the one [ProblemGenerator] random, so a
 * seed plus the level it was dealt at determines the problem completely.
 */
data class ProblemSpec(
    val seed: Long,
    val level: Level,
    val topics: Set<ProblemTopic>,
    /** Resolved per-topic levels, so replay needs no lambda. */
    val levels: Map<ProblemTopic, Level>,
    val review: ReviewPick?,
)
```

Add to the `Problem` data class, after `tolerance`:

```kotlin
    /**
     * How to rebuild this problem in another language. Null for problems
     * built by hand in tests, which never need re-wording.
     */
    val spec: ProblemSpec? = null,
```

- [ ] **Step 4: Seed each candidate and add `replay`**

In `ProblemGenerator.kt`, replace the body of `generate` (currently lines 165-184) with:

```kotlin
        val levels = ProblemTopic.ALL.associateWith(levelFor)
        var spare: Problem? = null
        repeat(REROLL_LIMIT) {
            val spec = ProblemSpec(random.nextLong(), level, topics, levels, review)
            val problem = replay(spec, language)
            if (worthKeeping(problem, spare)) spare = problem
            if (problem.kind == lastKind) return@repeat
            if (review != null && ProblemShape.of(problem) == review.shape) {
                return remember(problem)
            }
            if (keyOf(problem) !in recentTexts && ProblemShape.of(problem) !in recentShapes) {
                return remember(problem)
            }
        }
        return remember(
            spare ?: replay(ProblemSpec(random.nextLong(), level, topics, levels, review), language),
        )
```

Add to `ProblemGenerator`'s companion object (create one if the constants live in a companion already, otherwise add alongside `REROLL_LIMIT`):

```kotlin
        /**
         * Rebuilds exactly the problem [spec] describes, worded in
         * [language].
         *
         * Deliberately NOT routed through [generate]. The repeat rings are
         * keyed on the problem's TEXT, which differs by language, so a roll
         * accepted in English can be rejected as a repeat in Romanian and
         * the two runs diverge. Replay builds one known problem on a
         * throwaway generator with empty rings; it never chooses.
         */
        fun replay(spec: ProblemSpec, language: AppLanguage): Problem =
            ProblemGenerator(Random(spec.seed))
                .roll(spec.level, language, spec.topics, { spec.levels[it] ?: spec.level }, spec.review)
                .copy(spec = spec)
```

- [ ] **Step 5: Run the new test and the whole suite**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.problem.LanguageReplayTest"`
Expected: PASS.

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. If `ProblemGeneratorTest`'s repeat-ring or share assertions fail, the seeding changed the draw order — the fix is that `random.nextLong()` must be drawn *once per candidate before* `roll`, never inside it.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ak/momapp/problem/Problem.kt \
        app/src/main/java/com/ak/momapp/problem/ProblemGenerator.kt \
        app/src/test/java/com/ak/momapp/problem/LanguageReplayTest.kt
git commit -m "Every problem can be built again from its seed"
```

---

### Task 2: Re-word the live problem when the language changes

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/ui/problem/ProblemViewModel.kt:466-496`
- Test: `app/src/test/java/com/ak/momapp/problem/LanguageReplayTest.kt`

**Interfaces:**
- Consumes: `ProblemSpec`, `ProblemGenerator.replay` from Task 1.
- Produces: nothing new; `ProblemUiState.problem` becomes language-reactive.

- [ ] **Step 1: Write the failing test**

Append to `LanguageReplayTest.kt`:

```kotlin
    @Test
    fun `re-wording preserves everything the answer depends on`() {
        val generator = ProblemGenerator(Random(99))
        val english = generator.generate(Level(60), AppLanguage.ENGLISH)
        val romanian = ProblemGenerator.replay(english.spec!!, AppLanguage.ROMANIAN)
        // The spec must survive the round trip, or a second switch back
        // would have nothing to replay from.
        assertEquals(english.spec, romanian.spec)
        assertEquals(english.maxAttempts, romanian.maxAttempts)
        assertEquals(english.submitsOnTap, romanian.submitsOnTap)
        assertEquals(english.effort, romanian.effort, 0.0001)
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.problem.LanguageReplayTest"`
Expected: FAIL on `assertEquals(english.spec, romanian.spec)` if `.copy(spec = spec)` was omitted in Task 1. If it passes immediately, Task 1 was complete — record that and move on.

- [ ] **Step 3: Collect the language in the view model**

In `ProblemViewModel.kt`, add to the `init` block (create one after the `successSound` declaration if none exists):

```kotlin
    init {
        // The words are baked into a problem when it is generated, so a
        // language change has to rebuild the one she is looking at. Her
        // typed input, attempt count and phase all survive: only the
        // wording is replaced.
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.language }
                .distinctUntilChanged()
                .drop(1)
                .collect { language ->
                    _uiState.update { state ->
                        val spec = state?.problem?.spec ?: return@update state
                        state.copy(problem = ProblemGenerator.replay(spec, language))
                    }
                }
        }
    }
```

Add the imports `kotlinx.coroutines.flow.distinctUntilChanged`, `kotlinx.coroutines.flow.drop`, `kotlinx.coroutines.flow.map`.

- [ ] **Step 4: Run the suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ak/momapp/ui/problem/ProblemViewModel.kt \
        app/src/test/java/com/ak/momapp/problem/LanguageReplayTest.kt
git commit -m "Switching language re-words the problem on screen"
```

---

### Task 3: Make the daily challenge follow the language

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/ui/challenge/ChallengeViewModel.kt:69-73`
- Test: `app/src/test/java/com/ak/momapp/problem/DailyChallengeGeneratorTest.kt`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: nothing new.

- [ ] **Step 1: Write the failing test**

Append to `DailyChallengeGeneratorTest.kt`:

```kotlin
    @Test
    fun `the same day gives the same numbers in both languages`() {
        val date = java.time.LocalDate.of(2026, 8, 14)
        val en = DailyChallengeGenerator().generate(date, AppLanguage.ENGLISH)
        val ro = DailyChallengeGenerator().generate(date, AppLanguage.ROMANIAN)
        assertEquals(en.stages.size, ro.stages.size)
        en.stages.zip(ro.stages).forEach { (e, r) ->
            assertEquals("stage answers must match across languages", e.answer, r.answer)
            assertNotEquals("stage text should be translated", e.text, r.text)
        }
    }
```

Add imports `org.junit.Assert.assertNotEquals` and `com.ak.momapp.i18n.AppLanguage` if absent.

- [ ] **Step 2: Run it**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.problem.DailyChallengeGeneratorTest"`
Expected: PASS. This documents the property the fix depends on. If it FAILS, stop: the challenge is not language-stable and Step 3 is unsafe. Report before continuing.

- [ ] **Step 3: Collect the language instead of reading it once**

In `ChallengeViewModel.kt`, replace the one-shot read at line 69:

```kotlin
            val language = settingsRepository.settings.first().language
```

with a collection that rebuilds the day when the language changes:

```kotlin
            // A one-shot read meant the challenge kept whatever language was
            // active when it was first built. The numbers come from
            // Random(epochDay), so rebuilding costs nothing and returns the
            // same story correctly worded.
            settingsRepository.settings
                .map { it.language }
                .distinctUntilChanged()
                .collect { language ->
```

and close the new `collect` block around the existing body that follows, keeping `generator.generate(date, language)` inside it. Add imports `kotlinx.coroutines.flow.distinctUntilChanged` and `kotlinx.coroutines.flow.map`.

Verify the stage index and the answered/unanswered phase are read from `ChallengeRepository` inside the collect block, so a language change does not reset her position in the story.

- [ ] **Step 4: Run the suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ak/momapp/ui/challenge/ChallengeViewModel.kt \
        app/src/test/java/com/ak/momapp/problem/DailyChallengeGeneratorTest.kt
git commit -m "The daily challenge follows the language setting"
```

---

### Task 4: Phase 1 verification

- [ ] **Step 1:** Run `./gradlew :app:testDebugUnitTest`. Expected: all green, count >= the pre-change count plus 4.
- [ ] **Step 2:** Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3:** Install and check by hand: start a problem, open Settings, switch EN to RO, go back. The problem text must be in Romanian with the same numbers. Repeat inside the daily challenge.
- [ ] **Step 4:** Report results before starting Phase 2.

---

# Phase 2 — Skin foundation

### Task 5: The `UiSkin` setting

**Files:**
- Create: `app/src/main/java/com/ak/momapp/ui/theme/UiSkin.kt`
- Modify: `app/src/main/java/com/ak/momapp/data/SettingsRepository.kt`
- Test: `app/src/test/java/com/ak/momapp/data/SettingsSerializationTest.kt`

**Interfaces:**
- Produces: `enum class UiSkin { LEGACY, MODERN }`; `LocalSkin: ProvidableCompositionLocal<UiSkin>`; `SettingsSerialization.decodeSkin(raw: String?): UiSkin`; `BrainBreakSettings.skin: UiSkin`; `SettingsRepository.setSkin(skin: UiSkin)`.

- [ ] **Step 1: Write the failing test**

Append to `SettingsSerializationTest.kt`:

```kotlin
    @Test
    fun `an unset or unknown skin means modern`() {
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin(null))
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin(""))
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin("SPARKLY"))
    }

    @Test
    fun `a stored skin round trips`() {
        assertEquals(UiSkin.LEGACY, SettingsSerialization.decodeSkin("LEGACY"))
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin("MODERN"))
    }
```

Add `import com.ak.momapp.ui.theme.UiSkin`.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.data.SettingsSerializationTest"`
Expected: FAIL, unresolved reference `UiSkin`.

- [ ] **Step 3: Create the enum**

Create `ui/theme/UiSkin.kt`:

```kotlin
package com.ak.momapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Which look the app wears.
 *
 * A skin changes how the app looks and never what it can do: every
 * feature ships to both, and what differs is typography, shape, colour
 * roles and elevation. Screens ask the theme for a token rather than
 * asking which skin they are; [LocalSkin] is for the few places where the
 * LAYOUT genuinely differs, and each use wants a comment saying why a
 * token could not express it.
 */
enum class UiSkin {
    /** Exactly the look shipped through v1.6. Frozen on purpose. */
    LEGACY,

    /** Inter, tonal depth, drawn icons. */
    MODERN,
}

val LocalSkin = staticCompositionLocalOf { UiSkin.MODERN }
```

- [ ] **Step 4: Wire it into settings**

In `SettingsRepository.kt`:

Add to `Keys`:

```kotlin
        /** Which look; see [SettingsSerialization.decodeSkin]. */
        val UI_SKIN = stringPreferencesKey("ui_skin")
```

Add to `BrainBreakSettings`, after `successSound`:

```kotlin
    /** Which look the app wears; see [UiSkin]. */
    val skin: UiSkin = UiSkin.MODERN,
```

Add to `SettingsSerialization`, beside `decodePalette`:

```kotlin
    fun decodeSkin(raw: String?): UiSkin =
        UiSkin.entries.firstOrNull { it.name == raw } ?: UiSkin.MODERN
```

Add to the `settings` flow mapping:

```kotlin
            skin = SettingsSerialization.decodeSkin(prefs[Keys.UI_SKIN]),
```

Add a setter beside the other setters:

```kotlin
    suspend fun setSkin(skin: UiSkin) {
        context.brainBreakDataStore.edit { it[Keys.UI_SKIN] = skin.name }
    }
```

Add `import com.ak.momapp.ui.theme.UiSkin`.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/ak/momapp/ui/theme/UiSkin.kt \
        app/src/main/java/com/ak/momapp/data/SettingsRepository.kt \
        app/src/test/java/com/ak/momapp/data/SettingsSerializationTest.kt
git commit -m "Add the skin setting, defaulting to Modern"
```

---

### Task 6: Bundle Inter

**Files:**
- Create: `app/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`, `inter.xml`
- Modify: `app/src/main/java/com/ak/momapp/ui/theme/Type.kt`

**Interfaces:**
- Produces: `InterFamily: FontFamily`.

- [ ] **Step 1: Confirm the platform floor**

Already verified while writing this plan: `minSdk = 26`, Compose BOM `2026.02.01`. Variable fonts are therefore supported, and every `surfaceContainer*` role Task 9 needs exists in this Material 3.

**This plan still uses four static weights**, because they behave identically on every device and remove a whole class of "the weight axis did not apply" bugs. Do not switch to a variable font without a reason.

- [ ] **Step 2: Download the four static weights**

Fetch from the Inter release on Google Fonts (SIL OFL). Save as `app/src/main/res/font/inter_regular.ttf` (400), `inter_medium.ttf` (500), `inter_semibold.ttf` (600), `inter_bold.ttf` (700). Filenames must be lowercase with underscores or the resource will not compile.

Verify each file is a real TTF and not an HTML error page:

```bash
for f in app/src/main/res/font/inter_*.ttf; do echo "$f $(stat -c%s "$f") bytes"; done
```

Expected: each over 200 KB.

- [ ] **Step 3: Create the family**

Create `app/src/main/res/font/inter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
    <font android:fontStyle="normal" android:fontWeight="400" android:font="@font/inter_regular" />
    <font android:fontStyle="normal" android:fontWeight="500" android:font="@font/inter_medium" />
    <font android:fontStyle="normal" android:fontWeight="600" android:font="@font/inter_semibold" />
    <font android:fontStyle="normal" android:fontWeight="700" android:font="@font/inter_bold" />
</font-family>
```

- [ ] **Step 4: Expose it from Kotlin**

Add to `Type.kt`:

```kotlin
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
```

Add imports `androidx.compose.ui.text.font.Font` and `com.ak.momapp.R`.

- [ ] **Step 5: Verify it compiles and renders Romanian**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.i18n.LanguagePurityTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/font app/src/main/java/com/ak/momapp/ui/theme/Type.kt
git commit -m "Bundle Inter for the Modern skin"
```

---

### Task 7: Two type scales

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/ui/theme/Type.kt`
- Test: `app/src/test/java/com/ak/momapp/ui/theme/LegacyFrozenTest.kt` (create)

**Interfaces:**
- Consumes: `InterFamily` from Task 6.
- Produces: `LegacyTypography: Typography`, `ModernTypography: Typography`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/ak/momapp/ui/theme/LegacyFrozenTest.kt`:

```kotlin
package com.ak.momapp.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Legacy is the look the app shipped through v1.6, and its whole value is
 * that it does not move. These assertions are the record of what it was;
 * if one fails, either the change belongs in Modern or the record needs a
 * deliberate update.
 */
class LegacyFrozenTest {

    @Test
    fun `legacy body text is unchanged`() {
        val body = LegacyTypography.bodyLarge
        assertEquals(FontFamily.Default, body.fontFamily)
        assertEquals(18.sp, body.fontSize)
        assertEquals(26.sp, body.lineHeight)
        assertEquals(0.5.sp, body.letterSpacing)
    }

    @Test
    fun `legacy customises nothing else`() {
        assertEquals(null, LegacyTypography.titleLarge.fontFamily)
        assertEquals(null, LegacyTypography.labelLarge.fontFamily)
    }

    @Test
    fun `modern uses inter everywhere it sets a family`() {
        listOf(
            ModernTypography.bodyLarge,
            ModernTypography.titleLarge,
            ModernTypography.labelLarge,
            ModernTypography.headlineSmall,
            ModernTypography.displayLarge,
        ).forEach { assertEquals(InterFamily, it.fontFamily) }
    }

    @Test
    fun `legacy and modern text scales differ by a quarter`() {
        assertEquals(0.99f, UiSkin.LEGACY.textScale, 0.0001f)
        assertEquals(0.75f, UiSkin.MODERN.textScale, 0.0001f)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.ui.theme.LegacyFrozenTest"`
Expected: FAIL, unresolved references.

- [ ] **Step 3: Rename the existing typography and add Modern**

In `Type.kt`, rename the existing `Typography` value to `LegacyTypography`, keeping its comment and values byte-identical. Then add:

```kotlin
/**
 * Modern's scale. Weights and tracking are deliberate rather than
 * inherited, and every numeric style asks for tabular figures so the
 * answer field stops reflowing as she types and stat tiles line up.
 */
val ModernTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.9).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.35).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.7.sp,
    ),
)
```

- [ ] **Step 4: Add the per-skin scale to the enum**

In `UiSkin.kt`, add properties to the enum:

```kotlin
enum class UiSkin(
    /**
     * How large text renders before her system setting is applied.
     * Modern sits a quarter below Legacy, which was the explicit ask. The
     * app has no in-app text size control, so Legacy is the way back.
     */
    val textScale: Float,
    /**
     * How far chrome follows her system font size before it stops.
     * Modern's rows reflow, so it needs no ceiling; Legacy's cannot, which
     * is the only reason the cap exists.
     */
    val chromeFontScaleCeiling: Float,
) {
    LEGACY(textScale = 0.99f, chromeFontScaleCeiling = 1.5f),
    MODERN(textScale = 0.75f, chromeFontScaleCeiling = Float.MAX_VALUE),
    ;

    val typography: Typography
        get() = if (this == LEGACY) LegacyTypography else ModernTypography
}
```

Add imports `androidx.compose.material3.Typography`.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. `Theme.kt` still references `Typography` by its old name; fix that reference to `LegacyTypography` temporarily so the build passes, and Task 8 replaces it properly.

```bash
git add app/src/main/java/com/ak/momapp/ui/theme/ \
        app/src/test/java/com/ak/momapp/ui/theme/LegacyFrozenTest.kt
git commit -m "Give each skin its own type scale"
```

---

### Task 8: Three scales in the theme

**Files:**
- Create: `app/src/main/java/com/ak/momapp/ui/theme/Scales.kt`
- Modify: `app/src/main/java/com/ak/momapp/ui/theme/Theme.kt`
- Test: `app/src/test/java/com/ak/momapp/ui/theme/ControlScaleTest.kt` (create)

**Interfaces:**
- Consumes: `UiSkin` from Tasks 5 and 7.
- Produces: `controlScaleFor(fontScale: Float): Float`; `LocalControlScale: ProvidableCompositionLocal<Float>`; `@Composable Dp.asControl(): Dp`; `MomAppTheme(darkTheme, palette, skin, content)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/ak/momapp/ui/theme/ControlScaleTest.kt`:

```kotlin
package com.ak.momapp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Text and touch targets are different problems. Type follows her setting
 * without limit; a control must not shrink because the type did, and must
 * not grow without bound because the type did.
 */
class ControlScaleTest {

    @Test
    fun `a control never shrinks below its base size`() {
        assertEquals(1.0f, controlScaleFor(0.5f), 0.0001f)
        assertEquals(1.0f, controlScaleFor(0.85f), 0.0001f)
        assertEquals(1.0f, controlScaleFor(1.0f), 0.0001f)
    }

    @Test
    fun `a control grows with the accessibility setting`() {
        assertEquals(1.2f, controlScaleFor(1.2f), 0.0001f)
    }

    @Test
    fun `a control stops growing at the ceiling`() {
        assertEquals(1.45f, controlScaleFor(1.45f), 0.0001f)
        assertEquals(1.45f, controlScaleFor(2.0f), 0.0001f)
        assertEquals(1.45f, controlScaleFor(3.0f), 0.0001f)
    }

    @Test
    fun `the modern skin is not capped for chrome and legacy is`() {
        assertEquals(1.5f, UiSkin.LEGACY.chromeFontScaleCeiling, 0.0001f)
        assertEquals(2.4f, minOf(2.4f, UiSkin.MODERN.chromeFontScaleCeiling), 0.0001f)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.ui.theme.ControlScaleTest"`
Expected: FAIL, unresolved reference `controlScaleFor`.

- [ ] **Step 3: Create the control scale**

Create `ui/theme/Scales.kt`:

```kotlin
package com.ak.momapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/** The most a control grows for accessibility before it stops. */
private const val MaxControlScale = 1.45f

/**
 * How much larger to draw touch targets at a given system font scale.
 *
 * Never below 1: a button must not shrink because the skin's text baseline
 * did. Never above [MaxControlScale]: a keypad that grew with a 3x font
 * setting would take the whole screen.
 */
fun controlScaleFor(fontScale: Float): Float =
    fontScale.coerceIn(1f, MaxControlScale)

val LocalControlScale = staticCompositionLocalOf { 1f }

/** This dimension, grown for accessibility but not past the ceiling. */
@Composable
@ReadOnlyComposable
fun Dp.asControl(): Dp = this * LocalControlScale.current
```

- [ ] **Step 4: Rework the theme**

In `Theme.kt`, replace the `TextScale` and `MaxChromeFontScale` constants and the `MomAppTheme` body so that:

```kotlin
@Composable
fun MomAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.CLAY,
    skin: UiSkin = UiSkin.MODERN,
    content: @Composable () -> Unit,
) {
    val isDark = darkTheme || palette.alwaysDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val insets = WindowCompat.getInsetsController(window, view)
                insets.isAppearanceLightStatusBars = !isDark
                insets.isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    val density = LocalDensity.current
    // Type follows her system setting times the skin's own baseline.
    val userScale = density.fontScale * skin.textScale
    // Chrome stops where the skin says. Modern's rows reflow, so it does
    // not stop; Legacy's cannot, which is the only reason it does.
    val chromeScale = userScale.coerceAtMost(skin.chromeFontScaleCeiling)
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, chromeScale),
        LocalUserFontScale provides userScale,
        // Controls follow the ACCESSIBILITY setting, never the skin's text
        // baseline: a 25% smaller type scale must not shrink a button.
        LocalControlScale provides controlScaleFor(density.fontScale),
        LocalSkin provides skin,
    ) {
        MaterialTheme(
            colorScheme = palette.colors(isDark, skin),
            typography = skin.typography,
            shapes = skin.shapes,
            content = content,
        )
    }
}
```

Move `SoftShapes` onto the enum as `UiSkin.shapes`, with Legacy keeping 10/14/20/28/32 and Modern using 8/12/18/24/30.

`palette.colors(isDark, skin)` does not exist yet — Task 9 adds it. Until then, call `palette.colors(isDark)` so the build stays green.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug`
Expected: both PASS.

```bash
git add app/src/main/java/com/ak/momapp/ui/theme/
git commit -m "Separate the type, chrome and control scales"
```

---

### Task 9: The surface roles Modern needs

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/ui/theme/Palettes.kt`
- Test: `app/src/test/java/com/ak/momapp/ui/theme/PaletteContrastTest.kt`

**Interfaces:**
- Consumes: `UiSkin`.
- Produces: `AppPalette.colors(darkTheme: Boolean, skin: UiSkin): ColorScheme`.

- [ ] **Step 1: Write the failing test**

Append to `PaletteContrastTest.kt`:

```kotlin
    @Test
    fun `modern defines the container ladder from the palette seed`() {
        AppPalette.entries.forEach { palette ->
            listOf(true, false).forEach { dark ->
                val modern = palette.colors(dark, UiSkin.MODERN)
                val legacy = palette.colors(dark, UiSkin.LEGACY)
                // The five container roles must differ from each other, or
                // there is no ladder to build depth on.
                val ladder = listOf(
                    modern.surfaceContainerLowest, modern.surfaceContainerLow,
                    modern.surfaceContainer, modern.surfaceContainerHigh,
                    modern.surfaceContainerHighest,
                )
                assertEquals("$palette $dark ladder has duplicates", 5, ladder.toSet().size)
                // Legacy must keep Material's baseline outlineVariant, which
                // is the bug the Stats chart shows today.
                assertNotEquals(
                    "$palette $dark: modern must not inherit the baseline outline",
                    legacy.outlineVariant, modern.outlineVariant,
                )
            }
        }
    }
```

Add `import org.junit.Assert.assertNotEquals` and `import com.ak.momapp.ui.theme.UiSkin` if absent.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.ui.theme.PaletteContrastTest"`
Expected: FAIL, `colors` takes one argument.

- [ ] **Step 3: Add the skin-aware builder**

In `Palettes.kt`, change the public entry point:

```kotlin
    fun colors(darkTheme: Boolean, skin: UiSkin = UiSkin.LEGACY): ColorScheme {
        val base = if (darkTheme || alwaysDark) dark() else light()
        return if (skin == UiSkin.LEGACY) base else base.withModernSurfaces(darkTheme || alwaysDark)
    }

    /**
     * The roles [light] and [dark] never set.
     *
     * Material fills them from its own baseline palette, which is why the
     * Stats chart's zero-day stubs are lilac in every palette today. Modern
     * derives them from the same seed as everything else, so depth and
     * dividers belong to the palette she chose. Legacy keeps the baseline,
     * because Legacy is the look that shipped.
     */
    private fun ColorScheme.withModernSurfaces(isDark: Boolean): ColorScheme =
        if (!isDark) copy(
            background = primarySeed.tint(0.945f),
            surface = primarySeed.tint(0.945f),
            surfaceContainerLowest = primarySeed.tint(0.986f),
            surfaceContainerLow = primarySeed.tint(0.965f),
            surfaceContainer = primarySeed.tint(0.945f),
            surfaceContainerHigh = primarySeed.tint(0.92f),
            surfaceContainerHighest = primarySeed.tint(0.90f),
            outline = primarySeed.tint(0.62f),
            outlineVariant = primarySeed.tint(0.80f),
        ) else copy(
            background = primarySeed.shade(0.90f),
            surface = primarySeed.shade(0.90f),
            surfaceContainerLowest = primarySeed.shade(0.92f),
            surfaceContainerLow = primarySeed.shade(0.875f),
            surfaceContainer = primarySeed.shade(0.855f),
            surfaceContainerHigh = primarySeed.shade(0.82f),
            surfaceContainerHighest = primarySeed.shade(0.79f),
            outline = primarySeed.shade(0.50f),
            outlineVariant = primarySeed.shade(0.66f),
        )
```

Note the ladder must be ordered and distinct; the test asserts five different values.

- [ ] **Step 4: Point the theme at it**

In `Theme.kt`, change `palette.colors(isDark)` to `palette.colors(isDark, skin)`.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, including every existing contrast assertion.

```bash
git add app/src/main/java/com/ak/momapp/ui/theme/ \
        app/src/test/java/com/ak/momapp/ui/theme/PaletteContrastTest.kt
git commit -m "Derive the container and outline roles from the palette seed"
```

---

### Task 10: Wire the skin through the app and add the toggle

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/ui/AppRoot.kt:51-54,78`
- Modify: `app/src/main/java/com/ak/momapp/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/ak/momapp/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/ak/momapp/i18n/Strings.kt`

**Interfaces:**
- Consumes: `BrainBreakSettings.skin`, `SettingsRepository.setSkin`, `MomAppTheme(..., skin, ...)`.
- Produces: `SettingsViewModel.setSkin(UiSkin)`; strings `appearanceTitle`, `appearanceLegacy`, `appearanceModern`, `appearanceSubtitle`.

- [ ] **Step 1: Collect the skin in AppRoot**

Replace the `appearance` flow so it carries three values. Use a small local data class rather than nested `Pair`s:

```kotlin
    val appearance by remember {
        settingsRepository.settings.map {
            Appearance(it.language, it.palette, it.skin)
        }
    }.collectAsState(initial = Appearance(AppLanguage.ENGLISH, AppPalette.CLAY, UiSkin.MODERN))
```

Add at the bottom of `AppRoot.kt`:

```kotlin
private data class Appearance(
    val language: AppLanguage,
    val palette: AppPalette,
    val skin: UiSkin,
)
```

Change the theme call to `MomAppTheme(palette = appearance.palette, skin = appearance.skin)` and update the `language` usages to `appearance.language`.

- [ ] **Step 2: Add the strings**

In `Strings.kt`, add to the interface and both implementations. English:

```kotlin
    override val appearanceTitle = "Appearance"
    override val appearanceSubtitle = "Legacy keeps the original look"
    override val appearanceLegacy = "Legacy"
    override val appearanceModern = "Modern"
```

Romanian, written with the Edit tool so the diacritics survive:

```kotlin
    override val appearanceTitle = "Aspect"
    override val appearanceSubtitle = "Clasic păstrează stilul original"
    override val appearanceLegacy = "Clasic"
    override val appearanceModern = "Modern"
```

- [ ] **Step 3: Add the setter and the row**

In `SettingsViewModel.kt`:

```kotlin
    fun setSkin(skin: UiSkin) {
        viewModelScope.launch { settingsRepository.setSkin(skin) }
    }
```

In `SettingsScreen.kt`, inside the existing "The app itself" group (`settingsGroupApp`), add a row above the sound switch using the same `FilterChip` `FlowRow` pattern the starting-level picker uses, labelled `strings.appearanceTitle` with `strings.appearanceSubtitle` beneath and two chips.

- [ ] **Step 4: Verify by hand**

Run: `./gradlew :app:assembleDebug`, install, open Settings, toggle Legacy and Modern. The whole app must change look immediately, and the setting must survive a restart.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/ak/momapp/ui/ app/src/main/java/com/ak/momapp/i18n/Strings.kt
git commit -m "Let her choose between the Legacy and Modern looks"
```

---

# Phase 3 — Shared components

Everything here ships to both skins.

### Task 11: The keypad

**Files:**
- Create: `app/src/main/java/com/ak/momapp/ui/problem/Keypad.kt`
- Test: `app/src/test/java/com/ak/momapp/ui/problem/KeypadInputTest.kt` (create)

**Interfaces:**
- Consumes: `asControl()` from Task 8.
- Produces: `applyKey(current: String, key: KeypadKey): String`; `enum class KeypadKey { D0..D9, CLEAR, BACKSPACE }`; `@Composable Keypad(onKey: (KeypadKey) -> Unit, modifier: Modifier)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/ak/momapp/ui/problem/KeypadInputTest.kt`:

```kotlin
package com.ak.momapp.ui.problem

import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadInputTest {

    @Test
    fun `digits append`() {
        assertEquals("1", applyKey("", KeypadKey.D1))
        assertEquals("13", applyKey("1", KeypadKey.D3))
    }

    @Test
    fun `backspace removes the last digit and stops at empty`() {
        assertEquals("1", applyKey("13", KeypadKey.BACKSPACE))
        assertEquals("", applyKey("1", KeypadKey.BACKSPACE))
        assertEquals("", applyKey("", KeypadKey.BACKSPACE))
    }

    @Test
    fun `clear empties everything`() {
        assertEquals("", applyKey("1234", KeypadKey.CLEAR))
    }

    @Test
    fun `a leading zero is replaced rather than kept`() {
        assertEquals("5", applyKey("0", KeypadKey.D5))
        assertEquals("0", applyKey("", KeypadKey.D0))
    }

    @Test
    fun `input stops at a sane length`() {
        val long = "123456789"
        assertEquals(long, applyKey(long, KeypadKey.D1))
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.ui.problem.KeypadInputTest"`
Expected: FAIL, unresolved reference `applyKey`.

- [ ] **Step 3: Implement the key logic and the composable**

Create `Keypad.kt` with:

```kotlin
/** The longest answer any generator produces is well under this. */
private const val MaxAnswerLength = 9

enum class KeypadKey { D0, D1, D2, D3, D4, D5, D6, D7, D8, D9, CLEAR, BACKSPACE }

/**
 * The typed answer after [key] is pressed.
 *
 * Answers are non-negative whole numbers everywhere in the app, so there
 * is no sign to track and no decimal point to get wrong.
 */
fun applyKey(current: String, key: KeypadKey): String = when (key) {
    KeypadKey.CLEAR -> ""
    KeypadKey.BACKSPACE -> current.dropLast(1)
    else -> {
        val digit = key.ordinal.toString()
        when {
            current == "0" -> digit
            current.length >= MaxAnswerLength -> current
            else -> current + digit
        }
    }
}
```

Then the composable: a `Column` of four `Row`s, keys 1-9 then CLEAR / 0 / BACKSPACE. Each key is a `Surface` or `FilledTonalButton` of `height = 42.dp.asControl()`, glyphs sized from the key rather than the type scale, `MaterialTheme.colorScheme.surfaceContainerLowest` in Modern. Give each a `Modifier.semantics { contentDescription = ... }`.

- [ ] **Step 4: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.ui.problem.KeypadInputTest"`
Expected: PASS.

```bash
git add app/src/main/java/com/ak/momapp/ui/problem/Keypad.kt \
        app/src/test/java/com/ak/momapp/ui/problem/KeypadInputTest.kt
git commit -m "Add a keypad the app draws itself"
```

---

### Task 12: The input lockout

**Files:**
- Modify: `app/src/main/java/com/ak/momapp/ui/problem/ProblemViewModel.kt`
- Test: `app/src/test/java/com/ak/momapp/ui/problem/InputLockoutTest.kt` (create)

**Interfaces:**
- Produces: `ProblemViewModel.Companion.INPUT_LOCKOUT_MS: Long`; `acceptsInputAt(dealtAt: Long, now: Long): Boolean`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/ak/momapp/ui/problem/InputLockoutTest.kt`:

```kotlin
package com.ak.momapp.ui.problem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A stray second tap must not answer the problem that just arrived.
 * TRUE_FALSE has one attempt, so an accidental double-tap loses the whole
 * problem and costs seven points.
 */
class InputLockoutTest {

    @Test
    fun `a tap arriving with the problem is ignored`() {
        assertFalse(acceptsInputAt(dealtAt = 1_000L, now = 1_000L))
        assertFalse(acceptsInputAt(dealtAt = 1_000L, now = 1_100L))
    }

    @Test
    fun `a tap after the lockout is accepted`() {
        val after = 1_000L + ProblemViewModel.INPUT_LOCKOUT_MS
        assertTrue(acceptsInputAt(dealtAt = 1_000L, now = after))
        assertTrue(acceptsInputAt(dealtAt = 1_000L, now = after + 5_000L))
    }

    @Test
    fun `the lockout is short enough not to be felt`() {
        assertTrue(ProblemViewModel.INPUT_LOCKOUT_MS in 200L..500L)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ak.momapp.ui.problem.InputLockoutTest"`
Expected: FAIL, unresolved references.

- [ ] **Step 3: Implement**

In `ProblemViewModel.kt`, add to the companion object:

```kotlin
        /**
         * How long a freshly dealt problem ignores input.
         *
         * Long enough to swallow the second half of a double-tap, short
         * enough that a deliberate answer never bounces.
         */
        const val INPUT_LOCKOUT_MS = 350L
```

Add a top-level function in the same file:

```kotlin
fun acceptsInputAt(dealtAt: Long, now: Long): Boolean =
    now - dealtAt >= ProblemViewModel.INPUT_LOCKOUT_MS
```

Add `dealtAt: Long` to `ProblemUiState`, set from `System.currentTimeMillis()` in `deal()`, and guard both `submit()` and `submitChoice()` with `if (!acceptsInputAt(state.dealtAt, System.currentTimeMillis())) return`.

- [ ] **Step 4: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. If existing view-model tests submit immediately after dealing, they need their clock advanced; adjust the tests rather than weakening the lockout.

```bash
git add app/src/main/java/com/ak/momapp/ui/problem/ProblemViewModel.kt \
        app/src/test/java/com/ak/momapp/ui/problem/InputLockoutTest.kt
git commit -m "A new problem ignores input for a moment"
```

---

### Task 13: The dock

**Files:**
- Create: `app/src/main/java/com/ak/momapp/ui/problem/ProblemDock.kt`
- Create: `app/src/main/java/com/ak/momapp/ui/problem/StartContent.kt`
- Modify: `app/src/main/java/com/ak/momapp/ui/problem/ProblemScreen.kt`

**Interfaces:**
- Consumes: `Keypad`, `applyKey`, `asControl()`.
- Produces: `@Composable ProblemDock(uiState, onKey, onSubmit, onChoice, onHint, onSkip, onOpenNotes, modifier)`.

- [ ] **Step 1: Move `StartContent` out**

Cut `StartContent` (currently `ProblemScreen.kt:785-870`) and `TrophyButton` into `StartContent.kt`, changing nothing. Run `./gradlew :app:assembleDebug` and confirm it still builds. Commit this move on its own so the later diff is readable:

```bash
git add app/src/main/java/com/ak/momapp/ui/problem/
git commit -m "Move the start screen into its own file"
```

- [ ] **Step 2: Build the dock**

Create `ProblemDock.kt`. Structure, top to bottom:

1. Answer display (typed kinds only) or the tap choices row.
2. `Keypad` (typed kinds only).
3. Primary action: `Check` for typed kinds; omitted when `problem.submitsOnTap`.
4. The quiet row: Hint, Notes, Skip.

Rules that make it a dock rather than a column:

- It is the **last child of the screen `Column`** with no `weight`, so it takes its natural height and the scrolling content above takes the rest.
- The quiet row is always present and always last, so Hint and Skip sit at the same height on every problem of every kind.
- Notes is shown only when `problem.notes.isNotEmpty()`, and occupies a `Spacer` of the same width otherwise so the other two do not shift.
- Hint is hidden when `problem.hints.isEmpty()`, same `Spacer` treatment.

- [ ] **Step 3: Consume it in `ProblemScreen`**

In `ProblemScreenContent`, remove `.imePadding()` and the answer-field focus handling, put the header, dots and `ProblemTextCard` inside a `Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))`, and place `ProblemDock` after it.

Dim the header strip while answering:

```kotlin
    // Chrome recedes while she is thinking and returns the moment she
    // answers. Never to zero and never disabled: a control that vanishes
    // reads as a control that is gone.
    val chromeAlpha by animateFloatAsState(
        targetValue = if (uiState.phase == AnswerPhase.ANSWERING) 0.38f else 1f,
        label = "chrome",
    )
```

Apply with `Modifier.graphicsLayer { alpha = chromeAlpha }` on the header `Row` only.

- [ ] **Step 4: Give Modern its depth**

Depth runs both ways from one light source, and this is the only place it is built.

In `ProblemTextCard.kt`, the card takes elevation in Modern and none in Legacy:

```kotlin
    // The problem lifts off the background; the answer below it sinks into
    // one. Two directions from a single light source, which is what stops
    // a screen of stacked cards reading as flat.
    val lifted = LocalSkin.current == UiSkin.MODERN
    Surface(
        color = if (lifted) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (lifted) 0.dp else 0.dp,
        shadowElevation = if (lifted) 6.dp else 0.dp,
        shape = MaterialTheme.shapes.large,
        border = if (lifted) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) { /* existing content */ }
```

In `ProblemDock.kt`, the answer display is the inset counterpart: `surfaceContainerHigh` with no shadow and no border in Modern, and the existing 2dp `primary` outline in Legacy.

Compose has no inset shadow, so the well is read from **tone alone**: it is the darkest container in the ladder while the card is the lightest. Do not reach for a fake inner shadow drawable.

- [ ] **Step 5: Verify**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug`
Expected: both PASS.

Install and check: a typed problem shows the keypad and no system keyboard; a `TRUE_FALSE` problem shows two choices and no Check button; **the Skip button is at the same height on both**. In Modern the card reads as raised and the answer as recessed; in Legacy both are flat.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ak/momapp/ui/problem/
git commit -m "Anchor the controls so they stop moving between problems"
```

---

# Phase 4 — Screens

### Task 14: Icons and the start mark

**Files:**
- Create: `app/src/main/java/com/ak/momapp/ui/icons/AppIcons.kt`
- Modify: `app/src/main/java/com/ak/momapp/ui/problem/StartContent.kt`

**Interfaces:**
- Produces: `AppIcons.Trophy`, `AppIcons.Notebook`, `AppIcons.Tick`, `AppIcons.Cross`, `AppIcons.Backspace`, `@Composable StartMark(modifier)`.

- [ ] **Step 1:** Build the icons as `ImageVector`s. Settings and Back come from `Icons.Filled.Settings` and `Icons.AutoMirrored.Filled.ArrowBack`, which are already used and need no replacement.
- [ ] **Step 2:** Draw `StartMark` as a `Canvas`: a rounded tile in `primaryContainer`, three rods and six beads in `primary`, echoing the launcher icon.
- [ ] **Step 3:** In `StartContent.kt` and `ProblemDock.kt`, branch on `LocalSkin.current`: Modern draws vectors, Legacy keeps the emoji. **This is a sanctioned layout branch** and each site gets a one-line comment saying so.
- [ ] **Step 4:** Every icon keeps its existing `contentDescription`. Run `./gradlew :app:assembleDebug`.
- [ ] **Step 5:** Commit: `git commit -m "Draw the icons instead of borrowing the system's emoji"`

### Task 15: Settings, Exercises, and the dialogs

**Files:** `ui/settings/SettingsScreen.kt`, `ui/exercises/ExercisesScreen.kt`, `ui/WelcomeGuide.kt`, `ui/settings/ErrorReportScreen.kt`

- [ ] **Step 1: One shared card**

Both screens and both dialogs draw the same card three different ways today. Add one composable to `SettingsScreen.kt` and use it everywhere:

```kotlin
/**
 * The surface every grouped section sits on.
 *
 * Legacy's 40%-alpha wash muddied badly at large text, where more of the
 * screen is card and less is background. Modern gives it a real container
 * and a hairline instead.
 */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val modern = LocalSkin.current == UiSkin.MODERN
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (modern) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (modern) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
```

- [ ] **Step 2:** Replace every hand-rolled section card in `SettingsScreen.kt`, `ExercisesScreen.kt`, `WelcomeGuide.kt` (both the first-run guide and `PresetDialog`) and `ErrorReportScreen.kt` with `SectionCard`. This is the whole token pass for the dialogs and the error screen.
- [ ] **Step 3:** Group headings (`settingsGroupBreaks` and friends) use `MaterialTheme.typography.labelSmall` in Modern and today's style in Legacy.
- [ ] **Step 4:** In `ExercisesScreen.kt`, render the per-topic level with `labelSmall` as a short marker rather than a sentence, so the row survives maximum text without wrapping into a paragraph.
- [ ] **Step 5:** Confirm the long-press level readout still opens and that tapping a row still toggles its switch. Both were fragile when `combinedClickable` was last touched.
- [ ] **Step 6:** Run `./gradlew :app:assembleDebug`, check every one of these screens at maximum font in both skins, commit.

### Task 16: Stats

**Files:** `ui/stats/StatsScreen.kt`

- [ ] **Step 1: Tabular values**

The four tiles carry numbers of different widths that currently jitter between renders. In Modern:

```kotlin
    Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall.copy(
            // Digits of equal width, so "1284" and "74%" sit on the same
            // grid and the tiles stop twitching as the numbers change.
            fontFeatureSettings = "tnum",
        ),
    )
```

Keys use `labelSmall`. Legacy keeps its current styles.

- [ ] **Step 2:** In the chart, emphasise the most recent bar with `onBackground` and bold its weekday label, so the newest day is findable at a glance.
- [ ] **Step 3:** Zero-day stubs keep reading `outlineVariant` at `StatsScreen.kt:228` — **no code change**. It becomes palette-derived in Modern and stays Material baseline in Legacy purely from Task 9. Confirm by switching palettes on the Stats screen in both skins: Modern's stubs change colour, Legacy's stay lilac.
- [ ] **Step 4:** Run `./gradlew :app:assembleDebug`, commit.

### Task 17: Challenge

- [ ] **Step 1:** Give `ChallengeScreen` the same dock treatment: `ProblemDock` with no Skip (the challenge has none) and the notebook button retained.
- [ ] **Step 2:** Confirm the keypad appears for challenge stages and that the hint budget still caps at two with no reveal.
- [ ] **Step 3:** Confirm the language switch re-words a challenge stage mid-story without losing her position, from Task 3.
- [ ] **Step 4:** Run `./gradlew :app:assembleDebug`, commit.

---

### Task 18: Full verification

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest` — all green.
- [ ] **Step 2:** `./gradlew :app:assembleDebug` and `./gradlew :app:assembleRelease` — both succeed.
- [ ] **Step 3:** Device pass, both skins, at **maximum system font size**, on every screen: Start, Problem (typed), Problem (tapped), Solved, Settings, Stats, Exercises, Challenge, Practice, Error report. Nothing clipped, nothing off-screen, the gear reachable everywhere.
- [ ] **Step 4:** Device pass in dark mode and on one always-dark palette (Midnight or Deep forest) in both skins.
- [ ] **Step 5:** Switch language mid-problem and mid-challenge in both skins.
- [ ] **Step 6:** Confirm the skin setting survives a force-stop and relaunch.
- [ ] **Step 7:** Bump `versionCode` to 12 and `versionName` to `1.7`, build a signed release APK, and report. Do **not** bump before this point.

---

## Notes for the executor

- **Do not "improve" Legacy.** If a change makes Legacy look better, it belongs in Modern. `LegacyFrozenTest` is the guard.
- **`LocalSkin` branches are the exception.** Three are sanctioned by this plan: icons (Task 14), settings card treatment (Task 15), and the start mark. A fourth needs a stated reason.
- **The emulator degrades over long sessions.** Force-stop and relaunch before blaming the app, and allow 15-20 s after `am start` before the first tap.
- **`adb` is not on PATH.** Use `/c/Users/Anto/AppData/Local/Android/Sdk/platform-tools`, and set `MSYS_NO_PATHCONV=1` so Git Bash stops rewriting `/sdcard/...`.
- **Taps only register as `input swipe x y x y 80`** on this emulator, and `uiautomator dump` returns the previous screen often enough that a screenshot should be taken before believing one.
