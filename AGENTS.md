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

## Mandatory pre-work ledger discipline (user requirement, 2026-11-19)

For every work session and every new implementation task:
1. Re-read this file and the applicable required checklists before acting.
2. Before changing runtime/product code, append the task scope, exact target, upstream source, known callers, compatibility/security risks, regression tests and acceptance criteria to the mandatory checklist and the relevant migration plan.
3. Execute only the recorded scope. If inspection reveals that the checklist is inaccurate or incomplete, correct the checklist first, then continue.
4. After the work, update the checklist with actual files/behavior changed, test commands/results, CI run and commit SHA, unresolved items, and an explicit review of whether the checklist itself was correct.
5. Never mark a task complete based on a plan, documentation-only commit, compilation alone, pending CI, or unverified APK.

This process is mandatory; do not begin a code change while its task and regression/acceptance requirements are absent from the checklist.
