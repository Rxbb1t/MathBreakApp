# Modern skin — design

Date: 2026-08-14
Status: approved in outline, pending spec review
Mockup: https://claude.ai/code/artifact/1c589a10-35d3-48f1-a8c5-83a899526af3

## Goal

Give Brain Break a second visual skin that reads as a current app, keeps every
accessibility guarantee the current one has, and can be turned off. The existing
look survives under a Legacy setting and stays behaviourally identical.

Three functional changes ride along, because they are what the redesign exposed:
language stops being frozen into a problem, the system keyboard is replaced by a
keypad the app draws, and the controls stop moving between problems.

## The governing rule

**A skin changes how the app looks. It never changes what the app can do.**

Every feature below ships to both skins. What differs between them is
typography, shape, colour roles, elevation, and one clamp. Screens ask the theme
for a token; they do not ask which skin they are drawing. `LocalSkin` exists for
the few places where layout genuinely differs, and each use needs a comment
saying why a token could not express it.

This is what stops two skins becoming two apps.

## 1. The skin mechanism

New `ui/theme/UiSkin.kt`:

```kotlin
enum class UiSkin { LEGACY, MODERN }
```

- Stored as `ui_skin` in DataStore, decoded through the existing
  `SettingsSerialization.decodeX(raw)`-with-fallback pattern. Unknown or absent
  decodes to `MODERN`.
- Added to `BrainBreakSettings` as `skin: UiSkin = UiSkin.MODERN`.
- `AppRoot.kt:51-54` already collects `language to palette` in one flow to feed
  the theme. That becomes a triple.
- `MomAppTheme(palette, skin, content)` selects typography, shapes, and the
  colour-scheme builder.
- `LocalSkin` is a `staticCompositionLocalOf<UiSkin>` provided by `MomAppTheme`.

**Default: MODERN for everyone, including existing installs.** The toggle is the
escape hatch. A redesign nobody sees by default is wasted work.

**Where the toggle lives:** Settings, under the existing "The app itself"
grouping, beside language and sound.

## 2. Typography

`ui/theme/Type.kt` currently defines exactly one style — `bodyLarge` at 18sp —
and inherits Roboto defaults for everything else.

It becomes two `Typography` instances chosen by skin.

- **Legacy**: exactly today's values. `FontFamily.Default`, `bodyLarge` 18sp /
  26sp / 0.5sp letter spacing. No other style customised. This must not drift.
- **Modern**: Inter, bundled as a variable font in `res/font/`, with a full
  scale — display, headline, title, body, label — using deliberate weights and
  negative tracking at large sizes.

**Inter is SIL OFL** and its `latin-ext` subset carries `ro_Latn`, so `ș` and `ț`
render correctly and `LanguagePurityTest` is unaffected. Verified against Google
Fonts metadata before selection.

**Tabular figures are required** wherever digits appear: the answer field, the
keypad, worked-solution steps, stat tiles, the level readout. This is
functional, not decorative — the answer field currently reflows as she types.

### Text scale

`Theme.kt` has a single `TextScale = 0.99f` applied app-wide.

It becomes skin-dependent:

| Skin   | TextScale |
|--------|-----------|
| LEGACY | 0.99 (unchanged) |
| MODERN | **0.75** |

Modern's default text is 25% smaller than Legacy's. This was the user's explicit
instruction.

**Risk, stated once and accepted:** the app has no in-app text-size control — the
`largeText` setting was removed at some point before v1.6 — so the system font
scale is the only lever. At 0.75, `bodyLarge` renders near 13.4sp and button
labels near 10.5sp. Because Legacy stays at 0.99, the skin toggle is itself the
way back. If it reads too small on her phone, the fix is one constant.

## 3. Two scales, deliberately separate

This is the core of "readable at any font size", and getting it wrong is what
forced the current clamp.

| Token | Drives | Formula |
|-------|--------|---------|
| type scale | all text | `systemFontScale × skinTextScale` |
| chrome type | labels, chips, buttons | Legacy: `min(type, 1.5)`. Modern: uncapped |
| control scale | touch targets, keypad, icon buttons | `clamp(1.0, systemFontScale, 1.45)` |

**Controls never follow the skin's text baseline.** A touch target must not
shrink because the type did, and must not grow without bound because the type
did. It starts at its base size, grows for accessibility, and stops at 1.45×.

`MaxChromeFontScale = 1.5f` is **deleted for Modern**. It exists only because the
current rows cannot reflow; the layout rules below remove the reason for it.
Legacy keeps it.

`AtUserFontScale` and `LocalUserFontScale` survive unchanged — the problem text
still gets the full system scale in both skins.

### Layout rules Modern must satisfy

1. **At most one growable text per row.** Every collision in the current layout
   is two text elements sharing a row. Everything else in a row is a fixed-size
   control or moves to its own line.
2. **Every screen is a single scrolling column.** Overflow becomes scrolling,
   never clipping.
3. **The problem scrolls; the controls do not.** See the dock, below.

## 4. Colour

`ui/theme/Palettes.kt` is not restyled. Same five palettes, same three seeds
each, same `tint`/`shade` recipe, same `PaletteContrastTest`.

Modern **adds the roles that are currently undefined**, derived from the same
seeds:

- `surfaceContainerLowest` … `surfaceContainerHighest` — the tonal ladder that
  depth is built from.
- `outline`, `outlineVariant`.

**This fixes a live bug.** `StatsScreen.kt:228` draws zero-day chart stubs with
`outlineVariant`, which `Palettes.kt` never defines, so today they render in
Material's baseline lilac regardless of the chosen palette. Legacy keeps this
behaviour (it is the original look); Modern fixes it.

`PaletteContrastTest` must be extended to cover the new roles in all five
palettes × both modes.

## 5. Shape and elevation

| | Legacy | Modern |
|---|---|---|
| extraSmall | 10dp | 8dp |
| small | 14dp | 12dp |
| medium | 20dp | 18dp |
| large | 28dp | 24dp |
| extraLarge | 32dp | 30dp |

Modern tightens the radii and spends the difference on elevation. Depth runs in
both directions from one light source: the problem card **lifts** on a wide,
low-opacity shadow; the answer field is **inset**, a well she drops the number
into.

## 6. Language goes live

### What is broken

`ProblemGenerator.generate(level, language, topics, levelFor, review)` bakes the
language into `Problem.text`, `hints`, `notes` and `solution` as plain strings.
Changing the language setting leaves all of them stale.

The challenge is worse: `ChallengeViewModel.kt:69` reads
`settingsRepository.settings.first().language` — a one-shot read — so it never
reacts to a language change at all.

### The challenge fix (cheap and exact)

`DailyChallengeGenerator` draws from `Random(date.toEpochDay())`, so both
languages already produce an identical story by construction. Collect the
language instead of reading it once, regenerate for the day, and the correctly
worded challenge comes back with the same numbers. No stored state changes, no
progress lost.

### The ordinary-problem fix (seed replay)

`ProblemGenerator` holds one `Random` and passes it to every sub-generator
(`EquationGenerator(random)`, `LogicProblemGenerator(random)`, and twelve more).
A problem is therefore fully determined by its seed.

- Draw an explicit `seed` per problem; build the problem from `Random(seed)`.
- Store enough on the `Problem` to replay it: the seed, and the resolved `Level`
  and `ProblemTopic` actually used.
- Re-render by replaying that seed in the new language.

**The trap, and why replay must not simply re-call `generate`:** `recentTexts` is
keyed on `keyOf(problem)`, which includes the problem's text — and text differs
by language. A roll accepted in English can be rejected as a repeat in Romanian,
so the two runs would diverge. **Replay bypasses the reroll loop
(`repeat(REROLL_LIMIT)`) and the ring updates entirely.** It reconstructs one
known problem; it does not choose a new one.

### Scope of the fix

Replay covers the live problem and its worked solution. A finished problem
already scrolled past is not retranslated, and the review queue is unaffected
because it stores shapes, not text.

### The guard

A test asserting that the same seed in both languages yields identical `answer`,
`kind`, `cards`, `correctCards`, `tolerance` and `diagram`, differing only in
language-bearing strings.

This invariant already holds — `ShowpieceGeneratorTest` relies on it today — but
it is currently implicit. The test makes it explicit so that adding a
language-dependent random draw fails loudly instead of silently corrupting
replay.

## 7. The keypad

The Android keyboard is the worst thing on the problem screen: it covers the
Check button, its keys are small, digits often need a mode switch, and opening
it is a layout event that moves everything.

Replace it, in both skins, with a keypad the app draws: nine digits, zero, clear,
backspace.

- **Sized from the control scale, not the type scale.** Base 42dp keys, growing
  with the accessibility setting, capped at 1.45×.
- **Glyphs sized from the key**, not from type, so a digit always fits the thing
  it is printed on.
- **No minus key and no decimal point.** `Problem.answer` is an `Int` and
  `Problem.tolerance` is an `Int`, so this holds for every kind including
  `ESTIMATE`. Verified, not assumed.

Removes the `imePadding()` handling and the answer field's focus management.

## 8. The dock

Today the action area is assembled by whichever problem arrived. Typed problems
get a Check button; `submitsOnTap` kinds do not, so Hint and Skip slide upward.
The buttons move between problems.

The dock is anchored to the bottom of the screen instead of following content.
Hint, Notes and Skip sit at the same height on every problem, of every kind, at
every text size. Its contents change; its position does not.

It carries three further fixes:

1. **Input lockout.** A newly dealt problem ignores input briefly. This matters
   most for `TRUE_FALSE`, which has `maxAttempts = 1`: a stray double-tap
   currently loses the problem outright at a cost of 7 points.
2. **The helper sheet becomes findable.** The notebook is an edge-swipe drawer
   with a small tab today. In the dock it is a labelled button, shown only when
   `problem.notes` is non-empty.
3. **Maximum text works.** The problem scrolls; the controls never leave.

## 9. Icons

Modern replaces the emoji (🏆 ⚙️ 📝 🧮) with vector icons, and redraws the Start
mark as an SVG echoing the launcher icon's rods and beads. Emoji render
differently on every Android version and read as placeholders.

Legacy keeps the emoji. This is one of the few genuine `LocalSkin` layout
branches.

## 10. The top strip

On the problem screen, the level chip / trophy / gear strip **dims to 38% while
she is answering** and returns to full on feedback.

It is never dimmed to zero and never disabled — a control that fades out reads as
a control that has disappeared. Worth watching on her device before considering
this settled.

## Screens in scope

All eight. Most of Settings, Stats, Exercises and the dialogs is carried by
tokens alone; their layout changes are minor.

| Screen | Work |
|---|---|
| `ProblemScreen` (1047 lines, also drives Practice) | Dock, keypad, zones, strip dimming |
| `SettingsScreen` (836) | Tokens, card edges, skin toggle row |
| `ChallengeScreen` (451) | Tokens, dock, live language |
| `StatsScreen` (361) | Tokens, tile treatment, chart endpoint |
| `ExercisesScreen` (206) | Tokens, level-as-label |
| `WelcomeGuide` / `PresetDialog` (113) | Tokens |
| `ErrorReportScreen` (157) | Tokens |

`ProblemScreen` at 1047 lines is already too large for one file and this work
adds to it. Extract the dock, the keypad and the start content into their own
files as part of the change — targeted improvement to code being worked in, not
an unrelated refactor.

## Testing

- `PaletteContrastTest` extended to the new surface and outline roles, all five
  palettes × both modes.
- New: same-seed-both-languages invariant (section 6).
- New: challenge regenerates identically across a language change.
- New: replay reproduces a problem exactly, without touching the repeat rings.
- New: `SettingsSerialization` round-trip and fallback for `ui_skin`.
- New: input lockout rejects a submit inside the window and accepts after it.
- Legacy regression: `Typography`, `Shapes` and `TextScale` for `UiSkin.LEGACY`
  match today's values exactly. This is the test that stops Legacy drifting.
- Manual on device: every screen at maximum system font in both skins, both
  themes, at least one always-dark palette.

## Out of scope

- Read-aloud / TTS.
- Any change to the adaptive ladder, scoring, or problem content.
- Restoring an in-app text-size control (noted as the remedy if 0.75 proves too
  small, but not built now).
- Retranslating finished problems already scrolled past.

## Settled decisions

1. Default skin is **MODERN** for everyone.
2. **Vector icons in Modern, emoji in Legacy.**
3. Top strip **dims to 38%**, never zero, never disabled.
4. Language replay covers **the live problem and its solution**; finished history
   is left alone.
5. Modern `TextScale` **0.75**; Legacy stays **0.99**.
6. Keypad on the **control scale**: base 42dp, `clamp(1.0, fontScale, 1.45)`.
