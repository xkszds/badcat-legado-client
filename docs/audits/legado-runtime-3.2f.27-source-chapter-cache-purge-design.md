# Runtime-3.2f.27 — source-scoped chapter-body cache purge design audit

Date: 2026-11-23  
Branch / audit HEAD: `integration/legado-upstream-runtime` / `e0a1f0b62548361ffcabba98944da295441ccb3e`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Evidence inspected

Host exact blobs at audit HEAD:
- `FileChapterContentCache.kt`: `585b3fb96b70d2d84536d507dd67112b5a0d312c`
- `CacheFilePruner.kt`: `e7931b71ec194d6417c03b0f641e1c891395df78`
- `BadcatReadingEngineFactory.kt`: `cd8946aaaffacef3d2a748a537736cf2798c96c8`
- `FileBookSourceCatalog.kt`: `1f3c9cd879e58f533f57352e3074c667c06c664b`
- `SourceCatalogActivity.kt`: `20ca409aa192ce4e2f7a7d6e41de9a8fac059e6b`
- `RhinoSourceScriptRuntime.kt`: `879d6ed978894b16271cf5207348176fd040eda7`
- `ChapterContentCache.kt`: `95135a2cdd2b79ee0994b328012f25000ba07ad5`
- `ChapterContentSaveFence.kt`: `88fe8e5bdf8ee2ad685376d4a0d1d724c383f9ab`
- `FileChapterContentCacheAtomicFileTest.kt`: `8179bd26b9808c7397311454b4d83b46febb2ca0`

## Current layout and ownership

- Cache directory is app-private `filesDir/legado_chapter_cache`.
- Every base entry is a flat `<sha256>.json` file. The digest input is the concatenation of length-prefixed `sourceId`, `bookUrl`, and identity; identity is either `url:<chapterUrl>` or `index:<chapterIndex>`.
- The filename is one-way SHA-256; it cannot be reversed to recover sourceId/bookUrl/chapter identity.
- Legacy URL JSON fields are `title/text/nextPageUrl/previousChapterUrl/savedAt`; there is no sourceId, bookUrl, chapterUrl or chapterIndex in the file body. Index JSON additionally stores chapterUrl, but still no sourceId/bookUrl.
- Therefore neither filename nor existing JSON body can safely identify all files belonging to one source. Searching JSON for sourceId is impossible; guessing by URL host or book URL is unsafe.
- AtomicFile owns recovery sidecars (e.g. backup files) alongside base files. Current pruner enumerates only `.json` base files, so a purge design must account for base and AtomicFile sidecar lifecycle without deleting another source's files.
- All FileChapterContentCache instances share one process-wide coroutine Mutex for file IO/pruning, but the source save fence is a separate shared process-wide fence in the current descendant implementation. The source lifecycle callback in FileBookSourceCatalog runs after the catalog AtomicFile commit; SourceCatalogActivity currently calls `RhinoSourceScriptRuntime.clearSourceState(sourceId)`, which cleans Rhino/KV/file/session/Cookie domains but not chapter body cache.
- `invalidate(sourceId,bookUrl,index)` advances the shared save fence and deletes only one index-keyed file. It cannot find URL-keyed legacy files or all books for a source.

## Safe design conclusion

**NO-GO for source-wide purge against the existing flat digest-only files.** There is no safe way to identify source-owned legacy and index entries without extra ownership metadata. Do not iterate and delete the whole directory; do not infer source from URL hostname; do not derive index from URL; do not delete all entries for a book by guessing.

A forward-compatible implementation needs a durable ownership index/manifest, or a new source-partitioned directory layout. Preferred migration direction for a separately registered implementation:
1. Keep the current flat digest format readable during migration.
2. For every new write, atomically commit the content entry and an ownership record mapping opaque digest filename to sourceId/bookUrl/namespace/version. The manifest itself needs AtomicFile or equivalent crash-safe updates and a process-wide lock shared with put/prune/purge.
3. Define crash ordering/reconciliation: content written but manifest missing must become a safe orphan eligible only for global LRU expiry, never source purge; manifest pointing to missing content is pruned; stale backup sidecars must be reconciled by AtomicFile open/read or explicit safe cleanup.
4. Purge only files whose verified manifest owner equals sourceId; advance source/book save-fence generations before deletion so in-flight old requests cannot republish; serialize with cache writes and pruning. Report partial cleanup failures after catalog commit, matching existing lifecycle callback semantics.
5. Legacy pre-manifest files remain unowned and must not be deleted by source purge. They can age out through existing global LRU or be migrated only when a verified read provides sourceId/bookUrl and exact namespace identity. Do not silently claim the old cache is fully purged.
6. Bound manifest size and recovery cost; prevent path traversal by storing only validated digest filenames, never caller-supplied paths.

An alternative source-partitioned layout would require a versioned directory migration and dual-read policy. It has the same unresolved legacy ownership problem, so it is not a shortcut.

## Minimal follow-up implementation scope (requires new preregistration)

Likely files: `FileChapterContentCache.kt`, `ChapterContentCache.kt`, `ChapterContentSaveFence.kt`, catalog lifecycle wiring in `FileBookSourceCatalog.kt` / `SourceCatalogActivity.kt`, focused production AtomicFile tests, manifest codec/manager and migration tests. The exact list must be established by caller review before implementation. Do not extend .26's test-only scope implicitly.

## Decision and limits

- GO: implement a versioned ownership manifest for newly written entries, crash-safe reconciliation, and source-scoped purge for entries with verified ownership, under a separately preregistered task.
- NO-GO: promise deletion of pre-manifest legacy entries; delete the entire directory; change current digest/schema in place; or close source purge before process-restart and concurrent save/purge tests pass.
- This was a read-only static audit. No production/test/build/CI/device code was run. It does not prove complete all-caller closure and does not change .15/.16/.22/.26 outcomes.
