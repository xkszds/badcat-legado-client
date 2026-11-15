# Badcat + Legado Client

A clean-room integration workspace for combining the Badcat 1.5.0.5 client experience with the open-source Legado reading/rule engine.

## Project goals

- Preserve the original Badcat navigation, screen hierarchy, visual language and interaction flow wherever technically and legally possible.
- Integrate Legado-compatible book sources, search/detail/catalog/content parsing and replacement rules through an explicit adapter boundary.
- Keep Badcat's existing purification behavior separate from book-text replacement, web-page ad filtering and video/episode rules.
- Keep the supplied original APK immutable as a reference artifact. Never overwrite it or rely on bypassing signature, integrity or native protection checks.
- Produce a reproducible, installable Android APK only after the source, licensing, build and device-validation gates pass.

## Current status

This repository was empty when connected on 2026-11-15. The initial architecture and engine contract are being established. No APK has been built from this repository yet.

## Repository map

- `docs/architecture.md` — integration boundaries and staged implementation plan.
- `docs/licensing.md` — GPL-3.0 compliance checklist for Legado-derived components.
- `integration-contract/` — platform-neutral Kotlin contract between the preserved client UI and a reading engine.

## Important constraints

1. The original APK is a read-only behavioral/resource reference, not a source-code substitute.
2. DEX symbols and resource names are evidence for investigation, not proof of runtime behavior.
3. Do not claim visual parity, feature parity, successful compilation or installation without corresponding tests.
4. Before distributing a combined work containing GPL-3.0 Legado code, provide the complete corresponding source and required notices under GPL-3.0.
5. Do not commit APKs, signing keys, passwords, tokens or private user data.

## Next engineering gates

1. Recover/establish a legitimate editable Android project baseline and verify the original app's build/runtime assumptions.
2. Select and pin a compatible Legado revision; map its app-layer rule execution dependencies rather than treating the book/rhino libraries alone as the complete engine.
3. Implement and test source/search/catalog/content/replacement adapters.
4. Connect the adapters to the original UI flow without changing existing navigation unnecessarily.
5. Integrate purification by content domain and validate on the target Android device.
6. Build, sign with a new project key, install and run regression tests.

See [the architecture plan](docs/architecture.md) and [licensing checklist](docs/licensing.md).
