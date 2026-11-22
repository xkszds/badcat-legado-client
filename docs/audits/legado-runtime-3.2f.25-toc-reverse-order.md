# Runtime-3.2f.25 — TOC reverse-order and duplicate representative audit

Date: 2026-11-23  
Category: Third — Legado runtime / TOC merge compatibility  
Audit HEAD: `3d67219516145d665087cf556b3ae43178068aaa`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Upstream execution order

Evidence:
- `BookChapterList.kt` blob `dd2ae3950cee55a9c8e7880dc2c034d6f4c6f33d`, lines 52–60, 124–137, 188–306.
- `TocRule.kt` blob `f525e912340257125059b6ad87d0cf7e73d6a1dc`: `chapterList` is an optional string; no independent boolean reverse field exists in TocRule.
- `Book.kt` blob `05024a6c71404eb49c23cd1df0482df365bc63fe`, lines 206–212: `getReverseToc()` reads `config.reverseToc`; the value is stored in the book's `ReadConfig` (`readConfig`) rather than the source rule.

The pinned flow is:
1. Read `tocRule.chapterList` (null becomes empty string).
2. If it starts with `-`, set local `reverse=true` and remove that prefix. Else if it starts with `+`, remove the plus prefix. The plus prefix does not set reverse.
3. Parse the initial TOC response and any next-page responses, appending each page's chapter rows to one mutable list. Each parsed chapter receives that response's `redirectUrl` as `baseUrl`.
4. If local `reverse` is false (the default and the `+` case), reverse the entire accumulated chapter list.
5. Construct `LinkedHashSet(chapterList)`. Since pinned `BookChapter.equals/hashCode` is URL-only, the first occurrence in this pre-dedup sequence wins; title, baseUrl and metadata are not equality fields.
6. If `book.getReverseToc()` is false (default), reverse the deduplicated list. If true, leave it in the pre-dedup order.
7. Assign continuous indexes after both reversals and deduplication.

For a simple accumulated sequence containing URL U first as row A and later as row B:
- default source rule (no leading `-`) reverses before set construction, so B is the retained object; final book reverse=false reverses the unique list again.
- leading `-` leaves the accumulated order before set construction, so A is retained; final book reverse=false still reverses the unique list.
- book reverse=true suppresses only the final reversal; it does not change which object is selected by the earlier source-rule-controlled pre-dedup reversal.
- leading `+` is equivalent to no prefix for ordering; it is only stripped before rule evaluation.

Thus source-rule reverse and per-book reverse are separate switches, and source-rule reverse affects duplicate representative selection as well as order. A generic first-wins URL distinct operation over the unreversed input is not equivalent.

## Host comparison

Evidence:
- `SourceTocService.kt` blob `0904d635be19ffd5de5ca56a5f06b258ed919d0b`: appends each parsed page to `chapters`, then uses `distinctBy { it.copy(index = 0) }` for incremental and final projections. It has no pre-dedup reversal and does not accept a book/config argument.
- `ReadingEngine.kt` blob `4777ceae6af13633c2561c7fc77c4cbde3a87480`: host `BookSource` retains raw `ruleTocJson`, but no parsed reverse flag; host `BookChapter` remains a data class.
- `LegadoBookSourceCodec.kt` blob `2d6f22a65898a2eaa4a8ee6386dd8f71d116e0dd`: raw TOC rule JSON is preserved and `chapterList` can be read, but host does not interpret the leading `-` / `+` ordering prefix.
- `SourceRuleResolver.kt` blob `7e4ec39248fcad9562b3a9dfb0263e5857bba1fd`: exposes string rule fields without prefix semantics.
- `SourceReadingEngine.kt` blob `70f1e0036a17b6d582744d612564842831b623f2`: TOC call is `tocService.load(source, bookUrl, detail.tocUrl, key, onChapters)`; no book config or reverseToc value is passed.
- `SourceBookActivity.kt` blob `15c0585b3a4f6dd44f49792563601caaa39158a0`, lines 195–215 and 490–515: `reverseCatalog` is a transient UI display toggle applied by `ChapterCatalogQuery.apply`; it is not the upstream `Book.getReverseToc()` value and does not affect TOC merge or dedup representative selection.
- GitHub code search returned no results for three targeted caller queries; direct call-chain inspection found the above engine/activity path. Therefore this is a verified path audit, not a claim of a complete repository-wide dynamic caller inventory.

## Compatibility matrix

| Behavior | Pinned Legado | Host at audit HEAD |
|---|---|---|
| Strip `chapterList` leading `-` | yes; also sets pre-dedup reverse | not implemented |
| Strip leading `+` | yes; no reverse | not implemented |
| Reverse accumulated rows before URL dedup | when no leading `-` | no |
| URL-only dedup | yes, through BookChapter equality + LinkedHashSet | no; title/baseUrl/flags also affect generated data-class equality |
| Duplicate representative | determined by pre-dedup reversal and input ordering | first row in appended order under current distinctBy |
| Per-book reverseToc | persistent Book ReadConfig; controls post-dedup reversal | no engine input or persisted equivalent found |
| UI reverse display | separate from source merge | transient activity display toggle only |
| Reindex after merge | yes | yes, but over a different retained set/order |

## Decision for .24

**GO** for the narrow URL-only identity correction as a host-local partial parity improvement, but only with explicit limits:
- It may safely stop baseUrl/title/flags from affecting duplicate identity.
- It must not claim exact upstream duplicate representative parity until source-rule prefix handling is implemented and ordering of all page results is specified.
- It must not claim per-book reverseToc parity; the current engine contract has no book ReadConfig input. A future implementation needs a separately registered typed book-level input and persistence/ownership design.
- The current host service can preserve response-page baseUrl on distinct URLs; URL-only collapse necessarily discards one duplicate row's metadata. The selected representative is currently first appended. That is a deterministic host rule, but it differs from pinned default source-rule pre-dedup reversal when the same URL occurs multiple times.
- UI `reverseCatalog` cannot be used as a substitute because it changes display ordering after TOC results and does not affect which duplicate object survives.

**NO-GO** for silently mapping the UI reverse toggle to upstream book reverseToc, globally overriding BookChapter equality, or claiming full reverse/representative parity from URL-only `distinctBy` alone.

## Evidence boundary

Static source inspection only. No code, tests, build, CI, emulator or device execution was performed. .24 remains registered but blocked on the terminal exact-SHA result of #3178 because it shares `SourceTocServiceTest.kt`; the reverse-order audit resolves the design prerequisite but does not close .24 or full Legado compatibility.
