# Pinned upstream analyzer integration audit

Audit date: 2026-11-19  
Host branch: `integration/legado-upstream-runtime`  
Host head reviewed: `6e1ffd1e513cc2e77581c889822e28e54f63558a`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`

This is a source-level dependency and compatibility audit. It is not a completion claim and intentionally precedes analyzer code changes.

## 1. Immutable source and current build boundary

- `.gitmodules` points `upstream/legado` to the official Legado repository.
- The host Git tree records `upstream/legado` as a gitlink at exactly `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- `:integration-contract` is a Kotlin/JVM module. Its `pinnedLegado` source set currently compiles selected parser files (JSoup, regex, RuleAnalyzer, SourceHtmlParser, and platform-adapted XPath/JSONPath bridges), not the upstream app execution layer.
- `:app` is an Android application module and currently depends on a host-defined source model, HTTP gateway and per-stage CSS parser services. Those are not upstream runtime models or the upstream lifecycle.

## 2. `AnalyzeRule.kt` — real rule evaluator and lifecycle

Pinned source: `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt` (1,060 lines).

The actual upstream class:
- Implements `io.legado.app.help.JsExtensions`.
- Accepts `RuleDataInterface?`, `BaseSource?`, pre-update-JS and book-info context.
- Retains mutable evaluation state: current chapter, next chapter URL, `BatchContentContext`, content/base URL/redirect URL, JSON/regex mode, cached parser instances, parsed-rule cache, regex cache, compiled-script cache, local bindings, weakly-held Rhino top scope and coroutine context.
- Supports scalar/list/element extraction, rule splitting and composition, `put/get` variables, regex replacement, JS evaluation, source AJAX, book refresh and TOC URL refresh. It also exposes extension setters for rule data, chapter, coroutine context, next-chapter URL and batch context.
- Dispatches actual upstream modes including WebJS, JS, JSONPath, XPath, JSoup/CSS and regex. It preserves collection-vs-scalar distinctions, URL normalization, replacement order, rule-local variables and script state.

Direct dependency families visible in imports and method bodies:
1. Upstream models: `BaseBook`, `BaseSource`, `Book`, `BookChapter`, `BookSource`, `RssArticle`, `RuleDataInterface`, `RuleData`, `SourceRule` and rule modes.
2. Parser implementations: `AnalyzeByJSoup`, `AnalyzeByXPath`, `AnalyzeByJSonPath`, `AnalyzeByRegex`, `RuleAnalyzer`, JSoup `Node`, Rhino `NativeArray/NativeObject/Scriptable`.
3. Script/runtime: pinned `CompiledScript`, `RhinoScriptEngine`, `buildScriptBindings`, source shared scope helpers, `SharedJsScope`, coroutine context and execution timeout/cancellation.
4. Host services: `JsExtensions`, `CacheManager`, `CookieStore`, `BackstageWebView`, `NetworkUtils`, Gson variants and debug/error helpers.
5. Batch/content: `BatchContentContext`, chapter identity and save lifecycle, source/book state and refresh callbacks.

**Compatibility risk:** copying this class into the current JVM module or compiling it against local look-alike models would silently change its state, overload, object-identity, JS scope, coroutine and cache semantics. It cannot be treated as a parser-only file.

## 3. `AnalyzeUrl.kt` — request construction and transport lifecycle

Pinned source: `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt` (1,053 lines).

Its public behavior includes:
- URL initialization in the upstream order: `@js`/WebJS and result chaining; embedded `{{...}}` plus page replacement; URL/options parsing; request field parsing and query/form encoding.
- JS bindings and per-source shared scope, `put/get`, request and response callbacks.
- Sync and suspend response paths, `StrResponse` and raw OkHttp `Response`, byte-array and input-stream paths, data URLs, uploads, Glide URL and user-agent behavior.
- Method/body/headers/charset/origin/retry/type/WebView/timeout/redirect/DNS/JS/bodyJS/server ID/WebView delay options.
- Cookie injection and persistence, redirect policy, proxy client, DNS override, Cronet and rate limiting.

Direct dependency families:
- Android `Base64`, Media3 `MediaItem`, Glide `GlideUrl`/headers.
- Hutool percent/hex codecs; OkHttp request/response/body/client types.
- `BaseSource`, `Book`, `BookChapter`, `RuleDataInterface`, `JsExtensions`.
- `CookieManager`, `CookieStore`, `BackstageWebView`, `Cronet`, `ConcurrentRateLimiter`, proxy/client and request helpers.
- Rhino script bindings/shared scope, app configuration, network/encoder/Gson utilities and coroutine context.

**Compatibility risk:** the current `SourceHttpGateway` compresses the request/response into a smaller host contract and the static URL resolver is not equivalent to `AnalyzeUrl`. Replacing only URL interpolation or routing the current gateway through a string-only adapter would lose raw response, byte stream, options, cookies, redirect, callback and cancellation semantics. Do not silently route unsupported WebView/DNS/proxy options to ordinary HTTP.

## 4. `JsExtensions.kt` — host API is not a small helper class

Pinned source: `app/src/main/java/io/legado/app/help/JsExtensions.kt` (1,390 lines).

The interface is coupled to Android and application services: Android WebView and Activity, app context, source verification/login UI, browser dialogs, video player, event bus, cookie and SSL helpers, cache and archive/file utilities, configuration, BookHelp, URL/encoding/Chinese conversion and async/coroutine helpers.

Important API families include:
- Batch `getBatchContext/cacheContent` and book/TOC/content refresh events.
- `ajax`, `ajaxAll`, `ajaxTestAll`, `connect`, Jsoup `get/head/post` response objects and timeout overloads.
- WebView rendering/source/override URL methods.
- Browser launch/await, video playback, verification-code UI and script import.
- Cache/download/archive/file operations and cookie access.
- Encoding, Base64/hex/crypto/hash, text/Chinese conversion, date/string and TTF helpers.

The current Android bridge has a substantial but bounded implementation. The following upstream methods remain without a true host implementation at this audit: `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap`, `getThemeMode`, `getVerificationCode`, `openUrl`, `openVideoPlayer`, `refreshBookInfo`, `refreshBookToc`, `refreshContent`, `showBrowser`, `startBrowser`, `startBrowserAwait`, `webView`, `webViewGetOverrideUrl`, `webViewGetSource`. Do not fill these with no-op/null stand-ins. Map each to an actual Badcat host service or return the exact upstream-compatible unsupported/error behavior for the contexts where it cannot exist.

## 5. Operation pipeline and model boundary

The pinned operation classes are:
- `model/webBook/BookList.kt`: search/explore list pipeline.
- `model/webBook/BookInfo.kt`: book detail pipeline.
- `model/webBook/BookChapterList.kt`: TOC extraction, pagination, ordering and chapter metadata.
- `model/webBook/BookContent.kt`: chapter content, replacements and batch content.

They call the real `AnalyzeRule` and `AnalyzeUrl`; TOC/content additionally use Room database state, chapter processing, Rhino bindings, rate limiting, and batch context. They are not independent parser utilities.

The upstream model audit also shows:
- `BookSource` imports Room annotations/converters, Parcelable, six distinct rule objects (`BookInfoRule`, `ContentRule`, `ExploreRule`, `ReviewRule`, `SearchRule`, `TocRule`), constants and audio-play model.
- `BaseSource` itself implements `JsExtensions` and depends on Rhino bindings, AES, CookieStore, CacheManager, shared source scopes, login UI and source lifecycle.
- `BaseBook` implements `RuleDataInterface` and uses big-data helpers.
- `Book` depends on Room, application DB, reader configuration, content processing, app configuration and network utilities.
- `BookChapter` depends on Room, application DB, `AnalyzeUrl`, regex timeout handling, Chinese utilities, URL resolution and app context.
- `BatchContentContext` depends on real `BookSource/Book/BookChapter`, `ContentSaveToken`, `BookHelp.saveContent`, chapter-index identity, content replacement and coroutine cancellation.

**Compatibility risk:** the existing host `BookSource` JSON staging model cannot be passed into upstream classes. Lossless JSON preservation is useful for import/export but does not supply upstream model methods, nested rule types, source lifecycle, Room semantics or `RuleDataInterface`.

## 6. Required integration order (do not skip gates)

1. **Model/serialization foundation:** port the exact source/rule entities and converters, with explicit host-owned persistence interfaces. Preserve all fields, defaults, unknown JSON fields and serialization behavior. Test round-trip against upstream fixtures before any analyzer wiring.
2. **Host service contracts:** define real injectable interfaces for source state, cookie store, cache, network, script scope, logging, browser/WebView, verification, refresh events and content persistence. Every interface must have a real Android implementation or explicit unsupported behavior; no fake global app context.
3. **AnalyzeUrl:** integrate the pinned class and option parser against the host transport while retaining upstream object/response semantics. Test methods, bodies, headers, charset, redirects, cookies, retries, timeout, DNS/proxy capability, script order, callbacks and binary response.
4. **AnalyzeRule:** integrate the pinned rule lifecycle with actual models, parsers and script scope. Test all rule modes, overloads, list/scalar/element distinctions, replacement, variables, cancellation, source isolation and cache invalidation.
5. **JsExtensions:** close the API inventory one method family at a time. For every new method, re-audit existing bridge methods, overloads, call sites, shims, fallbacks and tests.
6. **Operation pipeline:** migrate search/explore, detail, TOC and content to upstream `BookList/BookInfo/BookChapterList/BookContent` behavior as one vertical slice.
7. **Batch and persistence:** wire the actual `BatchContentContext`, save tokens and durable chapter writes; verify duplicate chapter URLs, stale writes, close/cancel and concurrent callbacks.
8. **Badcat host and device acceptance:** connect the existing UI without changing its navigation; validate real source reading and install/run the resulting APK on device.

## 7. Mandatory pre-change risk register

Before the first implementation change in each gate, record:
- Same-name host API and overload collisions.
- Nullability/default argument and Java-visible overload differences.
- JS object identity, wrappers, `this/globalThis`, source shared-scope and per-source isolation.
- Sync-vs-suspend/thread dispatch and cancellation behavior.
- Redirect/cookie ordering, response object preservation and body stream ownership.
- Rule mode, scalar/list/element conversion, regex replacement and variable evaluation order.
- Chapter identity by index (not URL alone), save-token freshness and durable persistence.
- UI callback lifecycle and whether a host capability is actually available.

After each implementation, search the entire host and upstream call graph again, add tests for new and affected old paths, run focused and broad CI, inspect reports and diff. This audit does not authorize implementing the classes before their dependencies are resolved.

## 8. Verification snapshot

- GitHub Actions run [44550830295](https://github.com/xkszds/badcat-legado-client/actions/runs/44550830295), commit `6e1ffd1e513cc2e77581c889822e28e54f63558a`: workflow completed with `success`.
- Job `assemble-debug`: contract tests, bounded Rhino host tests, Debug APK build and artifact upload all succeeded.
- Best-effort live checks for Nyasama, Feisuzhong, Pinshuzhai and a sample of four uploaded-corpus sources completed successfully at the workflow-step level. These are network/source smoke checks, not proof of full upstream parity or physical-device validation.
- Unit-test report artifact: `14542741454`; prior report parsing counted 253 tests, 0 skipped, 0 failures and 0 errors.
- Debug APK artifact: `14543241920`; intermediate debug artifact only, not a final complete-Legado release.
- Remaining blockers: upstream model/runtime integration, full `AnalyzeRule/AnalyzeUrl`, complete host API mapping, operation lifecycle migration, true batch persistence, full fixture parity and device installation/reading validation.

## Next implementation gate

Begin with **model/serialization foundation**, not by copying `AnalyzeRule.kt` into the current module. First inventory the complete upstream rule entity graph and converters, then design host persistence/service interfaces and add upstream round-trip fixtures. Do not alter the existing Badcat UI.
