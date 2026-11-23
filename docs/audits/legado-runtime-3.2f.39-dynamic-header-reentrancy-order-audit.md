# Runtime-3.2f.39 — dynamic header re-entrancy and ordering audit

Date: 2026-11-23  
Branch: `integration/legado-upstream-runtime`  
Audit HEAD: `249f72c4334df993e6125fff7448cc9e1c0ec962`  
Pinned Legado behavior baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`  
Status: read-only audit closed; implementation NO-GO.

## Scope and evidence reviewed

Host files reviewed at the audit HEAD:

- `integration-contract/src/main/kotlin/app/badcat/engine/SourceRequestFactory.kt`
- `integration-contract/src/main/kotlin/app/badcat/engine/SourceHttpGateway.kt`
- `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt`
- `app/src/main/java/app/badcat/client/runtime/BadcatSourceHttpGatewayFactory.kt`
- `app/src/main/java/app/badcat/client/runtime/BadcatReadingEngineFactory.kt`
- `app/src/main/java/app/badcat/client/runtime/RhinoSourceScriptRuntime.kt`
- `app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt`
- `app/src/main/java/app/badcat/client/runtime/SourceLoginSession.kt`

Pinned upstream files reviewed at the exact SHA:

- `app/src/main/java/io/legado/app/data/entities/BaseSource.kt`
- `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`
- `app/src/main/java/io/legado/app/help/rhino/NativeBaseSource.kt`
- `app/src/main/java/io/legado/app/constant/AppPattern.kt`
- `app/src/main/java/io/legado/app/help/config/AppConfig.kt`
- `app/src/main/java/io/legado/app/help/http/CookieStore.kt`

## 1. Re-entrancy and lock analysis

`RhinoSourceScriptRuntime.evaluate()` acquires the process-wide per-source `Mutex` with `withLock` before creating/reusing the source `VarScope`, binding the source bridge, evaluating `mainJs` (once) and evaluating the requested script. The bridge callbacks for `java.ajax`, `java.get`, `java.head`, `java.post`, and `java.connect` synchronously invoke host callbacks while this lock remains held.

The production callbacks in `BadcatReadingEngineFactory` build requests through `SourceRequestFactory.create()` and call the durable client via `runBlocking(Dispatchers.IO)`. A proposed dynamic-header provider that calls `SourceScriptRuntime.evaluate(source, headerJs, ...)` from one of these callbacks will attempt to acquire the same non-reentrant Mutex and can deadlock indefinitely. Creating another runtime is not a safe workaround: it would split source globals, can rerun `mainJs`, and breaks same-source JS state semantics.

The current API does not expose a proven safe operation to evaluate inline header JS inside the already-active Rhino scope/context. Therefore no production provider implementation is authorized by this audit. This is a hard NO-GO until a separately registered design demonstrates same-context evaluation without recursive lock acquisition, duplicate initialization, scope leakage, or cross-request/source contamination.

Cancellation is explicitly rethrown by `evaluate()` (`CancellationException` is not converted to a parse failure). `TimeoutCancellationException` is mapped to `RULE_TIMEOUT`. The synchronous `runBlocking` request callbacks create an additional cancellation/structured-concurrency boundary that must be addressed by a future implementation rather than assumed equivalent to the upstream suspend path.

## 2. Pinned header contract

At the pinned SHA:

- `AppPattern.JS_PATTERN` is case-insensitive and recognizes `<js>...</js>` or `@js:...`.
- `BaseSource.extractInlineJs()` trims the rule and uses `matcher.matches()`; the inline JS wrapper must match the complete header rule, not merely a substring.
- `BaseSource.getHeaderMap()` evaluates inline JS through `evalJS`, normalizes its result using `JsSourceEngine.normalizeJsResult`, then parses a string map. Ordinary exceptions are logged and swallowed; coroutine cancellation is rethrown.
- If no User-Agent header exists (case-insensitive), it adds `AppConfig.userAgent`.
- `AppConfig.userAgent` is a user-configurable preference. When unset/blank, the pinned default is a desktop Chrome-style UA derived from `BuildConfig.Cronet_Main_Version`; it is not the device WebView UA.
- Login headers are merged only when `hasLoginHeader` is true. `AnalyzeUrl` defaults `hasLoginHeader=true`, and evaluates eligibility against the initial request URL using the source and target registrable subdomain. If domain resolution fails, upstream preserves the legacy permissive behavior and attaches the login header. A later @js URL rewrite is not rechecked; URL-option headers can explicitly supply cross-domain headers.
- URL-option headers are applied after source headers (which already include default UA and eligible login headers), so per-request URL-option headers have final precedence.

## 3. Host caller graph and observed gaps

The audited host request routes include:

1. Rule-driven requests: `SourceHttpGateway.execute()` resolves URL rules, then calls `SourceRequestFactory.create(source, resolved)`, then the injected client.
2. JS `java.ajax`: `RhinoSourceScriptRuntime` bridge → `BadcatReadingEngineFactory.sourceAjax` → `SourceRequestFactory.create` → durable client.
3. JS `java.get`: bridge → `sourceGet` → `SourceRequestFactory.create` → durable client.
4. JS `java.head/post`: bridge → `sourceHttpRequest` → `SourceRequestFactory.create` → durable client.
5. JS `java.connect` with/without timeout: bridge → timeout-specific callback → `SourceRequestFactory.create` → durable client.
6. Gateway script-backed requests in `BadcatSourceHttpGatewayFactory` also construct requests directly for ajax and timeout-enabled connect.

The factory currently parses only static JSON in `source.header`; it has no inline header JS evaluator and does not inject the pinned configurable default UA. Therefore the dynamic header and UA behavior is absent on all the listed request-construction paths unless supplied explicitly by a rule/request.

The host login wrapper `SourceLoginSessionRequestClient` merges stored login headers **after** the complete request has been created. This means login headers currently overwrite URL-option/per-request headers, opposite to pinned `AnalyzeUrl` ordering where URL-option headers are applied after login headers. This is a concrete precedence mismatch.

The wrapper also calls same-site eligibility using `request.sourceId`. The host model has a separate `baseUrl` and an opaque `id`; `RhinoSourceLoginExtensions` is constructed with `source.id` as its `sourceKey`. The audit did not establish that this identifier is always the pinned source URL/key. The source-key-to-domain mapping must be made explicit before relying on this check.

## 4. Cookie and login lifecycle

Pinned `BaseSource.putLoginHeader()` parses the login header and, when a Cookie entry exists, calls `CookieStore.replaceCookie(getKey(), cookie)` before caching the raw login header. `removeLoginHeader()` deletes the login-header cache entry and calls `CookieStore.removeCookie(getKey())`.

Host `RhinoSourceLoginExtensions.putLoginHeader()` calls the supplied cookie writer before persisting the header; removal deletes the stored header and invokes the cookie remover. The production factory maps these callbacks to `mergeLoginCookieHeader(sourceKey, cookie)` and `removeCookie(sourceKey)`. However, because the current `sourceKey` is populated from `source.id`, URL/domain identity equivalence is not proven. Header map parsing also accepts only exact `Cookie` or `cookie` spellings for the cookie side effect, whereas the request-header map itself is treated case-insensitively in other host paths. A future implementation must test the pinned exact behavior and ensure secrets never enter logs.

## 5. Header precedence matrix

| Stage | Pinned upstream | Host at audit HEAD |
|---|---|---|
| Source static JSON header | Parsed first | Parsed by `SourceRequestFactory` |
| Source inline `@js:` / `<js>` | Full-match JS evaluation + normalize | Not implemented |
| Header JS error | Log/swallow ordinary exception; propagate cancellation | Not applicable; malformed static JSON fails request construction |
| Default User-Agent | Configurable `AppConfig.userAgent` if absent | Not injected |
| Same-site login header | Added after default UA, before URL-option headers | Client wrapper adds it after request creation |
| URL-option/request headers | Final override | Added during request creation, then may be overwritten by login wrapper |
| Cross-site eligibility | Initial URL; permissive fallback on unresolved domain | Check uses sourceId as source key; identity mapping unproven |
| Cookie write/remove | `replaceCookie(getKey(), cookie)` / `removeCookie(getKey())` | Host callbacks exist; source key mapping and exact parity unproven |

## 6. Decision and implementation gate

**NO-GO for dynamic-header implementation at this time.** The re-entrant Mutex risk is not resolved by the current runtime API. A provider must not call `evaluate()` recursively or instantiate a second source runtime.

Before any production change, create a separate implementation preregistration that:

1. Proves a same-active-scope inline evaluation path (or changes the execution architecture with explicit proof of preserved source scope, mainJs-once semantics, cancellation and serialization).
2. Centralizes header resolution so every request route above uses the same provider exactly once.
3. Preserves the exact order: source static/dynamic header → default UA if absent → eligible same-site login header → per-request URL-option headers.
4. Supplies a configurable AppConfig-compatible UA source, not WebView's UA.
5. Makes source URL/key/domain identity explicit and proves same-site behavior, including unresolved domains and initial-URL versus rewritten-URL behavior.
6. Preserves upstream ordinary-error swallowing and cancellation propagation without leaking secrets.
7. Tests Cookie replacement/removal and login-header persistence/clear semantics.
8. Adds differential tests for full-match JS syntax, string/object result normalization, case-insensitive UA detection, precedence collisions, same-site/cross-site cases, redirect behavior, all request entry points, cancellation, timeout and no deadlock.

No production code, tests, Gradle, workflows, UI, storage schema, original APK or user data were changed. No CI/JUnit/APK/device verification was run for this read-only audit. As requested, CI runs numbered 3200 and later are reserved for a unified inspection pass; no individual run was inspected in this audit.
