# Badcat + Legado Client

A clean-room integration workspace for combining the Badcat 1.5.0.5 client experience with the open-source Legado reading/rule engine.

## Project goals

- Preserve the original Badcat navigation, screen hierarchy, visual language and interaction flow wherever technically and legally possible.
- Integrate Legado-compatible book sources, search/detail/catalog/content parsing and replacement rules through an explicit adapter boundary.
- Keep Badcat's existing purification behavior separate from book-text replacement, web-page ad filtering and video/episode rules.
- Keep the supplied original APK immutable as a reference artifact. Never overwrite it or rely on bypassing signature, integrity or native protection checks.
- Produce a reproducible, installable Android APK only after the source, licensing, build and device-validation gates pass.

## Current status

The repository now has a passing CI Debug build for the latest code commit `61fb8240cdf4`: [GitHub Actions #43844875160](https://github.com/xkszds/badcat-legado-client/actions/runs/43844875160). Contract tests, bounded Rhino host tests, Android Debug APK assembly and artifact upload all succeeded. The artifact is 4,766,215 bytes (SHA-256 `040a7d9f536d261e6e572a4995f81a749e092b3ccbba1969cc1b9590a3b8ecba`). A documentation-only follow-up commit `681c8ddc640e` also passed tests, APK assembly and upload; its Actions run is completing post-job cleanup.

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
3. Reconstruct the original Badcat UI screens and navigation from the read-only resource/screenshot baseline; see `docs/badcat-ui-reconstruction-map.md`.
4. Connect the adapter to those screens without changing their navigation unnecessarily.
5. Map and integrate existing purification behavior by content domain.
6. Install the debug build on the target Android device and run import, search, detail, TOC, chapter navigation, progress, bookmark and reader-setting regression tests.
7. Establish GPL-3.0 source/notices compliance and a separate release-signing process before distribution.

See [the architecture plan](docs/architecture.md), [upstream baseline](docs/legado-upstream-baseline.md), [integration gates](docs/legado-integration.md) and [licensing checklist](docs/licensing.md).
