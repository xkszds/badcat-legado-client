# Runtime-1.1 — Post-G2d A/B/C architecture boundary review

- Review registration: `bb1e5041b5586fbe8cdb107985ac93f2196ee4d4`
- Exact review base HEAD: `bb1e5041b5586fbe8cdb107985ac93f2196ee4d4`
- Branch: `integration/legado-upstream-runtime`
- Fixed upstream behavior baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Review type: read-only architecture/source/build/caller audit. No implementation was authorized.

## 1. Executive decision

**A is not a full-compatibility solution. Naive B/B′ is not presently buildable within the product boundary. C is the only technically coherent direction worth further investigation, but it is not yet proven and is NOT approved for production integration.**

C means a controlled extraction/fork of the authentic pinned Legado source-runtime core, with explicit typed ports for platform/UI and persistence services, while Badcat remains the only product UI and the single owner/writer for each adopted data domain. This is not equivalent to importing the full upstream `:app`, and it is not permission to replace upstream behavior with simplified host facades.

**Gate outcome:**
- GO: a separately registered, read-only cut-set/port-contract proof task to identify the smallest behavior-preserving boundary and exact source transformations.
- NO-GO: broad source copying, production runtime wiring, Room/schema migration, persistence cutover, UI/Reader changes, or claiming B′/G2/full compatibility.
- G2 remains BLOCKED/partial. B′ remains unpassed. No release or device verification is claimed.

## 2. Evidence reviewed

### Badcat exact tree

- `:integration-contract` is a Kotlin/JVM module. It compiles selected pinned analyzer files and host-side rule/network/runtime projections. It is Android-free by design.
- `:legado-runtime` is currently an Android library but has no production Kotlin source tree; its compile filter selects the seven pinned rule classes plus `GsonExtensions.kt`. The G2d probe extends this isolated selection to 12 verified upstream declarations. It does not contain BaseSource/BookSource or the source execution service closure.
- `:legado-rhino` packages the pinned HtmlUnit Core JS runtime and supporting coroutine/OkHttp/AndroidX collection dependencies. Badcat app already consumes it.
- `:app` consumes `:integration-contract`, `:legado-runtime`, and `:legado-rhino`. This is an existing partial integration, not a complete source runtime.
- Existing host data owners are split: `FileBookSourceCatalog` stores raw source JSON via Android AtomicFile; `BadcatReadingStateRepository` stores progress/bookmarks/history across SharedPreferences; TOC is in-memory; chapter body cache uses a separate file cache. The existing save fence is a host in-process mechanism, not proof of full upstream ContentSaveFence/BatchContentContext lifecycle parity.
- Existing host login is Login UI v2-oriented. Runtime-0.4 audit records missing legacy RowUi/login callbacks, full CookieStore behavior, WebView/browser/verification callbacks, reader/theme JS context and lifecycle refresh callbacks.

### Pinned upstream Gradle/module layout

At the exact pinned revision, upstream `settings.gradle` includes only `:app`, `:modules:book`, and `:modules:rhino`.

- `:modules:book` is a small Android library whose build declares AndroidX annotation; it is not the BookSource/BaseSource/JS source-runtime module.
- `:modules:rhino` is the reusable Rhino engine module (HtmlUnit Core JS API, coroutines, OkHttp, AndroidX collection). Badcat already has the corresponding pinned Rhino module.
- BookSource, BaseSource, Book, BookChapter, JsExtensions, JsSourceEngine, SharedJsScope, CacheManager and CookieStore live in upstream `:app` source. There is no upstream standalone book-source-runtime/core module to depend on directly.
- Upstream `:app` build config includes Android application plugins, Parcelize, Room plugin/KSP, ViewBinding, BuildConfig, app resources and other application packaging. Copying its build wholesale would also copy product-application concerns.

### Exact pinned source coupling

| Pinned source | Verified direct coupling | Architecture consequence |
|---|---|---|
| `BookSource.kt` blob `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c` | Android Parcelable; Room Entity/Index/PrimaryKey/TypeConverters; six typed rule models; AudioPlay; Gson utilities; implements BaseSource | Cannot be treated as a plain JSON DTO or copied into the JVM module without losing authentic Android/Room/source lifecycle contracts |
| `BaseSource.kt` blob `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa` | Extends JsExtensions; CacheManager, CookieStore, AppConfig, LoginUiV2, SharedJsScope, JsSourceEngine, source discovery/concurrency, encrypted login state and JS lifecycle | Behavior-bearing runtime root, not a marker interface |
| `Book.kt` blob `05024a6c71404eb49c23cd1df0482df365bc63fe` | Room/Parcelable; direct `appDb.bookDao` insert/update/delete; BookHelp/ContentProcessor/ReadBook helpers | Direct singleton DB access prevents simple module isolation |
| `BookChapter.kt` blob `9798267c77c9e14d0a673a822930491cd6e44cc1` | Room/Parcelable; direct `appDb.bookChapterDao` and `replaceRuleDao`; app context/toast; AnalyzeUrl/RuleDataInterface | Both persistence and UI side effects cross the boundary |
| `JsExtensions.kt` blob `f6808edd2a0d312e8bd13649935dea444cb82581` | AppCompatActivity, Android WebSettings, BackstageWebView, BottomWebViewDialog, Activity launches, app context, cookie/cache, source verification/browser, book and reader/theme helpers | Full API requires typed Badcat platform callbacks; plain HTTP or no-op replacements are not equivalent |
| `JsSourceEngine.kt` blob `44452025f81d8751afee92d3ee1f2d52f9e55905` | BaseSource/BookSource, CacheManager, JsExtensions, CookieStore, SharedJsScope, BatchContentContext, Rhino | Must preserve source object identity, batch context, cancellation and JS execution lifecycle |
| `SharedJsScope.kt` blob `dab7770d746c851c06ad05c08a35a2866a6100b3` | Rhino, HTTP, ACache, app context, global state/cache | Process/session state and persistence semantics need an explicit owner |
| `CacheManager.kt` blob `9575697d8ed60c0864aa3fd52d548ba059054205` | Direct `appDb.cacheDao`, ACache and memory LRU | Cannot import as-is without the upstream app DB authority |
| `CookieStore.kt` blob `09720d79f7009db052c080b54fc5fa2805257b64` | Direct `appDb.cookieDao`, CacheManager, CookieManager and OkHttp cookie semantics | Cookie/session storage and memory invalidation must have one owner and parity tests |

The pinned `AppDatabase` is Room version 113 and declares 26 entities plus a view and a long migration history. It includes book/source/chapter, Cookie, Cache and many unrelated product domains. Pulling the full database is not a narrow source-runtime integration.

The prior compiler-guided closure audit from exact pinned BookSource + BaseSource reaches 658 source files. Dependency-family counts overlap: Android 460, Activity/UI 288, database 141, network 84, Rhino/JS 59, cache 59, Room 54, WebView 32, Parcelable 27. This is an import/source graph, not a compiler-resolved call graph; it establishes breadth, not a precise minimal cut set.

## 3. A/B/C comparison

| Dimension | A — constrained Badcat-owned runtime/data ports | B′ — Badcat persistence + full runtime facade | C — controlled upstream core extraction + Badcat platform/data ports |
|---|---|---|---|
| Source fidelity | Low-to-medium unless every behavior is reimplemented and differentially proven; current selector/rule host path is not the complete upstream runtime | High in principle if authentic source is reused, but the current module cannot compile the BaseSource/BookSource service closure | Highest potential: preserve upstream types/semantics and maintain a small, explicit patch set at platform boundaries |
| Build boundary | Existing JVM module is buildable but cannot host Android/Room/Parcelable source contracts as-is | Current Android library is buildable only for the 12-file slice; full closure reaches app/UI/DB | No existing upstream core module; requires a deliberate source extraction and dependency refactor |
| UI/WebView/login | Must recreate all behavior in Badcat services; high parity burden | Full upstream closure imports Legado UI/WebView/login dependencies unless adapted | Can keep Badcat UI only if every browser/WebView/verification/login callback is implemented as a real Badcat host service with matching lifecycle/thread/error semantics |
| Database/cache/Cookie | Keeps host stores, but requires faithful mapping for all source/runtime state and batch/cache/Cookie semantics | Direct upstream appDb calls in Book/BookChapter/CacheManager/CookieStore create a competing authority unless redirected | One Badcat-owned database/repository boundary can be the only writer; authentic upstream entity/DAO contracts or explicit lossless adapters must be proven. Generic CacheManager KV, Cookie/session, and chapter body cache remain distinct domains |
| Five PoC domains | Can incrementally preserve host data but does not establish authentic upstream Book/BookChapter contracts | Can reuse authentic models, but DB and model methods are coupled to upstream appDb | Could unify ownership behind one Badcat-owned store while preserving upstream identity/converters; migration/rollback must be demonstrated before cutover |
| Risk | Semantic drift and endless reimplementation; may never meet “full Legado” | UI/Reader leakage, second DB, broad app source closure | Highest initial refactor cost and maintenance burden; upstream updates require controlled rebase/diff and full differential regression |
| Current status | Partial implementation only | Not buildable/accepted beyond isolated rule slice | Candidate only; no buildable core boundary yet |

### A — decision

A is useful as a safe incremental host architecture and can preserve existing Badcat UI/data. It is not acceptable as the final “complete Legado-compatible runtime” unless the full upstream API/overload/model/JS/lifecycle behavior is implemented and differentially proven. Replacing upstream methods with current selector evaluators, metadata maps, Login UI v2 only, or HTTP-only bridges would leave known compatibility gaps. **A alone is NO-GO for the user's full-compatibility goal.**

### B′ — decision

B′ describes desirable product/data ownership, not a proven technical source boundary. The current Badcat DB/data stores are not yet one fact source: source JSON, progress/bookmarks/history, TOC and chapter cache have separate owners. Further, authentic upstream Book/BookChapter/CacheManager/CookieStore methods access `appDb` directly, while BaseSource/JsExtensions require Android UI services. A dedicated module that merely depends on the upstream app source would either import broad product UI/Reader/DB dependencies or introduce behavior-breaking stubs. **Naive B′ is NO-GO.** B′ can remain the target ownership model only if implemented through C's controlled extraction and passes the migration/rollback gates.

### C — decision

C is the only coherent direction to investigate: extract the pinned source-runtime core from upstream `:app` into a dedicated, maintained module boundary; keep original package/API and behavior wherever possible; replace only unavoidable direct platform/data singleton dependencies with narrowly typed ports; implement those ports with Badcat-owned UI and a single Badcat-owned persistence authority. This is not a thin one-file adapter and may require a maintained patch set against the pinned source.

C is **not yet proven**. The exact minimum cut set is unknown; 658-node lexical reachability is not a compiler call graph. A valid C proof must show:
1. every required upstream public symbol/overload, caller and dynamic JS API remains covered;
2. every extracted source has a pinned path/blob and a documented minimal patch (if any);
3. UI ports cover WebView/browser/verification, login v1/v2, refresh callbacks, video and reader/theme context with correct lifecycle, thread, cancellation, result/error and null semantics;
4. data ports preserve BookSource/Book/BookChapter, ReadConfig/ParagraphAnchor, Cookie/session, generic CacheManager TTL/KV, source variables, batch content, chapter save fencing and cache invalidation;
5. exactly one writer/fact source exists per domain, with backup/dry-run/idempotency/rollback/restart proof;
6. the module compiles independently without importing Legado product UI/Reader or a competing AppDatabase; and
7. differential fixtures plus replay and real-source E2E pass.

## 4. Five-domain ownership implications

| Domain | Required upstream semantics | Current host fact owner / gap | C requirement |
|---|---|---|---|
| BookSource | `bookSourceUrl` identity, complete typed rules, converters/default/null/unknown fields, BaseSource lifecycle | AtomicFile raw JSON plus host projection; source catalog not an authentic BaseSource instance | One writer; lossless import/export; authentic model/serialization and source lifecycle parity |
| Book | `bookUrl`, all metadata/overrides, nested ReadConfig and ParagraphAnchor, chapter index/position | No unified durable Book owner in source flow; progress is SharedPreferences/UI state | Explicit field ownership and restart round-trip; no shadow Book pretending to be upstream |
| BookChapter | composite `(bookUrl,index)`, full metadata and RuleDataInterface behavior | TOC is in-memory host model; earlier URL-only content path lost index (typed index path was added, but this does not import authentic entity semantics) | Preserve duplicate URL/multi-index identity and exact URL alias ambiguity behavior |
| Reading progress | chapter index/position + ReadConfig/ParagraphAnchor; URL only navigation alias | SharedPreferences stores URL and scrollY under host keys | Reconcile legacy keys to upstream semantics, with backup, dry-run, rollback and restart tests |
| Chapter body/cache | `(bookUrl,index)`, ContentSaveToken/ContentSaveFence, batch save/retry; separate generic CacheManager TTL/KV | file cache now has index identity and a host in-process save fence; not proof of upstream durable/lifecycle parity | Differential token, stale writer, failure, cancellation, replacement, invalidation and TTL tests; distinct chapter-body and generic KV owners |

## 5. Next gate and recommendation

Do not begin schema v1, Room, source import expansion, production caller migration or UI changes from this review. The next separately registered task should be a **compiler-guided cut-set and typed-port contract proof for C**, not another blind source expansion:

- Start from the exact pinned roots already audited; partition the reachable graph into (i) behavior-bearing core, (ii) database/storage adapters, (iii) platform/UI callbacks, (iv) unrelated Legado product features.
- For each candidate boundary, inspect actual symbol references and callers, not only imports. Record exact overloads, dynamic JS names, thread/cancellation/error/null semantics and state mutation.
- Identify the minimum set of direct `appDb`, Activity/WebView, app-context, reader/theme, and product-service references that must be redirected. Do not omit methods to make the cut smaller.
- Build a dependency matrix and propose exact source files/patches, module plugins/processors/resources, host port interfaces and differential tests. The task remains audit/probe-only until this cut set is concrete.
- Only after that report is reviewed should a new implementation registration authorize a disposable module compile PoC. Production integration is a later, separate gate.

## 6. Review limitations and actual changes

- The 658-node graph is lexical/source-level evidence and may over- or under-approximate compiler-resolved reachability. Same-package, reflection, generated code, resource, Gradle variant and runtime callback edges require explicit follow-up.
- This review inspected the pinned module layout and key source roots but did not compile the full upstream app or attempt a production integration.
- No source, Gradle, tests, database/schema, persistence, production callers, UI/Reader, APK, or `main` changed. No test/build/CI/device pass is claimed for this documentation review.
