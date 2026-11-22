# Runtime-3.2f.11 — chapter URL alias normalization differential audit

**Status:** Audit complete; production implementation is NO-GO pending a chapter-base identity design.

- Badcat audit HEAD: `123f1e5476d5ffd26be54be317f46092fcf6f92d`
- Repository branch: `integration/legado-upstream-runtime`
- Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: read-only source/caller comparison. No code, tests, build files, workflow, persistence or UI changed by this audit.

## 1. Evidence and provenance

| Surface | Pinned path | Blob SHA | Relevant lines |
|---|---|---|---|
| Upstream chapter model | `app/src/main/java/io/legado/app/data/entities/BookChapter.kt` | `9798267c77c9e14d0a673a822930491cd6e44cc1` | 42–60, 176–187 |
| Upstream URL resolver | `app/src/main/java/io/legado/app/utils/NetworkUtils.kt` | `517f63c18998be2f57087486069df9f67281d642` | 161–190 |
| URL options parser | `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt` | `d898c88dfd844d73f06d9396d0b46f39939f7e6c` | 248–260, 816–820 |
| URL scheme helpers | `app/src/main/java/io/legado/app/utils/StringExtensions.kt` | `694fe6d8f3a815261e034c1341109aae1b18bf2c` | 38–46 |
| Data URL pattern | `app/src/main/java/io/legado/app/constant/AppPattern.kt` | `dc3244b873c6f913f72fd5cf1151814dd1dad2e1` | 19–21 |
| Batch URL index | `app/src/main/java/io/legado/app/model/BatchContentContext.kt` | `68cad877f048b171e4f0f6cf22b1e0dd1e6481f9` | 30–118 |
| Batch identity tests | `app/src/test/java/io/legado/app/model/BatchContentContextTest.kt` | `67207fecb4a96e8aae0a1bd7083d61cccbf657a9` | full test class |
| Host chapter lookup | `integration-contract/src/main/kotlin/app/badcat/engine/ChapterCatalogQuery.kt` | `fc06c05a8bf0d7b7fbac306f14fe51a944745c29` | 1–27 |
| Host chapter model | `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt` | inspected at audit HEAD | 161–172 |
| Host navigation callers | `app/src/main/java/app/badcat/client/SourceBookActivity.kt` | `15c0585b3a4f6dd44f49792563601caaa39158a0` | 231–247, 415–435, 780–840 |
| Host bookmarks | `app/src/main/java/app/badcat/client/BookmarkActivity.kt` | `ad301ad390e89e300d0fe4523b0abd1c230ce8bc` | 14–22, 111–121 |
| Host history | `app/src/main/java/app/badcat/client/ReadingHistoryActivity.kt` | `afdb5dbf6d04a11f8005610ee14375fbe0c0c9bb` | 92–102, 143–150 |
| Host reading state | `app/src/main/java/app/badcat/client/BadcatReadingStateRepository.kt` | `916465a757f4430bd17975b4986c352c07d6d413` | 25–61, 72–90, 92–116, 138–176 |
| Host lookup tests | `integration-contract/src/test/kotlin/app/badcat/engine/ChapterCatalogQueryTest.kt` | `b2f1da7e04a8fce0b727ce23c80d926840298c6f` | full test class |

The upstream helper source is part of the fixed SHA; the host evidence is read at the audit HEAD. Exact host line numbers may move in later commits.

## 2. Fixed upstream behavior

### 2.1 Chapter absolute URL

Pinned `BookChapter.getAbsoluteURL()` has two branches:

1. If `isVolume` is true and `url.startsWith(title)`, it returns the chapter's `baseUrl` directly.
2. Otherwise it finds the first `AnalyzeUrl.paramPattern` match in the chapter URL. The pattern is `\\s*,\\s*(?=\\{)`: an optional-whitespace comma immediately followed by a JSON object opener. It resolves only the URL prefix against the chapter's `baseUrl`, then reattaches the original comma/options suffix.

The chapter model stores `baseUrl` per chapter. It is not necessarily the same value as the book's original TOC URL, especially when chapters were parsed from subsequent TOC pages or redirects.

### 2.2 NetworkUtils.getAbsoluteURL

Pinned source establishes these branches:

- Null or empty base URL: return `relativePath.trim()`.
- String base overload: attempt `URL(baseURL.substringBefore(","))`; on parse failure, the URL base remains null and the overload falls back to the trimmed path.
- Null URL base: return trimmed path.
- Absolute HTTP(S): return trimmed path unchanged. The upstream `isAbsUrl` check is case-insensitive and recognizes only `http://` / `https://`.
- Data URL: return trimmed path unchanged when it matches `^data:.*?;base64,(.*)`.
- A string starting with lowercase `javascript`: return the empty string.
- Otherwise call Java `URL(baseURL, relativePath)`; if that throws, return the initially trimmed path.

A source-level detail matters: the resolver computes `relativePathTrim`, but the successful Java URL constructor receives the original `relativePath`, not the trimmed variable. Therefore leading whitespace can take a different path from the simple “trim then resolve” mental model. This should be locked by a pinned differential fixture before reproducing it.

### 2.3 URL option parsing

`AnalyzeUrl.analyzeUrl()` strips the URL/options suffix before resolving the request URL and separately parses the suffix as `UrlOption`. The same `paramPattern` is referenced by `BookChapter.getAbsoluteURL()`. The suffix is therefore not an ordinary path/query component and must not be silently dropped or normalized as part of the URL prefix.

### 2.4 BatchContentContext alias index and ambiguity

At the pinned SHA:

- Batch base URL is `book.tocUrl.ifBlank { bookSource.bookSourceUrl }`.
- For each chapter, the context indexes three aliases: raw `chapter.url`, `chapter.getAbsoluteURL()`, and `normalize(chapter.url)`.
- `normalize` calls `NetworkUtils.getAbsoluteURL(batchBaseUrl, url)`.
- Every alias maps to a list of candidate chapters. Candidate de-duplication is by chapter index, not URL.
- A URL callback is trimmed; direct raw matches and matches under the normalized callback are combined, de-duplicated by chapter index and accepted only if exactly one candidate remains.
- A real `BookChapter` object resolves against the current batch by index. Numeric values and null are rejected.
- Saving one duplicate-URL chapter does not make the URL unique. The ambiguity remains until the source supplies a chapter object.

The two upstream bases are intentionally distinct in the implementation: `BookChapter.getAbsoluteURL()` uses per-chapter `chapter.baseUrl`, while the batch normalization helper uses the book TOC URL (falling back to the source URL). A faithful port must preserve both paths rather than collapse them into one assumed base.

## 3. Badcat behavior and differences

### 3.1 ChapterCatalogQuery

Current host behavior is narrower:

- `findByUrl` accepts a nonblank input but compares it exactly against `chapter.url`; it does not trim the input or resolve relative/absolute aliases.
- `findByIdentity` uses a nonnegative index as primary identity, then checks the supplied URL with exact equality. When index is absent/negative, it falls back to the same exact URL lookup.
- Neither method accepts a base URL.
- Host `BookChapter` currently projects `index/title/url/isVip/isPay/tag`; it has no `baseUrl` or `isVolume` field, so it cannot reproduce the upstream chapter-specific URL method as written.

### 3.2 Direct navigation and persistence callers

Directly confirmed callsites in the inspected host sources:

- `SourceBookActivity.beginReadingOrQueue`: resolves last-read URL before opening a chapter.
- `SourceBookActivity.showChapters`: the “continue reading” button resolves a saved URL.
- `SourceChapterActivity.loadInitial` (same Activity file): a URL-only legacy intent waits for the current TOC and resolves the URL before requesting content; indexed intents use index plus exact URL validation.
- `BookmarkActivity` and `ReadingHistoryActivity`: launch URL/title-only intents; neither supplies chapter index.
- `BadcatReadingStateRepository`: last-read progress stores a chapter URL; bookmark/history records store source ID, book URL, chapter URL and title, but not chapter index or the TOC page base URL.

The repository code-search endpoint returned no usable complete index, so this report records direct source callsites only and does not claim a compiler-generated or exhaustive whole-repository call graph.

### 3.3 Existing safety behavior

The host already fails closed on ambiguous URL-only chapter navigation rather than choosing the first match. The indexed reader path preserves the chapter index and checks the URL. That safety behavior must remain: normalization must not turn multiple chapters sharing one canonical alias into a guessed selection.

The host TOC parser currently resolves chapter links to absolute HTTP(S) URLs using its URL policy, but it does not retain each chapter's source page `response.finalUrl` as a chapter-level base field. This is an information-loss point for reproducing relative aliases from a multi-page/redirected TOC.

## 4. Differential test matrix required before implementation

| Case | Pinned upstream behavior to assert | Host question / required evidence |
|---|---|---|
| Relative path | Java URL resolution against the exact chapter base | Preserve the base of the page that emitted the chapter |
| Absolute HTTP(S) | Preserve trimmed absolute URL | Match case-insensitive scheme behavior without weakening transport validation |
| Protocol-relative | Java URL constructor behavior with the base scheme | Capture pinned JVM result; do not assume URI behavior |
| Dot segments | Java URL resolution result | Compare exact string output |
| Query-only / fragment-only | Java URL resolution against base | Preserve query/fragment semantics |
| Comma + JSON options | Strip prefix, resolve it, reattach original suffix | Do not treat options as query text or drop them |
| Whitespace around comma | Exact regex boundary and suffix preservation | Test leading/trailing spaces and spaces before JSON |
| Invalid base | Null-base fallback to trimmed relative path | Do not invent a base |
| Data URL | Preserve only the pinned base64-pattern match | Keep matching separate from HTTP transport policy |
| Lowercase javascript prefix | Returns empty string in NetworkUtils | Do not evaluate it during chapter navigation |
| Volume chapter | Return chapter base URL under the upstream predicate | Host projection currently lacks volume/base fields |
| Relative/absolute aliases | Batch context indexes all supported aliases | Must preserve per-chapter base and batch fallback base separately |
| Alias collision | Candidate list resolves only if exactly one chapter index | Multiple candidates remain ambiguous even after one is saved |
| Chapter object | Resolve by chapter index | Preserve typed chapter identity |
| Numeric/null/unknown | Reject | Never infer array position from numeric identifier |

## 5. Design constraints and decision

A simple overload such as `findByUrl(chapters, savedUrl, bookTocUrl)` is not yet sufficient to prove parity. The upstream chapter method depends on a per-chapter base URL and volume metadata; the batch alias map also has a separate book-level fallback base. The host currently discards per-page final URL metadata after producing absolute chapter URLs.

A later implementation design must decide whether to:

1. Add a defaulted chapter `baseUrl`/required source-page metadata to the host chapter projection and preserve it from every TOC page response; or
2. Provide an explicit typed alias-index object built during TOC parsing that retains raw URL, resolved absolute URL and chapter-specific base without changing persisted reader identity.

The design must also preserve legacy URL-only bookmark/history data as aliases, fail closed on duplicate canonical matches, and avoid silently upgrading old records by guessing an index from title or display order. The URL matching helper must never authorize a network request or bypass `WebUrlResolver` credential-free HTTP(S) checks.

**Decision:**

- **GO** for a separately preregistered chapter-base metadata / pure-JVM differential-fixture design.
- **NO-GO** for a production `ChapterCatalogQuery` normalization patch or any claim of upstream alias parity before the per-chapter base and legacy URL-only identity questions are solved.
- The existing fail-closed behavior remains preferable to an incorrect chapter selection.

## 6. Audit closeout

- Read-only pinned-source and host-source inspection completed.
- No build, tests, CI, APK, device or live-source test was run for this audit.
- No production source, test, Gradle/workflow, persistence, UI/Reader or `main` was modified.
- This audit does not implement or close `BatchContentContext`; both upstream JS and rule batch-content paths remain unintegrated in Badcat.
