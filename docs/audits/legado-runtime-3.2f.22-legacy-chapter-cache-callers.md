# Runtime-3.2f.22 — legacy URL-keyed chapter-cache caller and invalidation audit

Date: 2026-11-23  
Category: Third — Legado runtime  
Audit HEAD: `63ed3919778a0e7bf75a90d50e7abd25b547f553`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Scope and evidence

Read-only audit of the current host chapter-cache interface, its production implementation and wiring, service call path, cache tests, and source lifecycle integration points. No production or test code changed. GitHub code-search index returned no matches for this repository; therefore this report makes claims only for directly inspected source files and the complete recursive tree's cache-related paths. It does not claim a mathematically exhaustive call graph for every Kotlin expression.

## Cache API and storage identity

| Surface | Blob SHA | Finding |
|---|---|---|
| `ChapterContentCache.kt` | `95135a2cdd2b79ee0994b328012f25000ba07ad5` | Interface exposes two legacy URL-keyed overloads (`get/put(sourceId, bookUrl, chapterUrl)`) and four index-keyed/fenced operations (`beginSave/get/put/invalidate`). Comment calls legacy methods “migration/debug compatibility”. |
| `FileChapterContentCache.kt` | `585b3fb96b70d2d84536d507dd67112b5a0d312c` | Legacy file identity is `SHA256(lengthPrefix(sourceId,bookUrl,"url:"+chapterUrl))`; index identity is `SHA256(lengthPrefix(sourceId,bookUrl,"index:"+chapterIndex))`. Both serialize to separate `.json` files in `filesDir/legado_chapter_cache`. |
| `SourceChapterContentService.kt` | `8e2da87c300784fe4a0ca96e161c9597590e6e0f` | Production service only calls the index-keyed `beginSave`, `get(sourceId,bookUrl,chapter)`, `isCurrent`, and fenced `put`; it never calls the legacy URL overloads. |
| `BadcatReadingEngineFactory.kt` | `cd8946aaaffacef3d2a748a537736cf2798c96c8` | Each engine construction creates a new `FileChapterContentCache(context.applicationContext)`; all instances share the same app-private directory, process-wide file mutex and process-wide save fence. |
| `FileChapterContentCacheTest.kt` | `03d92206fe7bfcdbac02ab50041edf09281b633e` | Tests digest separation and bounded-reader behavior only; does not instantiate Android AtomicFile cache or verify actual legacy/index file reads, writes, invalidation or source-wide deletion. |
| `SourceChapterContentServiceTest.kt` | `a8592ea9c645c28e25240169282c6eab146982ca` | Cache behavior is tested with fake implementations; confirms service's index/fence usage, not the Android file implementation. |

## Confirmed call path and invalidation behavior

1. `BadcatReadingEngineFactory.create()` constructs the source content service with a `FileChapterContentCache`.
2. `SourceChapterContentService.load()` captures an index-based save token before network work, reads the index-based cache entry, checks TTL/current generation, and writes successful content through the fenced index overload.
3. The legacy URL-keyed `get/put` methods remain implemented but are not called by the inspected production service or factory path. No active migration/fallback from URL-keyed file to index-keyed file is present in these inspected paths.
4. `FileChapterContentCache.invalidate(sourceId,bookUrl,chapterIndex)` invalidates the process-wide save fence and deletes only the file named by the `index:<chapterIndex>` digest. It has no chapter URL parameter and does not enumerate/delete the `url:<chapterUrl>` digest.
5. Legacy and index files are distinct names. Thus, if a legacy URL-keyed file exists from an older caller/version, index invalidation cannot delete it. It becomes an orphan from the active service's perspective; current bounded LRU pruning may eventually remove it only when a later put triggers pruning and the global 64 MiB budget requires eviction. There is no explicit legacy TTL sweep or migration cleanup in the inspected implementation.
6. Source-catalog removal/replacement lifecycle work (.15) clears Rhino source state (JS/KV/file/session/Cookie) after catalog mutation; the chapter cache lives in a separate `legado_chapter_cache` directory and is not included in that source-state cleanup. The inspected cache interface has no source-wide clear operation. This report does not infer that .15 clears chapter content files.

## Risks / open questions

- Legacy API callsites outside the directly inspected production paths remain unproven because repository code search was unavailable; the recursive tree shows one production implementation and the source content service as its main typed boundary, but that is not a complete expression-level caller proof.
- Existing legacy URL-keyed files may remain until LRU pressure; no measured inventory of user devices' cache directory was available.
- The interface has no `invalidateLegacy(sourceId,bookUrl,chapterUrl)`, no source-wide clear, and no migration marker. Adding those semantics without a caller/ownership plan could cause broad deletion or stale data loss.
- Cache TTL is enforced by `SourceChapterContentService` for indexed reads; legacy `get` itself returns a parsed entry without TTL enforcement. Since no active production caller was found, this is not currently on the main service path, but any future legacy caller would need explicit TTL policy.

## Decision

**GO** for a separately registered, narrowly scoped follow-up that first proves all legacy overload callers with a complete branch-aware scan, then either removes the unused legacy API and defines a bounded orphan cleanup strategy or explicitly implements migration with tests.  
**NO-GO** for changing the cache schema, deleting all URL-keyed files, or claiming source-removal invalidation is complete based only on this audit. A source-wide chapter-cache purge needs its own typed API, ownership decision and tests; it is not equivalent to Rhino `clearSourceState`.

## Evidence boundary

Static source audit only; no tests, CI, APK, Android instrumentation, device filesystem inspection or migration execution was run. This does not close chapter cache lifecycle parity, BatchContentContext, or full third-category compatibility.
