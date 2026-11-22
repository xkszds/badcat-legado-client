# Runtime-3.2f.12 — cache mutation callers and invalidation closure audit

**Status:** Audit complete; chapter-content cache parity and generic source-cache lifecycle parity remain NOT MET.

- Badcat audit HEAD: 136819704e6fd508e2889195d5ae0f591c6dacc3
- Branch: integration/legado-upstream-runtime
- Pinned upstream: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a
- Task type: read-only source and caller audit. No production code or tests were changed.

## 1. Upstream generic CacheManager

Pinned CacheManager is not a single in-memory map.

- Room Cache entity is stored in the caches table with a unique key and deadline.
- CacheDao provides get/insert/delete, runtime source cache export, source-variable deletion and expired-deadline cleanup.
- CacheManager.put(key,value,saveTime) interprets saveTime in seconds. A zero deadline is permanent. A timed write persists the row and deletes the same key from memory; only permanent string entries are promoted into memory on read.
- get(key, onlyDisk=true) bypasses memory. delete(key) deletes the Room row, memory value and ACache file for the same key.
- putFile/getFile/getByteArray use ACache. ACache has its own filesystem LRU and expiry metadata.
- App startup invokes cacheDao.clearDeadline(now); expired DB rows are cleaned there. A normal expired read returns null but does not itself delete the row.
- WebCacheManager is a separate WebView-facing wrapper that delegates to CacheManager.

The upstream CacheManagerTtlContractTest is a source-contract test: it verifies seconds conversion, invalidating memory after timed writes, promoting only permanent entries, disk-only read behavior and synchronized persistent operations. It is not an Android Room/ACache device integration test.

### Production caller inventory

Pinned GitHub code search for CacheManager returned 43 matching files with incomplete_results=false. Separate searches returned 11 delContent files, 17 saveText files, 9 reverseContent files and 2 commitResources files. This is symbol search evidence, not a compiler-generated call graph.

- Source runtime/source state: BaseSource stores login headers, login info, source variables and arbitrary source values under keys derived from getKey(); it exposes CacheManager as the JS cache binding. JsSourceEngine, AnalyzeRule and AnalyzeUrl also bind CacheManager into script/rule contexts.
- Other generic persistent/memory users: InfoMap, CheckSource, BookCover, AutoTask, RssArticlesAdapter3, BookController, VideoPlay, CodeEditViewModel, TextDialog, HttpHelper, CookieStore, CookieManager, BackstageWebView and WebJsExtensions.
- WebView cache surface: WebCacheManager is registered by BackstageWebView, WebViewActivity, VideoPlayerActivity, ReadRssActivity, BookInfoActivity and BottomWebViewDialog. This is not the same registration path as Rhino's cache object.
- Adjacent but separate: AppCacheManager source-variable globals and AudioCacheManager/media caches are separate objects. They are not evidence that generic CacheManager or chapter-content cache has been ported.

SourceHelp.deleteBookSourceInternal calls CacheDao.deleteSourceVariables(key) and AppCacheManager.clearSourceVariables(). The DAO predicate removes v_<source>_* and sourceVariable_<source>; it does not match the loginHeader_<source> or userInfo_<source> prefixes visible in BaseSource. Those other keys have explicit per-key remove methods, but are not covered by this source-variable cleanup query.

Runtime source cache backup is opt-in and exports only the DAO's recognized runtime cache keys. Restore validates the prefix allowlist and merges those rows; it is not a full CacheManager snapshot.

## 2. Upstream BookHelp chapter content mutation lifecycle

Pinned BookHelp.ContentSaveFence maintains versioned state by ContentSaveKey(bookUrl, chapterIndex). The token also carries the book folder name. Its state and write/replace operations are synchronized.

- saveContent validates book URL, chapter index and folder identity, then publishes only if the captured version is still current.
- saveText, reverseContent, resource replacement/commit and delContent advance the generation before mutating the chapter file. A failed replacement still advances the version, preventing an older in-flight request from publishing over it.
- readContent uses the fence when materializing or repairing EPUB cache content.
- BatchContentContext captures a token per chapter index, rejects ambiguous URL callbacks, and calls BookHelp.saveContent under its batch lock.
- ReadBookViewModel deletes chapter content and then invalidates the independent CacheBook downloader state. Manual edit, reverse-content and resource refresh have their own callers.
- BookHelp.clearCache(book) deletes the book cache directory but does not itself advance every chapter's ContentSaveFence generation in the inspected implementation. This is a potential refresh-vs-in-flight-write edge that needs a focused upstream test before claiming stronger semantics.

## 3. Badcat chapter-content cache lifecycle

### Current implementation

- ChapterContentCacheKey is (sourceId, bookUrl, chapterIndex); the indexed file stores chapterUrl as a guard against reusing the same index for another URL.
- SourceChapterContentService.load captures a save token before network work, reads the cache, checks freshness using a default 12-hour TTL, fetches and parses all content pages, applies scoped text replacement, then writes the final replaced content using the captured token.
- A network failure can fall back to a cached value while its token remains current, even when the value is outside the fresh TTL window. This is an explicit offline fallback, distinct from a fresh cache hit.
- FileChapterContentCache uses AtomicFile, a process-wide Mutex for file I/O and a 64 MiB LRU prune. Reads update lastModified. Expired files are ignored by the service but are not immediately deleted; they remain until pruning.
- Legacy URL-key get/put and index-key fenced get/put use different digest namespaces. invalidate(sourceId,bookUrl,index) deletes only the index-key entry; it does not delete any old URL-key file.

### Proven invalidation and concurrency gaps

1. **Fence scope is per instance.** FileChapterContentCache creates ChapterContentSaveFence() as an instance field, while BadcatReadingEngineFactory.create() constructs a new FileChapterContentCache each time. File operations share a top-level mutex, but save generations do not. A request started by cache instance A is not invalidated by an invalidate/replace performed through instance B.
2. **Fence map is not thread-safe.** ChapterContentSaveFence uses a plain HashMap without synchronization. beginSave and isCurrent are not protected by the file mutex; only file operations and the indexed put/invalidate critical sections hold that mutex. The current fence tests are serial and do not cover this race.
3. **No production invalidation caller was confirmed.** ChapterContentCache.invalidate exists, but SourceChapterContentService only calls beginSave/get/isCurrent/put. The inspected source catalog remove/import paths do not clear the chapter cache. Source removal can therefore leave files under the old sourceId; reusing the same sourceId/bookUrl/index/URL can make old content eligible for reuse.
4. **Rule changes do not participate in the cache key.** The service caches content after scoped replacement. The key has sourceId/bookUrl/index but no replacement-rule revision, so changing a replacement rule can leave already-replaced content visible until cache expiry or an explicit invalidation.
5. **Refresh/edit lifecycle is incomplete.** The host service has no counterpart for upstream BookHelp's saveText/reverseContent/commitResources/delContent mutation family or the BatchContentContext batch save-token pipeline. No TOC refresh/content edit invalidation wiring was found in the inspected host production paths.

### Test coverage

- ChapterContentSaveFenceTest covers serial token publication, replacement and invalidation on one fence instance.
- SourceChapterContentServiceTest uses fake caches for fresh hit, expired fallback, invalidation-during-request and index identity behavior.
- FileChapterContentCacheTest currently tests digest separation and bounded reads; it does not instantiate the real Android AtomicFile store.
- CacheFilePrunerTest covers protected just-written files and LRU ordering.
- RhinoSourceCacheExtensionsTest uses in-memory key-value/file stores; SourceScriptFileCacheStoreTest exercises the in-memory byte store. These are not persistence/restart tests for SharedPreferences and AtomicFile.

There is no verified multi-engine race test, real AtomicFile interruption/recovery instrumentation, cross-instance invalidation test, source-delete cache cleanup test or replacement-rule revision invalidation test in the inspected test set.

## 4. Badcat Rhino source cache lifecycle

### Current structure

- RhinoSourceCacheExtensions scopes storage by sourceId and prefixes persistent keys with __legado_cache__:. String KV values are wrapped with expiresAt; saveTime is seconds and finite values are bounded to 365 days. Memory values live in a concurrent in-memory map.
- SourceScriptFileCacheStore stores sourceId/key-separated files with SHA-256 filenames and AtomicFile; the durable implementation has a global 50 MiB budget. Reads update lastModified and pruning protects the newly committed file.
- SharedPreferencesSourceScriptKeyValueStore namespaces keys by encoded sourceId and has a clearSource implementation.
- RhinoSourceScriptRuntime.clearSourceState removes runtime scopes and cache extension instance, then clears key-value store, file store, physical source cache directory, login session and cookies.

### Semantic differences and lifecycle gap

- Upstream CacheManager.delete(key) removes the persistent row, memory entry and ACache file. Host RhinoSourceCacheExtensions.delete(key) removes only the persistent string KV key; a file written through putFile(key) remains. This is not the same mutation contract.
- Host RhinoSourceCacheExtensions.clear() clears memory and the file store, but does not call store.clearSource(sourceId); only the outer runtime clearSourceState also clears the key-value store.
- The inspected SourceCatalogActivity delete path removes catalog JSON, login session and Cookie, but does not call RhinoSourceScriptRuntime.clearSourceState. The import path can replace an existing source descriptor under the same ID without a confirmed cache invalidation hook.
- The host code-search index returned incomplete results for clearSourceState; this audit therefore does not claim that no other caller exists. The direct source-catalog lifecycle paths above are confirmed.

## 5. Decision and follow-up boundaries

| Domain | Decision | Reason |
|---|---|---|
| Generic upstream CacheManager API | NOT MET in Badcat | Room+memory+ACache semantics and WebCacheManager/JS bindings are not a drop-in match for the host source KV/file stores |
| Chapter-content cache storage | PARTIAL | AtomicFile, index key, TTL check and LRU exist, but legacy URL namespace and persistent invalidation closure are incomplete |
| Chapter save fence | NO-GO | per-instance, unsynchronized fence can fail across engine instances |
| Source cache lifecycle | NO-GO | delete/clear semantics differ and source removal/replacement is not wired to full runtime clear |
| BatchContentContext | NOT MET | upstream batch JS/rule content-save context is not connected to host production runtime |

Recommended next implementation order, each separately preregistered:

1. Make chapter save generations process-wide and thread-safe; test two cache instances sharing one app directory, concurrent begin/invalidate/publish, and stale write suppression.
2. Add real Android/Robolectric AtomicFile tests for write interruption, backup recovery, indexed invalidation, pruning and restart.
3. Add explicit source lifecycle hooks for remove/reimport/replacement that clear source KV, source file cache and chapter content cache without deleting unrelated book progress.
4. Align cache.delete and cache.clear across KV and file stores, then test TTL, clear, restart and source isolation.
5. Only after these gates, implement and test the upstream BatchContentContext rule/JS save path.

No implementation is authorized by this audit. The follow-up must be registered with exact paths and acceptance criteria before source edits.

## 6. Audit closeout

- Pinned upstream code search: CacheManager 43 files (complete index); delContent 11, saveText 17, reverseContent 9, commitResources 2. This is symbol search evidence, not a formal compiler-generated call graph.
- Host code search index is incomplete; only direct inspected callsites are claimed.
- Read-only inspection only. No build, tests, CI, APK, device or live-source test was run.
- No source, tests, Gradle/workflow, database, persistence, UI/Reader or main was changed.
