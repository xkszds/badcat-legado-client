# Badcat + Legado Client

A clean-room integration workspace for combining the Badcat 1.5.0.5 client experience with the open-source Legado reading/rule engine.

## Project goals

- Preserve the original Badcat navigation, screen hierarchy, visual language and interaction flow wherever technically and legally possible.
- Integrate Legado-compatible book sources, search/detail/catalog/content parsing and replacement rules through an explicit adapter boundary.
- Keep Badcat's existing purification behavior separate from book-text replacement, web-page ad filtering and video/episode rules.
- Keep the supplied original APK immutable as a reference artifact. Never overwrite it or rely on bypassing signature, integrity or native protection checks.
- Produce a reproducible, installable Android APK only after the source, licensing, build and device-validation gates pass.

## Current status

The repository now has a successful CI debug build. The latest verified run is [GitHub Actions #43707974288](https://github.com/xkszds/badcat-legado-client/actions/runs/43707974288), for commit `c72b96a288942dbeda02a653f268843bb25c78b1`. It passed the contract unit-test task, restored and verified the pinned Rhino artifact and extension sources, assembled the debug APK, and uploaded the `badcat-legado-debug` artifact. Download it from the run's **Artifacts** section.

The app currently contains a temporary launcher plus development-only source import/export, search, detail, TOC and chapter-reader screens. **This is a buildable integration prototype, not a reconstruction of the original Badcat UI and not yet a release-ready reader.** The bounded Rhino adapter evaluates scripts with string bindings only; it does not yet provide Legado's complete `AnalyzeUrl` / `AnalyzeRule` environment, including source-specific HTTP, cookie, cache and other host objects. Badcat's original purification behavior has also not yet been ported. A successful CI build does not prove installation or runtime behavior on a physical device.

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
