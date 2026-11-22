# Runtime-3.2f.23 — upstream TOC equality vs batch chapter identity

Date: 2026-11-23  
Category: Third — Legado runtime  
Audit HEAD: `09fd5b04e3f55a334bd10b49d06a5abc422b6506`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Scope

Read-only differential audit of pinned `BookChapter`, `BookChapterList`, `BatchContentContext` and its tests against the current host chapter projection, TOC parser/service, and catalog query. No production or test files changed.

## Pinned upstream contracts

| File | Blob SHA | Observed behavior |
|---|---|---|
| `BookChapter.kt` | `9798267c77c9e14d0a673a822930491cd6e44cc1` | Although declared as a data class, it overrides `hashCode() = url.hashCode()` and `equals(other)` to compare `url` only. `title`, `baseUrl`, `index`, pay/VIP flags and other metadata do not participate in equality. |
| `BookChapterList.kt` | `dd2ae3950cee55a9c8e7880dc2c034d6f4c6f33d` | Per-page parser creates each chapter with `baseUrl = redirectUrl`, appends pages to one chapter list, then deduplicates with `LinkedHashSet(chapterList)`. Because the model's equality is URL-only, same URL rows collapse even when title/baseUrl differ. The list is optionally reversed before/after LinkedHashSet; therefore which duplicate object supplies retained metadata depends on those ordering flags and must not be guessed from a simple first-wins host operation. |
| `BatchContentContext.kt` | `68cad877f048b171e4f0f6cf22b1e0dd1e6481f9` | It receives a chapter list and does not deduplicate that list. It indexes each chapter's raw URL, `getAbsoluteURL()`, and book-level-normalized URL into candidate lists; candidates are distinct by `index`. Chapter-object resolution uses index; URL-string resolution succeeds only for one candidate and rejects ambiguity. |
| `BatchContentContextTest.kt` | `67207fecb4a96e8aae0a1bd7083d61cccbf657a9` | Constructs same-URL chapter twins manually and proves URL-only resolution is ambiguous, object resolution is index-specific, and saving one twin does not mark the other saved. These tests prove BatchContentContext's ability to handle twins supplied to it, not that the ordinary BookChapterList TOC merge emits same-URL twins. |

### Important separation

There are two different stages/contracts:

1. **Normal TOC merge:** `BookChapterList` uses URL-only model equality via `LinkedHashSet`, so duplicate URLs collapse regardless of different titles or per-page `baseUrl`.
2. **Batch content context:** if a list with same-URL chapter twins is supplied to it, the batch context can retain those objects by index and reject ambiguous URL-only callbacks.

The second contract does not negate the first. A manually supplied duplicate list or another producer can exercise BatchContentContext's twin support even though the normal TOC merge path deduplicates URL-only.

## Host behavior and divergence

| Host file | Blob SHA | Observed behavior |
|---|---|---|
| `ReadingEngine.kt` | `4777ceae6af13633c2561c7fc77c4cbde3a87480` | Host `BookChapter` is a Kotlin data class with generated equality over constructor properties, including `baseUrl`; it does not override equality/hashCode to match upstream. |
| `CssTocParser.kt` | `29ac90c3fc74546d52839e2479424890f9619de1` | Both script and non-script parser paths now populate each chapter's `baseUrl` from the current `response.finalUrl`. This preserves the correct per-page resolution provenance. |
| `SourceTocService.kt` | `0904d635be19ffd5de5ca56a5f06b258ed919d0b` | Merges pages, then deduplicates via `distinctBy { it.copy(index = 0) }`. This ignores only index; title, URL, flags, tag and baseUrl still participate. It therefore does not match upstream URL-only equality. |
| `SourceTocServiceTest.kt` | `2a4a8ed63ddf0d4eb1d3de33d342566dfc93360a` | .10-era tests explicitly retain same-URL/different-title rows; .21 test correction now expects same-URL/same-title rows from different page final URLs to remain because baseUrl differs. These are host-specific semantics, not demonstrated upstream TOC parity. |
| `ChapterCatalogQuery.kt` | `fc06c05a8bf0d7b7fbac306f14fe51a944745c29` | Exact URL lookup accepts only one match and fails closed on ambiguity. This is a safe URL-only caller policy, but it does not make the upstream TOC merge retain duplicates. |

## Corrected interpretation of .10/.10.1/.21

- The earlier assumption that pinned upstream `BookChapter` used generated data-class equality was wrong. The explicit URL-only override is authoritative.
- Existing .10/.10.1 CI proves the host's then-registered duplicate-preservation behavior and its regression tests; it does not prove that the ordinary pinned upstream TOC merge has the same behavior.
- Adding chapter-specific `baseUrl` is still justified: the pinned parser sets it from each page's redirect URL, and `getAbsoluteURL()` uses it. But it must not accidentally become a TOC dedup identity field if the goal is parity with `BookChapterList`.
- The current .21 test-only change that retains same-URL rows with different baseUrl is not accepted as upstream parity. #3178 is a separate pending exact-SHA CI for that host behavior and cannot settle this semantic question.

## Recommendation

**GO** for a separately registered TOC-only correction that makes host TOC merge identity explicitly URL-only while keeping the chapter data class's general equality unchanged. This avoids changing BatchContentContext's future index identity or using host-wide `equals` as a surrogate for every stage's identity semantics.

Before implementation, the follow-up must determine representative-selection order. Pinned `BookChapterList` reverses the accumulated list according to the source rule prefix, applies `LinkedHashSet`, then may reverse again according to book reverse-TOC state. The host `SourceTocService` currently lacks an explicit equivalent reverse-TOC contract. Do not claim exact retained metadata/order parity until this is resolved or explicitly recorded as an unresolved upstream behavior.

**NO-GO** for changing global host `BookChapter.equals/hashCode`, deleting BatchContentContext duplicate-twin tests, or treating BatchContentContext's candidate list as evidence that the normal TOC parser retains duplicate URLs.

## Evidence boundary

Static source inspection only. No tests/build/CI/device run for this audit. It does not implement BatchContentContext or close full Legado compatibility.
