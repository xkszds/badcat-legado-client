# Runtime-3.2f.40 — read-only concurrent-rate update and lifecycle audit

- Date: 2026-11-23.
- Category: Third — pinned Legado BaseSource concurrent-rate configuration/update behavior.
- Starting branch / HEAD: `integration/legado-upstream-runtime` / `2aa0b6524bccdf74fcf95f52691a7fd86fd4122b`.
- Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Trigger: Runtime-3.2f.36 aligned only the initial `fetchStart` parser and explicitly excluded `updateConcurrentRate`, window/wait strategy and lifecycle. The current host limiter updates its in-memory record's access/interval values inside every `acquire`, while the pinned upstream exposes an explicit `updateConcurrentRate(key, concurrentRate)` operation that preserves the current record on invalid values and preserves time/frequency when valid settings are applied. The equivalence and caller lifecycle have not yet been audited.
- Goal: read-only trace the pinned `updateConcurrentRate` contract, all callers that create/update/remove source concurrency settings, key identity and record lifetime; compare host `LegadoConcurrentRateLimiter`, source model mutation/import/update paths, all `acquire` callers and tests. Determine whether host per-request setting refresh is semantically equivalent or diverges from upstream explicit update timing.
- Required upstream evidence: pinned `ConcurrentRateLimiter.kt`, `BaseSource.getKey()`, `BaseSource.concurrentRate` mutation/callers, source import/update/delete lifecycle and any tests.
- Required host evidence: `LegadoConcurrentRateLimiter.kt`, all `acquire` callers, source DTO/catalog mutation and deletion paths, runtime lifecycle cleanup, relevant tests. Resolve every caller from the branch tree; do not infer from names.
- Allowed changes: only add `docs/audits/legado-runtime-3.2f.40-concurrent-rate-update-lifecycle-audit.md` and append this registration/closeout to `docs/required/03-LEGADO-RUNTIME.md`. No production/test/build/workflow/resource/UI/storage changes.
- Acceptance: pinned update function's valid/invalid/null/blank/zero/positive/N/M parsing and record-preservation matrix; key identity and record creation/update/removal call graph; host caller/lifecycle matrix; timing/window/frequency semantics; exact source blob evidence; GO/NO-GO and smallest separately registered implementation slice.
- Exclusions: no change to rate limiter implementation or tests; no wait strategy/cancellation/thread model change; no dynamic-header work; no schema, UI, Gradle, workflow, upstream pin, fourth/fifth category or `main`.
- CI policy: do not inspect Actions run #3200 or later individually; reserve them for the user's unified inspection pass. This documentation-only audit makes no CI/test/APK/device claims.
- Status: preregistered; read-only audit not yet closed.
