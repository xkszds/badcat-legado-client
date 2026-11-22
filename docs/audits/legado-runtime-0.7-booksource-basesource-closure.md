# Runtime-0.7 authentic BookSource/BaseSource compile-input audit

Date: 2026-11-20

## Immutable inputs

- Legado repository: `LegadoTeam/legado`
- Required source commit: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Badcat audit base: `28e3ec308434a6328af4d1d5744de46727db4b55`
- Source-closure evidence: Runtime-0.6 artifact `legado-runtime-dependency-closure`, ID `14625097285`, SHA-256 `a46b156a7ac23f0425cb2512550845d3be9a585def3c73a309c1ee9030a8829d`.

## Exact initial source inputs

- `upstream/legado/app/src/main/java/io/legado/app/data/entities/BookSource.kt`
- `upstream/legado/app/src/main/java/io/legado/app/data/entities/BaseSource.kt`

Both are actual upstream files and must be compiled unchanged. Their direct source imports establish the first dependency boundary.

### BookSource.kt direct imports

```
android.os.Parcelable
androidx.room.ColumnInfo
androidx.room.Entity
androidx.room.Index
androidx.room.PrimaryKey
androidx.room.TypeConverter
androidx.room.TypeConverters
io.legado.app.constant.AppPattern
io.legado.app.constant.BookSourceType
io.legado.app.data.entities.rule.BookInfoRule
io.legado.app.data.entities.rule.ContentRule
io.legado.app.data.entities.rule.ExploreRule
io.legado.app.data.entities.rule.ReviewRule
io.legado.app.data.entities.rule.SearchRule
io.legado.app.data.entities.rule.TocRule
io.legado.app.model.AudioPlay
io.legado.app.utils.GSON
io.legado.app.utils.fromJsonObject
io.legado.app.utils.splitNotBlank
kotlinx.parcelize.Parcelize
```

### BaseSource.kt direct imports

```
android.webkit.JavascriptInterface
cn.hutool.crypto.symmetric.AES
com.script.ScriptBindings
com.script.buildScriptBindings
com.script.rhino.RhinoScriptEngine
io.legado.app.constant.AppConst
io.legado.app.constant.AppLog
io.legado.app.constant.AppPattern.JS_PATTERN
io.legado.app.data.entities.rule.RowUi
io.legado.app.exception.NoStackTraceException
io.legado.app.help.CacheManager
io.legado.app.help.ConcurrentRateLimiter.Companion.updateConcurrentRate
io.legado.app.help.JsExtensions
io.legado.app.help.config.AppConfig
io.legado.app.help.crypto.SymmetricCryptoAndroid
io.legado.app.help.http.CookieStore
io.legado.app.help.source.clearExploreKindsCache
io.legado.app.help.source.getShareScope
io.legado.app.help.source.getSharedGlobalStateKey
io.legado.app.model.SharedJsScope
io.legado.app.model.SharedJsScope.remove
io.legado.app.model.jsSource.JsSourceEngine
io.legado.app.model.login.LoginUiV2
io.legado.app.utils.GSON
io.legado.app.utils.fromJsonArray
io.legado.app.utils.fromJsonObject
io.legado.app.utils.has
io.legado.app.utils.isMainThread
kotlinx.coroutines.CancellationException
kotlinx.coroutines.runBlocking
org.intellij.lang.annotations.Language
```

## Dependency evidence and consequences

- Android platform: Android SDK APIs including Parcelable and JavascriptInterface.
- Room: upstream app pins Room `2.8.4` and KSP `2.3.12`; source annotations are real runtime model metadata, not safe to erase.
- Kotlin Parcelize: upstream Kotlin plugin `2.4.20`; Parcelize-generated Parcelable implementation is required.
- Gson: upstream `2.14.0`; upstream Gson extensions and all six rule models are part of the model contract.
- Rhino: actual upstream `:modules:rhino` Android library, plus `org.htmlunit:htmlunit-core-js:5.3.0-legado.4`, coroutines `1.11.0`, OkHttp `5.4.0`, AndroidX collection `1.6.0`.
- Crypto: Hutool `5.8.22` and Bouncy Castle `1.86` (upstream app coordinates).
- The direct imports alone already reach app configuration, shared JS scope, CookieStore, CacheManager, concurrency limiter, login semantics and JS extensions. A marker interface or DTO replacement would not preserve upstream behavior.

## Closure result

The Runtime-0.6 graph was traversed from the exact two roots `BookSource.kt` and `BaseSource.kt` using directed resolved internal import edges. It reaches **658 upstream source nodes**. The overlapping dependency-family counts are:

| Family | Reachable source nodes |
|---|---:|
| Android platform | 460 |
| Activity/UI | 288 |
| Database | 141 |
| Room | 54 |
| WebView | 32 |
| Rhino/JS | 59 |
| Network | 84 |
| Parcelable | 27 |
| Cache | 59 |

Representative closure paths include `App.kt`, `AppDatabase.kt`, Book/BookChapter and DAO entities, `CacheManager.kt`, `JsExtensions.kt`, `SharedJsScope.kt`, `AnalyzeRule.kt`, `AnalyzeUrl.kt`, `BackstageWebView.kt`, `CookieStore.kt`, WebView pools, source login/verification bridges, reader helpers, RSS helpers and video/player classes. The graph's limitations still apply: same-package references, reflection, generated resources and Gradle wiring are not fully modeled. The 658 count is therefore a lower-bound source graph, not a compiler closure.

## Architecture finding

A verbatim BookSource/BaseSource boundary is not a small independent SDK. The dependency graph pulls app-wide persistence, cache, UI/WebView, reader, RSS and media surfaces. Do not add all 658 files to production `:legado-runtime`, and do not shrink the source set by replacing upstream classes or methods with simplified stand-ins.

C remains a candidate only if a compiler-guided, behavior-preserving service seam can be identified. The next PoC should begin with an isolated Android compile target and the exact two upstream source files, then report the complete compiler diagnostics. If the required authentic closure necessarily imports broad Legado product UI/Reader or its database as a second authority, record that as an architecture blocker and evaluate the constrained official-core/adapter boundary. No production migration or schema change is authorized by this audit.

## Status

- This document is an input audit, not a compile PoC.
- No source files were copied or altered.
- No fake upstream types, stubs, no-op services or production callers were added.
- No Gradle, DB, UI or reader changes were made.
- Runtime-0.7 remains in progress; full Legado compatibility and B' remain unaccepted.
