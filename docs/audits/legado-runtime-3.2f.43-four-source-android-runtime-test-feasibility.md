# Runtime-3.2f.43 — four-source Android runtime test feasibility and source-fixture gate

- Date: 2026-11-23
- Branch: `integration/legado-upstream-runtime`
- Registration commit / starting task SHA: `2c4b459a5555c5f3deebd041e8a1a45f451bba97`
- Pinned upstream Legado SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- State: **blocked before implementation**; no production or test files changed.

## Question

Can the four sites be tested through the Android app's existing production reading-engine path, and are the exact original BookSource descriptors available to drive that test?

## Android host entry-point findings

The Android app does expose a real host path that can be exercised by instrumentation without creating a second scraper:

1. `app/src/main/java/app/badcat/client/runtime/BadcatReadingEngineFactory.kt` creates the app's shared `ReadingEngine` graph, with `SourceSearchService`, `SourceBookDetailService`, `SourceTocService`, and `SourceChapterContentService`. It installs the app's actual OkHttp request client, durable login-session request wrapper, source cookie jar, and `RhinoSourceScriptRuntime`.
2. The same factory exposes `sourceCatalog(): FileBookSourceCatalog`. `FileBookSourceCatalog.importJson()` accepts the original Legado BookSource JSON and persists it in app-private storage.
3. `integration-contract/src/main/kotlin/app/badcat/engine/SourceReadingEngine.kt` exposes the required operations through the production `ReadingEngine` contract: `search`, `bookDetail`, `chapters`, and `chapterContent`.
4. The existing unit test `app/src/test/kotlin/app/badcat/client/runtime/ImportedNyasamaSourceSearchTest.kt` only parses deterministic captured responses; it is not a live Android instrumentation test. Current Android instrumentation files are WebView provider/cookie tests; no four-source runtime test exists.

Important qualification: this is the app's current host-integrated Legado-derived runtime path, not proof that the full pinned upstream Legado engine has been migrated. Its services and supported rule subset remain bounded/partial. A successful test would demonstrate actual app-path behavior for these descriptors only, not complete Legado parity.

## Source-fixture availability

The recursive repository tree at registration SHA `2c4b459a5555c5f3deebd041e8a1a45f451bba97` contains no original BookSource JSON fixture for these four sources and no `app/src/androidTest/assets/legado-sources/` directory. The only four-source descriptors found are hardcoded in `.github/scripts/live-uploaded-source-reading-smoke.py`; they consist of site URLs and a small hand-coded CSS selector subset, with authentication material omitted. They are not full BookSource objects and cannot be imported through `LegadoBookSourceCodec`.

The workflow step in `.github/workflows/android-build.yml` invokes that Python script directly. The corresponding Actions artifacts are named `badcat-uploaded-corpus-live-reading-report` (for example, #3223 artifact 14830136420, 997 bytes; #3224 artifact 14829062604, 978 bytes; #3225 artifact 14827498487, 962 bytes). These are small smoke-report artifacts, not the original BookSource JSON corpus. No source JSON was found in the tracked repository tree.

The Python descriptors cannot be promoted to original source fixtures: doing so would fabricate/flatten the original rule set and omit source-specific headers, JS, cookies, login configuration, metadata, and exact source identity. That would invalidate the requested real compatibility test.

## CI state at discovery

- #3232, run 45203418924, exact SHA `2c4b459a5555c5f3deebd041e8a1a45f451bba97`: in progress at the time of this report. The contract unit-test step had succeeded; the pinned model-contract test step was running. No result is claimed here.
- Predecessor #3231, run 45199087795, exact SHA `298cfd3b459ea789af4478972c31a7fbedfc8652`: overall failure because Maven Central returned HTTP 429 for `org.robolectric:nativeruntime-dist-compat:1.0.16` during `:app:testDebugUnitTest`. The Android instrumentation substeps did run four tests, but that does not turn the overall workflow green.

## Conclusion and next input required

The Android app entry points are sufficient to construct a genuine instrumentation test **once the exact source fixtures are supplied**. The task is blocked at the fixture gate, before any implementation. Required input: the original Legado BookSource JSON export containing all four sources, preferably as one unmodified JSON array or four original JSON files. If the sources contain private credentials, provide a sanitized export plus a secure test-only credential provisioning path; do not place secrets in the repository or CI logs.

After receiving the exact source descriptors, verify their source IDs and SHA-256, then write the instrumentation test to:
- import them through `BadcatReadingEngineFactory.sourceCatalog().importJson()`;
- invoke the factory-created production engine's `search`, `bookDetail`, `chapters`, and `chapterContent` methods;
- execute serially and record sanitized per-stage evidence;
- avoid Python scraping, mocked HTTP, hand-transcribed rules, or a parallel engine.

No code, test, workflow, source rule, UI, storage, timeout, or dependency change was made in this task. Do not claim any of the four sources passed the Android app runtime test.
