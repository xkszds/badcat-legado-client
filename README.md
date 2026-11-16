# Badcat + Legado Client

A clean-room integration workspace for combining the Badcat 1.5.0.5 client experience with the open-source Legado reading/rule engine.

## Project goals

- Preserve the original Badcat navigation, screen hierarchy, visual language and interaction flow wherever technically and legally possible.
- Integrate Legado-compatible book sources, search/detail/catalog/content parsing and replacement rules through an explicit adapter boundary.
- Keep Badcat's existing purification behavior separate from book-text replacement, web-page ad filtering and video/episode rules.
- Keep the supplied original APK immutable as a reference artifact. Never overwrite it or rely on bypassing signature, integrity or native protection checks.
- Produce a reproducible, installable Android APK only after the source, licensing, build and device-validation gates pass.

## Current status

The latest repository commit at the time of this update is `e226dd6b559c`. Its GitHub Actions run [#43845483951](https://github.com/xkszds/badcat-legado-client/actions/runs/43845483951) passed all steps: contract tests, bounded Rhino host tests, Android Debug APK assembly and artifact upload. The artifact `badcat-legado-debug` is 4,766,218 bytes (SHA-256 `9a09984e4b6a0250348c4f523b47fa61fda1bd5cebb3f89ef241ab424a2c2fdb`).

**This is still a buildable integration prototype, not a reconstruction of the original Badcat UI and not a release-ready reader.** The app contains a temporary launcher and development-only source import/export, search, detail, TOC and chapter-reader screens. The host has a bounded CSS parser subset and a bounded Rhino replacement evaluator, not Legado's complete `AnalyzeUrl` / `AnalyzeRule` environment with all parser modes, HTTP, cookie, cache and host objects. Original Badcat WebView ad filtering, AdMark and PPRule behavior have not been ported. Physical-device installation, runtime, UI parity and purification parity remain unverified.

See [the project completion checklist](docs/project-completion-checklist.md) for the completed work, outstanding P0/P1 gates and recommended order.

## Repository map

- `app/` — temporary Android host shell and development validation screens; not the final UI.
- `integration-contract/` — engine port, purification boundaries and Legado source codec with unit tests.
- `docs/architecture.md` — integration boundaries and staged implementation plan.
- `docs/badcat-ui-reconstruction-map.md` — evidence-based original UI, screen, and regression map.
- `docs/legado-upstream-baseline.md` — pinned upstream revision and verified build facts.
- `docs/legado-integration.md` — app-layer engine integration requirements.
- `docs/legado-runtime-porting-map.md` — source-verified search/detail/TOC/content runtime dependency map.
- `docs/licensing.md` — GPL-3.0 compliance checklist.
- `gradle/legado-upstream.properties` — immutable Legado revision and toolchain pin.
- `third_party/legado/LICENSE` — upstream GPL-3.0 license text.

## Important constraints

1. The original APK is a read-only behavioral/resource reference, not a source-code substitute.
2. DEX symbols and resource names are evidence for investigation, not proof of runtime behavior.
3. Do not claim visual parity, feature parity, successful installation or device validation without corresponding evidence.
4. Before distributing a combined work containing GPL-3.0 Legado code, provide the complete corresponding source and required notices under GPL-3.0.
5. Do not commit APKs, signing keys, passwords, tokens or private user data.

## Next engineering gates

1. Complete the Legado app-layer rule environment: `AnalyzeUrl` / `AnalyzeRule`, HTTP and cookie context, parser/runtime globals, and source-specific helper objects.
2. Expand fixture-backed search/detail/catalog/content and JavaScript rule tests against representative real-world source definitions.
3. Reconstruct the original Badcat UI screens and navigation from the read-only resource/screenshot baseline; see [the UI reconstruction map](docs/badcat-ui-reconstruction-map.md).
4. Connect the adapter to those screens without changing their navigation unnecessarily.
5. Map and integrate existing purification behavior by content domain.
6. Install the debug build on the target Android device and run import, search, detail, TOC, chapter navigation, progress, bookmark and reader-setting regression tests.
7. Establish GPL-3.0 source/notices compliance and a separate release-signing process before distribution.

See [the architecture plan](docs/architecture.md), [upstream baseline](docs/legado-upstream-baseline.md), [integration gates](docs/legado-integration.md) and [licensing checklist](docs/licensing.md).


### 原版 APK 与配套 DEX 对照

- [原版 APK 与配套 DEX 归档核对记录](docs/reference-archive-comparison-2026-11-16.md)
- [APK 页面与增强客户端差距报告](docs/badcat-apk-reverse-engineering-comparison.md)


### 原版 APK / 解包重建 / 截图对照

- [多版本 APK、DEX 归档与原版 UI 截图对照](docs/reference-build-chain-and-visual-diff.md)
- [DEX 归档核对更正](docs/reference-archive-comparison-2026-11-16.md)
- [原版 APK 页面差距报告](docs/badcat-apk-reverse-engineering-comparison.md)
- [参考材料索引](reference/README.md)


### Modern implementation policy

The original app is the UI/behavior reference, not a legacy code template. The new client uses modern Kotlin/Android architecture (Compose where appropriate, ViewModel/StateFlow, repositories and explicit data migrations) while preserving the original navigation, visual language and user-visible behavior. See [modern implementation policy](docs/architecture.md#modern-implementation-policy-confirmed-2026-11-16).
