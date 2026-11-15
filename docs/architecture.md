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

A concrete Legado adapter is not implemented yet. It must be pinned to a reviewed upstream revision and use the full required rule execution/app-layer dependencies. The Legado Android app's HTTP API is not an embedded SDK, and its ContentProvider is not an in-process engine.

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

Badcat's existing purification behavior must be mapped before porting. Legado replacement rules must not silently replace or disable Badcat's existing page/video filtering.

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
