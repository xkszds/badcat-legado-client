# Runtime-3.2f.20 — chapter-specific baseUrl propagation audit

Date: 2026-11-23  
Category: Third — Legado runtime  
Host branch / audit HEAD: `integration/legado-upstream-runtime` / `22f7d358369d244f0db47c0922f368ecc822a24e`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Scope and method

Read-only source audit. No production, test, Gradle, workflow, persistence, or UI files were changed. The audit follows the pinned upstream chapter model and URL helper, BatchContentContext, and the current Badcat TOC/parser/catalog/reader/history/bookmark path. It does not claim a complete repository call graph beyond the directly inspected callers.

## Pinned upstream behavior

| Source | Blob SHA | Verified behavior |
|---|---|---|
| `BookChapter.kt` | `9798267c77c9e14d0a673a822930491cd6e44cc1` | Model contains `url`, `baseUrl`, `bookUrl`, `index`, volume/VIP/pay/tag and other metadata. `getAbsoluteURL()` returns `baseUrl` for a volume row whose URL starts with its title; otherwise splits the URL's comma-options suffix using `AnalyzeUrl.paramPattern`, resolves only the URL prefix against the chapter's own `baseUrl`, then reattaches the suffix. |
| `NetworkUtils.kt` | `517f63c18998be2f57087486069df9f67281d642` | String-base overload trims and returns the relative string if base is null/empty; otherwise parses the base before the first comma. URL overload trims the reference, returns absolute/data URLs unchanged, maps javascript URLs to empty, and uses `java.net.URL(base, relative)`; parse failures return the trimmed relative text. |
| `AnalyzeUrl.kt` | `d898c88dfd844d73f06d9396d0b46f39939f7e6c` | Defines the comma-options delimiter used by `BookChapter.getAbsoluteURL`; it is not equivalent to treating the whole comma suffix as a path component. |
| `BatchContentContext.kt` | `68cad877f048b171e4f0f6cf22b1e0dd1e6481f9` | For each chapter, indexes raw `chapter.url`, `chapter.getAbsoluteURL()`, and `normalize(chapter.url)`. The latter uses `book.tocUrl.ifBlank { bookSource.bookSourceUrl }` as a book-level base, distinct from the chapter-specific base used by `getAbsoluteURL()`. Resolution trims the supplied URL, unions direct and normalized candidates, de-duplicates by chapter index, and accepts only a single candidate. A chapter object resolves by index; pure numeric identifiers are rejected. Save operations use per-index ContentSaveToken and retry missing chapters through the normal path. |

Important distinction: upstream has two separate bases in the batch URL index: chapter-specific `baseUrl` for the chapter absolute URL and book-level TOC/source base for `normalize(chapter.url)`. Do not collapse them into one generic base field or one URL canonicalization pass.

## Current Badcat behavior

| Host source | Blob SHA | Verified behavior / gap |
|---|---|---|
| `ReadingEngine.kt` | `d37c8f5e7c8994295c7cc1e651f4e1245ddc242b` | Host `BookChapter` currently contains only index/title/url/isVip/isPay/tag; it has no chapter-specific base URL, volume flag, or complete upstream chapter metadata. Its chapter-object content overload defaults to URL-only legacy overload unless a concrete adapter overrides it. |
| `SourceTocService.kt` | `0904d635be19ffd5de5ca56a5f06b258ed919d0b` | Each page response retains requested URL and final redirect URL for cycle detection; parser receives the full response. The service merges chapter projections and reindexes them, but does not retain per-page final URL on the chapter projection. |
| `CssTocParser.kt` | `4e30082fb654719ffe7cf1605dba91d3898a1f9f` | Uses `response.finalUrl` as the document base for rule evaluation and resolves each raw chapter URL with `WebUrlResolver.resolve(response.finalUrl, rawUrl)`; constructs `BookChapter(index,title,url,isVip,isPay,tag)`. Thus the current URL is materialized as an absolute HTTP(S) URL, but the page/chapter base is discarded. |
| `WebUrlResolver.kt` | `d0352685f0a33d1810c3e26331621a89555dd8f2` | Enforces credential-free HTTP(S), preserves Legado template placeholders and comma options suffix, normalizes URI references, and has a query-only pagination special case. This is intentionally stricter/different from upstream `NetworkUtils` fallback behavior; do not replace it with permissive `java.net.URL` resolution. |
| `ChapterCatalogQuery.kt` | `fc06c05a8bf0d7b7fbac306f14fe51a944745c29` | URL lookup is exact string equality and accepts only one match. Index lookup validates the supplied URL as an exact alias; no raw/absolute/normalized candidate map exists. Existing tests prove duplicate URL ambiguity fails closed. |
| `BadcatReadingStateRepository.kt` | `916465a757f4430bd17975b4986c352c07d6d413` | Last chapter, scroll, bookmark and history persistence keys are URL-based; saved records do not include chapter index or chapter base URL. |
| `SourceBookActivity.kt` | `15c0585b3a4f6dd44f49792563601caaa39158a0` | TOC row navigation carries chapter index, URL and title. Reader startup with a legacy URL-only intent reloads the TOC and resolves by exact URL; ambiguity/missing URL fails closed. Bookmark/history activities still create URL/title-only intents. |

### Data-flow finding

1. TOC HTTP response exposes `finalUrl`.
2. `SourceTocService` passes that response to `CssTocParser`.
3. Parser uses `finalUrl` to resolve relative chapter links into absolute, credential-free HTTP(S) URLs.
4. Parser stores only the resolved URL and current metadata in the host projection; it discards the chapter's source page base.
5. The merged TOC retains absolute URLs but cannot reconstruct which page/final redirect URL supplied a chapter, particularly where different pages resolve relative links under different final URLs.
6. Reader TOC navigation can carry index, but legacy bookmarks/history and durable progress remain URL-only. Catalog lookup is exact, with ambiguity rejection.

The current parser's eager absolute URL resolution prevents many ordinary relative-link failures during initial TOC parsing. It does not establish upstream parity for raw URL aliases, chapter-specific base URL, comma-options handling across all branches, or BatchContentContext candidate matching.

## Differential risks and test matrix

Required fixtures for a future implementation:

- first TOC page and later page each return a different redirect final URL; relative chapter URLs must resolve against the page that produced them;
- root-relative, path-relative, protocol-relative, absolute, query-only and fragment-bearing references;
- dot segments and percent escapes;
- comma options suffix with whitespace and JSON payload; ensure suffix is not resolved as path;
- chapter URL templates `{{key}}` / `{{page}}` remain intact until request context is available;
- volume rows and javascript/data references: preserve explicit upstream behavior only where safe; never weaken Badcat HTTP(S)/credential restrictions;
- raw URL, chapter absolute URL, and book-level normalized URL each resolve to the expected candidate set;
- two chapters with the same raw URL but different indexes/bases; alias collision must remain ambiguous for URL-only callers;
- object/index identity must resolve exactly; numeric string must not be confused with a chapter index;
- old URL-only bookmark/history missing, unique, stale and ambiguous cases all fail closed unless exactly one TOC row matches.

## Recommendation / decision

**GO, limited to preserving chapter-specific page-base metadata and adding differential fixtures.** The smallest safe next slice is to append an optional `baseUrl` field to the host chapter projection and populate it from `response.finalUrl` when parsing each TOC page, while keeping the existing resolved `url` and strict `WebUrlResolver` behavior. Tests must cover multi-page redirect bases and ensure duplicate chapter preservation/reindexing remains stable.

**NO-GO** for implementing URL alias lookup in the same slice, switching to upstream `NetworkUtils` wholesale, changing URL-only history/bookmark persistence, or claiming BatchContentContext parity. A later separately registered task must define candidate union/ambiguity semantics and migrate callers only after the base metadata is retained. Actual production BatchContentContext is not wired: the current host still uses its own projection/service path and lacks the complete upstream Book/BookSource/ContentSaveToken runtime closure.

## Evidence boundary

This is a static source audit only. No tests, build, CI, live source, emulator or device validation was run for this audit. It does not close the upstream model/runtime, data ownership, cache, or full compatibility gates.
