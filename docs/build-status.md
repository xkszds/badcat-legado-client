# Build status

Last updated: 2026-11-15

## Latest engineering update

- Reviewed the pinned upstream implementation of `BookList`, `BookInfo`, `BookChapterList`, `BookContent`, `AnalyzeRule`, `AnalyzeUrl` and the upstream `BookSource` model.
- Added `docs/legado-runtime-porting-map.md` with the observed operation-level dependency map and a staged porting/acceptance plan.
- This is source review and documentation only; no upstream runtime code was ported and no build/test result is implied.

## Added

- Android application module and manifest; the launcher remains a temporary build-validation shell.
- Kotlin/JVM integration contract module.
- JDK 21 toolchain configuration.
- Kotlin plugin aligned to 2.4.20 and Android Gradle Plugin 8.13.2, matching the pinned Legado upstream baseline.
- Legado book-source JSON import/export codec that retains the complete raw source object, including rule fields unknown to this host, and maps core source metadata plus raw JSON for explore/search/detail/TOC/content rule objects into the host contract.
- Re-verified original Badcat APK SHA-256 and recorded screenshot-backed UI acceptance criteria and high-priority resource paths in `docs/badcat-apk-ui-baseline.md`.
- Unit tests for source JSON round-trip/defaults/invalid identity, purification-domain separation, and the source-runtime contract defaults/stage separation.
- GitHub Actions workflow to run contract tests and assemble a debug APK.
- Pinned upstream commit and copied upstream GPL-3.0 license text.

## Not yet verified

- Added a bounded tokenizer and list-combination evaluator for `&&`, `||` and `%%`, checked against the pinned upstream `RuleAnalyzer` and `AnalyzeByJSoup` behavior. It is wired into the CSS evaluator. Added bounded JSONPath and XPath evaluators using pinned upstream libraries (`json-path` 3.0.0, JsoupXpath 2.5.3, Jsoup 1.23.2), with synthetic tests. These are selector primitives, not the full upstream `AnalyzeRule` pipeline: embedded `{$...}` interpolation, chained rule grammar, regex suffixes, script execution and source-stage orchestration remain unimplemented. Tests are authored but unrun.
- Added `SourceRuleObject` parsing for nested stage rule JSON, named-field lookup, and `evaluateField` dispatch into the explicit CSS/XPath/JSONPath selector entry point. Added `SelectorLegadoRuleRuntime`, which evaluates only fields whose parser modes are explicitly configured (so URL templates/JS/regex are not misread as CSS), and returns row maps. Added synthetic tests for field lookup, dispatch, unknown fields, and the selector-only runtime. Coroutines 1.11.0 is pinned to the Legado baseline for suspend runtime tests. These tests remain unrun.
- GitHub Actions has not been confirmed green; no successful CI test or package result is claimed.
- No APK artifact has been downloaded or installed.
- The source codec maps common upstream source configuration (headers, cookie-jar flag, JS library, login/explore/search URLs, main JS and nested rule JSON) into the host contract. The new CSS/JSONPath/XPath components are standalone selector primitives; nested source rules are not yet dispatched through them. There is no complete Legado source runtime, JavaScript execution runtime or search/detail/TOC/content orchestration.
- Original Badcat UI and its real navigation are not yet present in the new Android module.
- Badcat purification behavior has not yet been ported; only isolated interfaces and contract tests exist.
- No Gradle wrapper has been committed; CI provisions Gradle 8.14.4.

## Latest implementation update (2026-11-15)

- Expanded `BookSource` host contract to expose frequently used upstream source settings and raw nested rule JSON for explore, search, book info, TOC and content.
- Extended codec mapping and fixture assertions, including upstream default behavior for the cookie jar.
- Added explicit host boundaries for HTTP transport, the five separate rule stages (explore/search/book info/TOC/content), and source JavaScript execution. Added a tested request-settings factory that parses static source headers, applies case-insensitive per-request overrides, normalizes the method and carries the cookie-jar flag. This is not a concrete HTTP client: URL interpolation, cookie persistence, redirects, Rhino runtime/bindings, and search/detail/TOC/content orchestration remain unimplemented.
- Tests were edited but have not been executed in this environment; CI has not been confirmed green.

## Next validation

1. Confirm the latest workflow run and inspect logs.
2. Implement the Legado app-layer engine slice and its initialization, network and persistence dependencies.
3. Complete Legado `SourceRule` tokenization/mode and replacement semantics, embedded-rule interpolation and JavaScript/Rhino; implement concrete HTTP/Cookie transport and the search/detail/TOC/content orchestration.
4. Migrate the original UI behavior and verify it on the target Android device.
5. Only then treat a CI-produced APK as a candidate build artifact.
