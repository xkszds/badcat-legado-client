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
