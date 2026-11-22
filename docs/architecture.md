# Integration architecture

## Product direction

The target is an enhanced Badcat client, not a generic replacement reader. The existing Badcat UI and interaction flow remain the host experience. Legado supplies the open-source reading/rule capabilities behind an adapter layer.

## Evidence and uncertainty

The historical APK inspection identified UI/activity/resource names and reading/purification-related symbols, including:

- UI clues: `MainActivity`, `SearchActivity`, `SearchResultActivity`, `BookDetailActivity`, `ChapterListActivity`, `BaseReadActivity`.
- Reading clues: `com.reader.reader.base.BaseReadActivity`, `GenuineBookReadView`, `LocalBookReadView`, `ReadConfig`.
- Purification clues: `SettingAdBlockActivity`, `sens.Pp.PPRule`, `sens.AdMark.AdMarkRuleItem`, `ADBlockUtils`, `AdBlockPresenter`.

These are static inspection clues only. They do not prove the exact runtime call graph or provide the original source/backend implementation.

## Layering

### 1. Host UI and compatibility layer

Responsibilities:
- Preserve existing page hierarchy, navigation, labels, controls, reader gestures and settings behavior.
- Translate existing UI actions into engine operations.
- Convert engine data into the host's existing view models.
- Keep the original APK immutable as a reference.

No UI rewrite should be undertaken merely to make engine integration easier.

### 2. Reading engine port

The platform-neutral contract lives in `integration-contract/`. It defines the minimum operations the host needs: source listing, search, book detail, chapter listing, chapter content, and replacement-rule application.

A complete Legado adapter is not implemented yet. The build pins upstream revision `330e606c4d57e08adc056ff8ab7a4132c784da9` and retrieves the Rhino JAR plus selected Rhino extension sources during CI, but the host still lacks the full AnalyzeUrl / AnalyzeRule app-level runtime and host objects. The Legado Android app's HTTP API is not an embedded SDK, and its ContentProvider is not an in-process engine.

### 3. Legado implementation adapter

Expected responsibilities:
- Book source models and persistence.
- Search/detail/catalog/content rule execution.
- HTTP, JavaScript/Rhino and parser environment initialization.
- Replacement rule management and deterministic application.
- Import/export compatibility and error mapping.

Do not assume `:modules:book` plus `:modules:rhino` alone provide the full app-level book-source engine.

### 4. Purification pipeline

Keep separate rule domains and execution points:

| Domain | Input | Intended responsibility |
|---|---|---|
| Book text replacement | Chapter text | Remove/replace unwanted text in returned reading content |
| Web/page ad filtering | HTML/DOM/WebView | Filter page elements or inject page-specific scripts |
| Catalog/chapter cleanup | Chapter metadata/list | Normalize unwanted catalog entries and parsing artifacts |
| Video/episode rules | Video pages and episode metadata | Remain isolated from novel text and chapter replacement |

Badcat's existing purification behavior has only been statically mapped from APK symbols and resources; its original rule execution behavior has not yet been ported. The platform-neutral pipeline currently provides separate injected interfaces, not the original implementation. Legado replacement rules must not silently replace or disable Badcat's existing page/video filtering.

## Data flow

1. Existing Badcat UI requests an operation from the host-facing engine port.
2. The selected engine adapter executes the configured source rule.
3. Results are normalized into stable host models.
4. The host UI renders using its existing navigation and presentation.
5. Text replacement runs only on the intended text payload; page and video purification run in their own domains.
6. Errors are returned as typed results so the original UI can show understandable feedback.

## Staged delivery

- Stage A — repository baseline and contracts (in progress).
- Stage B — pinned Legado dependency/source review and license inventory.
- Stage C — adapter implementation and fixture-based tests.
- Stage D — original UI integration and resource compatibility.
- Stage E — purification parity tests.
- Stage F — Android build, signing, installation and device regression.

## Explicit non-goals

- No signature forgery, license bypass, native integrity bypass or protection circumvention.
- No claim that a reconstructed project is the original source.
- No copying of proprietary server credentials or private keys.
- No release APK until build and device checks actually pass.


## Modern implementation policy (confirmed 2026-11-16)

The original application is a behavioral and visual reference, not an implementation template. The new client should reproduce the required user-visible behavior with clean, modern, maintainable code rather than transplanting an obsolete architecture.

### Preferred Android stack

- Kotlin as the primary application language; use coroutines and `Flow` / `StateFlow` for asynchronous work and observable state.
- Jetpack Compose for new/rebuilt screens where it improves iteration and testability. Keep the existing custom visual language; do not apply default Material styling when it would alter the reference UI.
- Use Compose/View interoperability where a gradual migration is safer than rewriting the entire shell at once.
- ViewModel for screen state and lifecycle-aware collection; keep UI state separate from source/network/parser logic.
- Room for structured relational data such as books, folders, reading progress and history when migration is justified; DataStore for small settings. Provide explicit migration from existing SharedPreferences data before changing storage.
- Repository and use-case boundaries for book sources, search, catalogs, chapters, favorites and history. Keep Legado behind a stable engine adapter.
- Keep WebView only where web rendering or the CSS reader requires it; apply restrictive WebView settings and do not expose native bridges to untrusted source content.
- Use Gradle version catalogs and pinned dependency versions. Prefer current stable AndroidX releases compatible with the project's compile/target SDK; do not chase alpha/beta versions merely to be “latest”.

### Compatibility rules

1. Preserve original navigation, labels, control placement, gestures and user-visible outcomes. Internal class names and data structures may be redesigned.
2. Match the reference screenshots and measured dimensions; do not force Material 3 components if they change the appearance. Compose is an implementation tool, not a visual redesign mandate.
3. Keep engine operations off the main thread; surface loading, empty, error and success states explicitly.
4. Use stable IDs and transactional migrations for persisted user data. Never discard existing favorites, folders, history, bookmarks or reader preferences during a refactor.
5. Keep domain logic testable without Android UI; use fixtures and screenshot/UI tests for presentation and navigation.
6. Replace temporary development screens with finished host screens, not by adding more shortcuts to the launcher.
7. Avoid unnecessary abstraction and duplicate compatibility layers. Prefer small, cohesive modules with explicit contracts.

### Delivery order

1. Rebuild the main shell and home screen against the reference screenshot.
2. Rebuild favorites and folder presentation while preserving the existing shelf operations and migrating stored data safely.
3. Rebuild search home/results and connect the current source engine through view models.
4. Rebuild book detail and chapter catalog.
5. Rebuild the reader chrome/settings while retaining CSS profiles and existing reader preferences.
6. Run screenshot, navigation, persistence and device regression tests before marking each screen complete.
