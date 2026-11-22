# Reading engine contract

This directory defines platform-neutral contracts for the existing Badcat UI and the pinned Legado runtime.

## Implemented contract surfaces

- Existing Badcat reading operations: source listing, search, book detail, chapter listing and chapter content.
- Separate interfaces for chapter text replacement, page HTML filtering, catalog cleanup and video payload processing.
- Legado book-source JSON import/export codec. It validates upstream identity fields and retains the full JSON object so nested rules and unknown future fields are not silently discarded.
- **Runtime↔Android Host Port type boundary** in `RuntimeHostPort.kt`: source/caller identity, operation context and coroutine cancellation parent; hidden WebView request/result types; visible browser verification; separate image-code verification; typed host errors/cancellation; UI dispatch outcomes; and distinct product refresh events.
- Deterministic JVM contract tests, including public API signature inspection for Android/AndroidX type leakage.

## Not implemented

- No Android Host Port implementation or production JS API connection exists yet.
- No hidden/background WebView owner, pool, recycle or destruction lifecycle implementation exists yet.
- No visible browser/verification Activity or image-code UI adapter exists yet.
- No Badcat product-event subscribers are connected to the new refresh port.
- Existing Legado online search/detail/catalog/content rule execution, Rhino environment and full source lifecycle remain incomplete.
- HTTP/network and cookie behavior, source persistence/database integration and Android reader UI integration are not complete.

The Host Port declarations are not stubs: they define the required typed boundary, but intentionally provide no default/no-op implementation. A production adapter must be implemented only after its real host service and lifecycle exist. The codec is source-file interchange plumbing, not a substitute for the Legado app-layer runtime.