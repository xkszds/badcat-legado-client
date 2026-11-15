# Reading engine contract

This directory defines a small, platform-neutral port for the existing Badcat UI.

## Implemented

- Host-facing operations for source listing, search, book detail, chapter listing and chapter content.
- Separate interfaces for chapter text replacement, page HTML filtering, catalog cleanup and video payload processing.
- Legado book-source JSON import/export codec. It validates the upstream identity fields `bookSourceUrl` and `bookSourceName`, maps basic source metadata, and retains the full JSON object so nested rules and unknown future fields are not silently discarded.
- Unit tests for codec import/export/defaults/validation and purification-domain separation.

## Not implemented yet

- Legado online search/detail/catalog/content rule execution.
- Rhino environment and script bindings.
- HTTP/network and cookie handling.
- Source persistence/database integration.
- Android reader UI integration.

The codec is source-file interchange plumbing, not a substitute for the Legado app-layer runtime. A concrete engine adapter must be implemented against the immutable upstream revision in `gradle/legado-upstream.properties`.
