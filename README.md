# Badcat + Legado Client

A clean-room integration workspace for combining the Badcat 1.5.0.5 client experience with the open-source Legado reading/rule engine.

## Project goals

- Preserve the original Badcat navigation, screen hierarchy, visual language and interaction flow wherever technically and legally possible.
- Integrate Legado-compatible book sources, search/detail/catalog/content parsing and replacement rules through an explicit adapter boundary.
- Keep Badcat's existing purification behavior separate from book-text replacement, web-page ad filtering and video/episode rules.
- Keep the supplied original APK immutable as a reference artifact. Never overwrite it or rely on bypassing signature, integrity or native protection checks.
- Produce a reproducible, installable Android APK only after the source, licensing, build and device-validation gates pass.

## Current status

The repository now contains an Android build scaffold, platform-neutral reading-engine and purification contracts, an upstream-compatible Legado book-source JSON import/export codec, and a CI workflow. The selected Legado source revision is pinned in `gradle/legado-upstream.properties`.

**No Legado runtime rule engine has been integrated yet. No successful build or installable APK is claimed.** The temporary Android launcher is only a build-validation shell, not the original Badcat UI.

## Repository map

- `app/` — temporary Android host shell; not the final UI.
- `integration-contract/` — engine port, purification boundaries and Legado source codec with unit tests.
- `docs/architecture.md` — integration boundaries and staged implementation plan.
- `docs/legado-upstream-baseline.md` — pinned upstream revision and verified build facts.
- `docs/legado-integration.md` — app-layer engine integration requirements.
- `docs/legado-runtime-porting-map.md` — source-verified search/detail/TOC/content runtime dependency map.
- `docs/licensing.md` — GPL-3.0 compliance checklist.
- `gradle/legado-upstream.properties` — immutable Legado revision and toolchain pin.
- `third_party/legado/LICENSE` — upstream GPL-3.0 license text.

## Important constraints

1. The original APK is a read-only behavioral/resource reference, not a source-code substitute.
2. DEX symbols and resource names are evidence for investigation, not proof of runtime behavior.
3. Do not claim visual parity, feature parity, successful compilation or installation without corresponding tests.
4. Before distributing a combined work containing GPL-3.0 Legado code, provide the complete corresponding source and required notices under GPL-3.0.
5. Do not commit APKs, signing keys, passwords, tokens or private user data.

## Next engineering gates

1. Extract the required Legado app-layer source engine and initialization behind a host-owned adapter; do not mistake `:modules:book` and `:modules:rhino` for the complete online book-source engine.
2. Implement fixture-backed search/detail/catalog/content and JavaScript rule tests.
3. Connect the adapter to Badcat's original UI flow without changing its navigation unnecessarily.
4. Map and integrate existing purification behavior by content domain.
5. Build, sign with a new project key, install and run regression tests on the target Android device.

See [the architecture plan](docs/architecture.md), [upstream baseline](docs/legado-upstream-baseline.md), [integration gates](docs/legado-integration.md) and [licensing checklist](docs/licensing.md).
