# Legado runtime porting map

Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

This document records a source-level dependency review. It is not a claim that the runtime has been ported or that the host app builds.

## Execution path verified at the pinned revision

| User operation | Upstream entry point | Direct responsibilities | Important dependencies visible in source |
|---|---|---|---|
| Search / explore | `app/src/main/java/io/legado/app/model/webBook/BookList.kt` | Fetch-response parsing, list selection, per-item extraction, optional detail-page fallback, de-duplication and ordering | `BookSource`, `Book`, `SearchBook`, rule entities, `AnalyzeRule`, `AnalyzeUrl`, `NetworkUtils`, Gson helpers, Android resources/application context, coroutines |
| Book detail | `.../model/webBook/BookInfo.kt` | Name/author/kind/count/latest/intro/cover/TOC or download URL extraction | `Book`, `BookSource`, `BookInfoRule`, `AnalyzeRule`, `HtmlFormatter`, `NetworkUtils`, Android resources/context, coroutines |
| TOC / chapter list | `.../model/webBook/BookChapterList.kt` | Chapter extraction, pagination, duplicate/order handling and chapter metadata updates | `Book`, `BookChapter`, `BookSource`, `TocRule`, `AnalyzeRule`, `AnalyzeUrl`, Room database access, content processing, Rhino script bindings, coroutines |
| Chapter content | `.../model/webBook/BookContent.kt` | Fetch and parse chapter response, including source-specific content rules | `Book`, `BookChapter`, `BookSource`, `ContentRule`, `AnalyzeRule`, `AnalyzeUrl`, replacement/content processing and script runtime |
| Rule evaluation | `.../model/analyzeRule/AnalyzeRule.kt` | Dispatches rule modes and evaluates CSS/JS/JSON/XPath and source-rule chains | Rule parser implementations, HTML/JSON/XML parsers, JS runtime, rule data bindings and coroutine context |
| Request / URL evaluation | `.../model/analyzeRule/AnalyzeUrl.kt` | URL interpolation, headers, cookies, redirects, request options and response retrieval | OkHttp/network helpers, cookie store, proxy/client configuration, JS extensions/Rhino, source state, coroutine context |

The table describes observed source dependencies, not a complete transitive dependency graph.

## Important model boundary

The upstream `data/entities/BookSource.kt` is not just three source metadata fields. It carries source URL/name/group/type, request headers and login state, cookie behavior, search/explore/detail/TOC/content rule objects, JavaScript library and main script, event/custom-button flags, and helper methods that supply default rule objects.

The host-side `BookSource` in `integration-contract` currently stores only basic metadata plus the original JSON string. This is a safe import/round-trip staging representation, but it is not yet a runtime-compatible upstream model. Do not pass it directly to upstream parsers.

## Porting order and acceptance gates

1. **Model and serialization layer** — vendor/adapt the exact pinned source and rule entity models, including nested rule classes and converters. Verify import/export fixtures preserve all fields.
2. **Rule evaluation layer** — bring in the parser implementations and script bindings with the pinned Rhino runtime. Add deterministic CSS/JSON/XPath/JS fixtures and cancellation/error tests.
3. **Request layer** — isolate the HTTP client, headers, cookies, redirects, proxy and URL-option behavior behind an injectable host service. Tests must use a local/mock transport; no production source site is required.
4. **Operation layer** — adapt search, detail, TOC and content as a coherent vertical slice. Keep upstream behavior and replacement-rule ordering; do not reimplement just a subset while claiming general Legado compatibility.
5. **Persistence and initialization** — decide which Room tables, migrations, cache/state services and application initialization are required. Provide explicit host-owned equivalents rather than relying on Legado's global `appCtx` or `appDb`.
6. **Badcat UI adapter** — connect the existing Badcat navigation and reader/shelf models only after the vertical slice passes fixture tests. Preserve original screen structure and interactions.
7. **License and notices** — keep the exact upstream revision, GPL-3.0 text, modification notices and corresponding source availability for distributed combined/derivative work.

## Explicit non-goals

- The source JSON codec alone is not a search engine.
- `modules/book` and `modules/rhino` alone are not the full online source pipeline.
- A Legado HTTP API or ContentProvider bridge would require a separately installed Legado app and would not meet the self-contained-app goal.
- This map does not authorize bypassing APK signing, native protection or access controls.

## Current status

- Source-level dependency review: documented from the pinned upstream files.
- Host JSON import/export: present as staging code.
- Runtime-compatible upstream models, parser/JS execution, HTTP adapter, persistence and operation pipeline: not implemented.
- Original Badcat UI migration and installable APK: not completed.
- No build or test pass is claimed by this document.
