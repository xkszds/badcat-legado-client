# Project working instructions

These instructions are persistent requirements for all future work on this repository. Read this file before making changes.

## User requirements
- Work directly from the repository and source URLs the user provides. Inspect source data and code yourself; do not ask the user to download or upload files that can be fetched from the supplied URL.
- Prioritize fixing reported defects with code changes and regression tests. Do not stop at explanations or merely list likely causes.
- Preserve the existing app UI and visual design unless the user explicitly requests UI changes.
- Do not repeatedly ask for project background or information already present in issues, commits, chats, or repository files.
- Be concise and action-oriented in user updates.
- Never claim a fix, test, build, source, or APK is successful unless it has actually been verified. Clearly separate app-side defects from upstream website/network failures (403/404/429/5xx, TLS, timeouts).
- After each meaningful change, run the narrowest relevant tests, then the full available test/build workflow when practical. Inspect CI results and artifacts before sharing them.
- Keep source compatibility broad: implement Legado-compatible JavaScript bridge APIs and rule semantics with regression tests; do not patch one source with brittle special cases when a shared compatibility fix is appropriate.
- Treat search, book detail, TOC extraction, chapter opening, and chapter content as one end-to-end reading flow. Verify each stage independently.
- Keep this file updated when the user gives new durable project requirements.

## Compatibility workstreams
Track these as separate workstreams and verify each with fixtures and tests:
1. Rhino/Legado JavaScript: `java.*` helpers, `java.ajax`, URL scripts, per-card scripts, `jsLib` and `mainJs`, source isolation and execution limits.
2. Rule engine: CSS, XPath, JSONPath, regex, selector collection-vs-scalar semantics, combined selectors, variables and replacement rules.
3. HTTP and URL: URI/fragment handling, encoding helpers, redirects, headers, cookies, TLS diagnostics, status codes, retries and timeouts.
4. Search and discovery: GET/POST search, bookList extraction, field rules and pagination.
5. Book detail: metadata extraction, detail scripts and TOC URL resolution.
6. Table of contents: multi-page TOCs, chapter URL resolution, ordering, deduplication and incremental updates.
7. Chapter content: body extraction, content scripts, next/previous navigation, decoding and replacement.
8. Import and diagnostics: source import/export compatibility, stage-specific actionable errors and reusable regression fixtures.
9. Android integration: end-to-end search -> detail -> TOC -> chapter content while preserving the existing UI.

Do not mark a workstream complete based only on compilation. Require focused tests and, where possible, an end-to-end fixture. Keep upstream site failures (403/404/429/5xx, TLS validation, timeouts) separate from engine compatibility defects.

## Non-negotiable: deliver the complete upstream Legado runtime

The user's explicit requirement is a **complete Legado implementation**, not a simplified edition, a selector-only subset, a compatibility facade with missing behavior, or placeholder APIs. This section is mandatory reading before every work session and before every code change.

1. **Use the pinned upstream implementation as the behavioral source of truth.** The pinned revision is `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Integrate the real upstream models, rule lifecycle, AnalyzeRule/AnalyzeUrl behavior, script runtime/extensions, networking, source lifecycle, reading flow and batch behavior. Do not silently replace upstream behavior with locally invented subsets.
2. **No fake completion.** No no-op methods, null placeholders, unsupported stubs, selector-only substitutes, or simplified implementations may be presented as complete Legado APIs. If a host capability genuinely cannot exist in a particular context, preserve the exact upstream contract and implement its documented behavior/error semantics; document the limitation explicitly rather than pretending the API is complete.
3. **Every new upstream/original API triggers a full compatibility re-audit.** Before implementing it, inspect the exact pinned upstream declaration and call sites, then search the entire existing project for same-name methods, adapters, shims, fallback paths, duplicate implementations, overloads, test doubles and callers. After implementation, re-audit all affected old code and call paths—not just the new file—to find conflicting semantics, stale simplified code, shadowed overloads, changed nullability/types, lifecycle mismatches, threading issues, state leakage and regressions. Update or remove obsolete compatibility code where appropriate.
4. **Regression tests are mandatory for both sides of the change.** Add tests for the new API's upstream-compatible behavior and regression tests for existing callers/implementations affected by the change. Cover overloads, null/empty/error cases, source isolation, lifecycle/context, and persistence where relevant. Run focused tests, then the broadest practical test/build suite. Inspect CI logs and APK artifacts; never infer success from a commit or a queued workflow.
5. **Do not stop at compiling.** Verify behavior against pinned upstream tests/fixtures and real source flows where feasible. For reading, verify search -> detail -> TOC -> chapter content, including URL resolution, scripts, pagination, cookies/network behavior and replacement. For batch APIs, verify real batch context and durable cache persistence. A null batch context or fail-fast cache method is not batch support.
6. **Preserve the host product while completing the engine.** Keep Badcat's existing UI, navigation, page hierarchy, controls, gestures and existing features intact unless the user explicitly requests a change. The engine must be complete behind the host integration, not achieved by deleting or weakening host behavior.
7. **No partial release labeling.** Do not call the result “Legado complete/full version” and do not deliver it as the requested final APK until the required upstream runtime, host integration, regression suite, real-source checks and Android installation/device validation have passed. Report incomplete items plainly.
8. **Mandatory pre-change checklist (perform every time):**
   - Re-read this `AGENTS.md` in full.
   - Confirm the active branch is `integration/legado-upstream-runtime`; never write to `main`.
   - Confirm the pinned upstream revision and inspect the exact upstream API/source being changed.
   - Search all existing code, adapters, compatibility layers, call sites and tests for affected behavior.
   - Write down the compatibility and regression risks before editing.
   - Implement upstream-compatible behavior; avoid duplicate or simplified shims.
   - Add/update tests for the new API **and** all affected old behavior.
   - Run focused tests and the broadest practical build/CI; inspect failure logs.
   - Review the complete diff for regressions and verify the branch ref after pushing.
   - Update this checklist whenever the user adds a durable project requirement.

If a task appears to conflict with this policy, follow this policy and the user's complete-Legado requirement; do not take the shortcut of implementing only the newly mentioned method.

## Mandatory checklist navigation and work discipline (2026-11-20)

Canonical checklist system:
- `docs/required/00-MASTER-WORKFLOW.md` — read for every task.
- Read exactly the relevant category checklist for a single-category task:
  1. `docs/required/01-BADCAT-UI.md`
  2. `docs/required/02-READER.md`
  3. `docs/required/03-LEGADO-RUNTIME.md`
  4. `docs/required/04-ADBLOCK-ADMARK.md`
  5. `docs/required/05-PP-RULE.md`

Do not require every session to reread all five category lists or all historical audits. For a cross-category task, read only the additional category lists actually affected. Historical audit files are supporting evidence, not routine entry checklists. In particular, Legado work uses the master workflow plus the single consolidated category-3 checklist.

For every task:
1. Confirm branch `integration/legado-upstream-runtime`; never write to `main`.
2. Read this AGENTS file, the master workflow and the relevant category checklist.
3. Before code changes, register scope, exact target, upstream source/pinned SHA, callers, data ownership, compatibility/security risks, contract/regression tests and acceptance/exit criteria in the relevant category checklist.
4. Execute only the registered scope. If the checklist is incomplete, correct it before code.
5. After work, record actual files/behavior, test commands/results, exact CI run/head, artifact hashes, unresolved items and checklist self-review in that category checklist.
6. Never mark completion based on a plan, documentation-only commit, compilation alone, pending CI or unverified APK.

This process is mandatory; do not begin a code change while its task and regression/acceptance requirements are absent from the checklist.


## Mandatory complete-Legado migration order (user-confirmed 2026-11-20)

Execute the remaining work in this dependency order; do not skip ahead or treat a later layer as complete while an earlier contract is still lossy:

1. **Official data models and lossless serialization** — pinned BookSource, Book, BookChapter, nested rule models, and related source/book/chapter state; preserve all official fields and unknown JSON fields through import, persistence, mutation, and export.
2. **Complete rule engine** — pinned AnalyzeRule / AnalyzeUrl lifecycle and CSS, XPath, JSONPath, regex, JS, variables, replacement, request and response semantics.
3. **Rhino extension compatibility** — close the audited JsExtensions declaration/overload gaps and reconcile host ABI differences such as java.getSource() without breaking existing callers.
4. **One unified reading lifecycle** — route search, discovery, detail, TOC, and chapter content through the same upstream-compatible model, rule, URL, script, HTTP, cookie and source-state lifecycle.
5. **Batch context and durable content cache** — implement upstream-compatible batch chapter context, deduplication, persistence, invalidation and resume behavior; an empty context or throwing cache method is not support.
6. **Badcat product parity and final acceptance** — preserve the original five product categories and existing UI/navigation/gestures; verify real sources, Android install/device behavior, and release packaging.

Each stage requires a separate pre-registration, full caller/adapter/test audit, focused and broad regression suite, exact-head CI/artifact inspection, and an honest remaining-gap report. The pinned upstream SHA remains 330e606c4d57e08adc056ff8ab7a4132c784da9a. Never claim complete Legado or deliver a final release APK before all gates pass.


## 2026-11-20 pre-registered implementation scope — Stage 1 official models and lossless serialization (NO RUNTIME CODE EDITS YET)

### Current branch and evidence baseline
- Active branch: integration/legado-upstream-runtime; inspected branch head: 2b2bf91afca2dce8c07642eae4ae95f1a228937c. Do not modify main.
- Pinned upstream Legado revision: 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Latest exact-code-head Actions run: 44630795195, head 2b2bf91afca2dce8c07642eae4ae95f1a228937c; all reported job steps completed successfully, including contract tests, app unit tests, Rhino verification, Debug APK assembly and live source sampling. JUnit/APK/live-source artifacts still require inspection before recording detailed counts or source outcomes.
- Existing host boundary: integration-contract/.../ReadingEngine.kt defines simplified BookSource, BookCandidate, BookDetail, and BookChapter; LegadoBookSourceCodec parses selected source fields and stores rawJson, while the engine consumes selected fields. FileBookSourceCatalog persists descriptors and mutates enabled state through the codec. The rule entities currently present under app/src/main/java/io/legado/app/data/entities/rule/ are only a partial set of pinned models.
- Pinned upstream model sources identified: app/src/main/java/io/legado/app/data/entities/BookSource.kt, Book.kt, BookChapter.kt, and data/entities/rule/{ExploreRule,SearchRule,BookListRule,BookInfoRule,TocRule,ContentRule,ReviewRule}.kt. These models include Android/Room/Parcelable and runtime-facing contracts; they cannot be copied blindly into the platform-neutral module.

### Exact implementation target
Build a deliberate model/serialization compatibility boundary for Stage 1:
- Compare the exact pinned upstream declarations, nested types, default values, annotations/converters and serialization behavior for BookSource, Book, BookChapter and all source rule models.
- Inventory all current host model declarations, JSON codecs, catalog persistence/mutation/export paths, runtime constructors, UI adapters, tests and callers before changing any type or signature.
- Define and implement a lossless representation/adapter strategy that retains every official known field plus unknown/future fields across import -> in-memory representation -> persistence -> edit/toggle -> export. Preserve source identity semantics (bookSourceUrl) and book/chapter identity and ordering semantics.
- Map nested rule models using the pinned upstream contract; do not leave rule objects as opaque strings where runtime behavior needs typed models, and do not invent defaults that differ from upstream.
- Keep Android/Room/Parcelable concerns at the Android boundary; do not introduce an incompatible dependency or break the platform-neutral contract merely to copy upstream annotations.
- Keep existing UI and all current ReadingEngine callers source-compatible during the migration, or add explicit, tested adapters and migrate every caller in the same change.

### Compatibility/security risks to audit before edits
- Room annotations, Parcelable, ignored/transient properties, converters, lazy variables, BaseSource identity, and upstream helper dependencies may not compile or behave in the current host module.
- Gson/Kotlin serialization differences: missing vs null vs default values, numeric coercion, enum/unknown values, nested rule objects encoded as object or string, ordering, escaping, and unknown-field preservation.
- Existing code relies on BookSource.id/baseUrl for source identity and login/session scoping; do not change this identity path accidentally.
- Existing raw JSON preservation and import bounds/validation must not regress; mutations must alter only the intended fields and retain all other JSON members.
- Book/chapter key collisions, chapter indices, duplicate URLs, and runtime call sites must be audited before mapping to upstream Book / BookChapter.

### Required tests and acceptance
- Add pinned-upstream model contract tests for required/optional/default fields, nested rule types, unknown fields, null/missing values, enum/type edge cases, and identity/equality behavior where relevant.
- Add import/export round-trip fixtures from official upstream JSON, including future unknown keys and every rule family; verify semantic equality and field preservation.
- Add persistence round-trip and targeted mutation tests: import -> reload -> enable/disable or edit one field -> export; assert all unrelated fields remain byte/semantically intact.
- Add regression tests for every existing BookSource, BookChapter, LegadoBookSourceCodec, FileBookSourceCatalog, source-login identity, and ReadingEngine caller touched by the change.
- Run focused model/codec/catalog tests, :integration-contract:test, full :app:testDebugUnitTest, and :app:assembleDebug on the exact final code head; inspect XML reports and APK artifact. Verify all five rule models and all audited callers are represented in the adapter map.
- Acceptance is this Stage 1 slice only; it does not establish complete AnalyzeRule/AnalyzeUrl, full Rhino, unified runtime, batch support, UI parity, physical-device validation or release readiness.

### Explicit exclusions
No UI redesign; no changes to main; no selector/rule-engine rewrite; no Rhino extension expansion; no java.getSource() ABI change; no batch API/cache implementation; no unsupported stubs/no-op shims; no release APK claim.

### Required self-review before code
Re-read all five mandatory files and this registration; verify branch head again; inspect full pinned upstream model declarations and every affected host caller/adapter/test; list compatibility risks and test cases before editing. If inspection shows this registered scope is incomplete, amend this ledger first. After implementation, record exact changed paths, test results, CI run/head, artifact inspection, remaining gaps and a checklist self-review.
