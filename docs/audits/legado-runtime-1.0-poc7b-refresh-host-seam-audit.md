# Runtime-1.0 PoC-7b — book/TOC/content refresh host seam audit

- Branch: `integration/legado-upstream-runtime`
- Registration commit: `94f1cecb478736601ddae94a79c55dde93b2cbee`
- Starting HEAD: `f71457b646bca831daa58a355dc42d26b280c112`
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: audit only; no runtime code or tests changed.

## 1. Upstream contract

Pinned `JsExtensions.kt` implements three distinct calls:

| JS method | Event tag | Consumer | Actual action |
|---|---|---|---|
| `refreshBookInfo()` | `refreshBookInfo` | `BookInfoActivity.observeLiveBus` | `refreshBook()` → `BookInfoViewModel.refreshBook(book)` → `loadBookInfo(book, false)`; remote detail is refreshed and the detail flow can subsequently reload the chapter list. |
| `refreshBookToc()` | `refreshBookToc` | `BookInfoActivity` and `ReadBookActivity` | Detail screen calls `loadChapter(book, true, isFromBookInfo = true)`; reader screen calls `loadChapterList(book)`, which fetches/replaces chapter rows and calls `ReadBook.onChapterListUpdated(book)`. |
| `refreshContent()` | `refreshBookContent` | `ReadBookActivity` | `refreshDurChapter()`: reset review summary, preserve reading position, clear current text chapter, invalidate current chapter body/resource cache, and reload the current chapter. Resource-theme generation changes use a separate resource refresh path. |

All are posted as `Boolean(true)` through LiveEventBus. The payload contains **no source, book, or chapter identity**. Activity observers are lifecycle-bound and additionally require `lifecycle.currentState.isAtLeast(STARTED)`. These three operations are not interchangeable.

Upstream files inspected: `JsExtensions.kt`, `EventBus.kt`, `EventBusExtensions.kt`, `BookInfoActivity.kt`, `BookInfoViewModel.kt`, `ReadBookActivity.kt`, and `ReadBookViewModel.kt` at the pinned SHA.

## 2. Badcat host state

- `SourceBookActivity.loadBook()` concurrently invokes detail and incremental TOC loading. It applies detail metadata and chapter rows on the main thread. It prevents duplicate loads with `bookLoading` and ignores callbacks after finish/destroy, but has no source-script refresh entry point.
- Nested `SourceChapterActivity` carries `sourceId`, `bookUrl`, `currentIndex`, and typed `BookChapter`; it loads content through the typed engine method. It has no source-script refresh observer.
- `ReadingEngine` exposes detail, TOC and typed chapter-content loads, but no refresh operation or cache invalidation contract.
- `SourceReadingEngine` delegates to `SourceBookDetailService`, `SourceTocService`, and `SourceChapterContentService`. TOC emits incremental snapshots and assigns chapter indices.
- `SourceChapterContentService.load()` can return cached content before a network request. Its save-token fence rejects stale publication after invalidation, but the reader-facing engine API does not expose an invalidate-and-reload operation.
- `BadcatReadingEngineFactory` wires the production engine/cache, but no refresh dispatcher or source-script refresh bridge is wired.

Host files inspected: `SourceBookActivity.kt`, `BadcatReadingEngineFactory.kt`, `ReadingEngine.kt`, `SourceReadingEngine.kt`, `SourceBookDetailService.kt`, `SourceTocService.kt`, `SourceChapterContentService.kt`, plus relevant tests and callers.

## 3. Compatibility matrix

| Capability | Host fit | Decision |
|---|---|---|
| Book info refresh | Detail service exists, but no event/port, identity-scoped generation, or coordinated detail/TOC semantics. | Not implemented; not a generic UI redraw. |
| TOC refresh | TOC service exists, but no active-screen replacement/reconciliation of the current reader chapter. | Not implemented; preserve identity and fail closed if removed/ambiguous. |
| Current content refresh | Typed chapter load and cache fence exist, but no engine-level exact-key invalidation before refetch. A plain load may return stale cached content. | Not implemented; invalidate then reload. |
| Lifecycle | Activities have lifecycle states, but there is no source-scoped dispatcher bound to the active matching book/reader. | Not implemented; no global dispatch to whichever screen is visible. |

## 4. Semantic risks

1. Upstream event identity is implicit singleton state. Copying its global Boolean event bus risks refreshing the wrong Badcat book.
2. Detail, TOC and content refresh have different effects; collapsing them into one method loses behavior.
3. TOC refresh can remove/reorder the active chapter. Reconcile by stable chapter identity/index and URL alias rules; never guess on ambiguity.
4. Content refresh must bypass cache. The save-token fence prevents stale writes after invalidation but does not itself invalidate.
5. Racing requests need cancellation and generations so older detail/TOC/content results cannot overwrite newer results. The current duplicate-load guard is not a complete cross-operation generation protocol.
6. Match upstream STARTED gating with explicit active-owner lifecycle; cancel/discard work on stop/destroy and do not replay old global events into a later book.

## 5. Future seam (not implemented)

Do not reproduce the global Boolean LiveEventBus contract as a Badcat public API. Keep the three JS method names at the eventual compatibility bridge, but route each through a source-execution-scoped host capability carrying immutable identity:

- `RefreshBookInfo(sourceId, bookUrl, requestGeneration)`
- `RefreshBookToc(sourceId, bookUrl, requestGeneration, currentChapterIdentity?)`
- `RefreshCurrentContent(sourceId, bookUrl, chapterIndex, chapterUrl, requestGeneration)`

These are conceptual shapes, not accepted production types. A separate implementation registration must define active UI owner selection, STARTED/STOPPED/DESTROYED behavior, distinct handlers, cancellation/generations, TOC chapter reconciliation, exact typed cache invalidation, position preservation, and no cross-book dispatch. Required tests include same URL/different index, removed/ambiguous chapter, stale completion, stopped/destroyed owner, and cache bypass, followed by Android integration tests using the real factory-wired engine.

Do not add methods to `RhinoSafeSourceJavaExtensions` until the host capability is real and its identity/privacy boundary is reviewed.

## 6. Result

- Documentation/audit only. No production Kotlin/Java, Gradle, runtime bridge, tests, database/schema, persistence, Badcat UI/Reader, APK, or `main` changed.
- No unit test, build, CI, source smoke test, or device result is claimed.
- PoC-7b is an audit checkpoint, not an implementation or compatibility pass. The three refresh APIs remain missing from the host bridge. Upstream JVM/Rhino parity, BaseSource lifecycle, WebView/login/navigation and full Legado compatibility remain blocked; B′ remains unpassed.
