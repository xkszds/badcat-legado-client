# Runtime-1.0 PoC-7a — Badcat-owned reader/theme API seam audit

- Date: 2026-11-20
- Branch: integration/legado-upstream-runtime
- Starting HEAD: 3f003df85cf98578eca0744e43fd8f10b3c67f00
- Registration commit: 8a9d4ec27bcf2a516d542260a34222c46353e1dd
- Pinned upstream: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a
- Scope: reader/theme methods getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap; no production implementation was authorized.

## Executive result

**There is no faithful direct mapping from the current Badcat reader preferences/theme palette to the pinned Legado reader/theme configuration objects.** Badcat owns and should retain its reader and CSS UI, but the current host model is materially smaller and structurally different. Implementing the five missing JS methods by returning a fabricated Map, mapping only similarly named settings, or coercing light/dark into the upstream four-state theme mode would silently downgrade the source API.

The correct direction is a typed Badcat-owned host port for Badcat reader state, kept separate from the upstream ReadBookConfig.Config and ThemeConfig.Config contracts. Such a port may support a future explicitly host-specific API or a fully specified compatibility adapter after the missing upstream data model is implemented. It is **not** evidence that the five upstream methods are currently satisfiable.

## Pinned upstream behavior

### JS method declarations

In app/src/main/java/io/legado/app/help/JsExtensions.kt:

- getReadBookConfig(): String returns GSON.toJson(ReadBookConfig.durConfig).
- getReadBookConfigMap(): Map<String, Any> returns ReadBookConfig.durConfig.toMap().
- getThemeMode(): String returns AppConfig.themeMode ?: "0".
- getThemeConfig(): String gets ThemeConfig.getDurConfig(appCtx) and Gson-serializes it.
- getThemeConfigMap(): Map<String, Any?> returns that config's toMap().

The source help documents theme mode values: 0 system-following, 1 light, 2 dark, 3 E-Ink. The four configuration methods deliberately expose two shapes each: Gson JSON and a Map. They must not be assumed identical.

### ReadBookConfig

Pinned ReadBookConfig.durConfig is getConfig(styleSelect); styleSelect selects comic or reading style based on the global isComic state and the read/comic style preferences. getConfig(index) resets to upstream defaults if fewer than five configurations exist and falls back to the first config for an invalid index. The selected object is mutable and backed by upstream configList/share-layout state, not a small immutable preference record.

The pinned nested ReadBookConfig.Config contains roughly 80 persisted/style keys, including:

- day/night/E-Ink background strings, background types and alpha;
- status-bar icon behavior, text/accent colors and derived color values;
- page animation, font family, title font/weight, text size, letter spacing, line/paragraph spacing;
- title positioning, title color/size, chapter-title splitting and numbering;
- paragraph indent and underline mode/color/width/distance/body/title behavior/version;
- review icon color/SVG/templates/scale;
- body/header/footer paddings, linked padding and line visibility;
- header/footer tip slots, templates, sizes/colors and modes.

Its toMap() includes computed color-int entries such as textColorInt and textAccentColorInt alongside the configuration fields. The JSON method instead uses Gson serialization of the object. The two outputs therefore have distinct observable contracts; a host implementation must preserve each independently, including nullability, private-field serialization and defaults. The source model also uses file-backed image/font resources and calls resource-generation invalidation when selected config/style changes.

### ThemeConfig and AppConfig

Pinned ThemeConfig.getDurConfig(context) chooses day or night configuration based on AppConfig.isNightTheme. The resulting ThemeConfig.Config has nine fields:

themeName, isNightTheme, primaryColor, accentColor, backgroundColor, bottomBackground, transparentNavBar, nullable backgroundImgPath, and backgroundImgBlur.

Its toMap() exposes those keys. Day/night selection is not equivalent to the raw four-state AppConfig.themeMode; E-Ink is a distinct theme path and system-following resolves against system configuration. Applying upstream theme changes also affects AppCompat night mode, theme resources, default covers and Activity recreation/event dispatch. Merely reporting a color palette does not reproduce those effects.

### Static consumers

Pinned repository code search finds the API documented in app/src/main/assets/web/help/md/jsHelp.md and included in RuntimeObjectCompletionLanguage.kt. No additional concrete source-script callsite was established by this scoped search. This is not proof that external imported source libraries do not call these methods; the API is public and must remain supported.

## Current Badcat behavior and ownership

### Reader UI preferences

ReaderSettingsActivity.kt writes the Badcat-owned SharedPreferences file badcat_reader_ui:

| Key | Default | Meaning |
|---|---:|---|
| volume_page_turn | true | volume-key page turn |
| reading_lock_minutes | 0 | screen-on duration |
| show_reading_progress | true | show progress |
| reading_progress_mode | percent | percent or chapter pages |
| show_reading_time | true | show clock |
| show_reading_battery | true | show battery |
| keep_system_status_bar | true | keep system status bar |

SourceChapterActivity reads those settings for progress/time/battery/status-bar/volume behavior. They are not the pinned upstream reading-layout model.

### Reader rendering preferences

SourceChapterActivity separately stores reader_text_size (default 18sp, clamped 14–32), reader_line_spacing (default 1.15, clamped 1.0–2.0), and reader_theme (default 1, clamped 0–2) in badcat_reading_progress. It renders HTML in a script-disabled WebView and offers three local palettes (white, warm paper, dark). These are not upstream ReadBookConfig.Config fields or upstream page-animation/font/background resource semantics.

### CSS profiles

ReaderCssActivity owns reader_css_profiles/profiles_v1: up to 100 profiles, each with id/name/note/enabled and selector rules (name/note/selector/css), with bounded import and rendering. ReaderCssDocument sanitizes selectors and CSS declarations, scopes selectors beneath .reader-root, and applies enabled rules in stored order. This is a Badcat-specific extension, not an upstream ReadBookConfig or ThemeConfig representation. Returning CSS profiles inside an upstream config would invent a contract and could expose user-authored CSS/notes to every source script.

### App appearance palette

BadcatTheme reads badcat_appearance/appearance_mode, default light, with only light and dark. Its palette is hard-coded in code (page/surface/raised/text/muted/divider/accent/field/stroke). It has no system-following mode, E-Ink mode, named theme configuration, user-defined theme colors, background image path/blur, transparent-navigation-bar setting or upstream theme lifecycle. It is not a substitute for ThemeConfig.Config.

## Compatibility matrix

| Upstream API | Current Badcat equivalent | Result |
|---|---|---|
| getReadBookConfig() | Separate UI toggles + three rendering preferences + separate CSS profile JSON | **No equivalent.** Missing most upstream fields and the selected style object semantics. |
| getReadBookConfigMap() | Same partial Badcat state | **No equivalent.** Upstream Map includes many keys and computed values; cannot return a sparse map as compatible. |
| getThemeMode() | light / dark appearance preference | **Partial only.** Cannot represent 0 system, 3 E-Ink, or upstream preference side effects. |
| getThemeConfig() | Hard-coded Badcat palette | **No equivalent.** No nine-field user-configurable upstream theme object or JSON contract. |
| getThemeConfigMap() | Hard-coded Badcat palette | **No equivalent.** No upstream Map keys/null/defaults or resource semantics. |

## Typed host port proposal (not implemented)

If Badcat needs a source-visible host extension, define a Badcat-owned interface such as:

- BadcatReaderEnvironmentPort.snapshot(): BadcatReaderEnvironmentSnapshot
- BadcatReaderEnvironmentSnapshot contains only explicitly named Badcat values: reader UI toggles, text size, line spacing, selected local palette enum, appearance enum, and CSS profile metadata only if a separate privacy review approves it.
- An Android adapter reads the existing preference owners; it must not move or duplicate writes.
- The runtime consumes the typed port through constructor injection. It must not import Activity classes, reach SharedPreferences directly, or depend on Legado Reader/config classes.
- Do not expose this snapshot under the five upstream method names. If a future compatibility facade is proposed, it must first supply the complete authentic upstream model and explicit unsupported-field behavior; no partial Map or fabricated defaults.

This proposed port is deliberately Badcat-specific. It does not close the upstream API gap and should not be exposed to source JS without an explicit source-script privacy decision.

## Decision and remaining blockers

1. Do not implement the five missing methods by mapping current Badcat values.
2. Keep Badcat reader, CSS editor and appearance palette as their sole UI and persistence owners.
3. To implement upstream compatibility, first bring in or faithfully host the authentic pinned ReadBookConfig.Config and ThemeConfig.Config data/serialization contracts and required state/resource semantics without importing Legado Reader UI. This requires a separate architecture and dependency-closure registration; no schema migration is implied by this audit.
4. Separately define source-script access policy for user reader settings and CSS profiles.
5. The other PoC-5 gaps (refresh, navigation/video, WebView/browser/verification, BaseSource/login lifecycle) remain untouched.

## Changes and validation

- Added this audit report and its preregistration/result record in docs/required/03-LEGADO-RUNTIME.md only.
- No Kotlin/Java, Gradle, tests, database/schema, persistence, production callers, Badcat UI/Reader, APK or main changed.
- No build, unit-test, source-smoke or device result is claimed for this documentation-only audit.
- No B′ exit condition is newly triggered or cleared; B′ and full Legado compatibility remain unpassed.
