# Runtime-3.2f.48 — existing Login UI v2 object-result fix supersedes duplicate work

- Date: 2026-11-24
- Branch: `integration/legado-upstream-runtime`
- Pinned upstream Legado SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Historical failure

Actions #3216, run ID `45102257055`, exact HEAD `b03759ce9ac2623bbf6c399ae300619b6acc7ca8`, job `137455545586`, reported:

`LegadoSourceLoginUiV2ServiceTest.mainJsInitializesLoginUiOnceInTheRealRhinoSourceScope`

The failure occurred because the service returned a root JavaScript object directly to a host runtime path that did not normalize root Scriptable objects.

## Existing fix

Commit `80b0b93e2d25d133f27cfeac905b484d5a7e74ae` is the immediate child of the failing HEAD. Its message is `runtime: normalize Login UI v2 JS object results`. It changes only:

- `app/src/main/java/app/badcat/client/runtime/LegadoSourceLoginUiV2Service.kt`
- `app/src/test/kotlin/app/badcat/client/runtime/LegadoSourceLoginUiV2ServiceTest.kt`

The service now wraps render/action hook results in `normalizeLoginResult`, which returns `JSON.stringify` for object values and preserves primitive/null/function behavior. Regression tests cover real-Rhino render and action paths.

## Current verification

On #3255, run `45240147409`, SHA `796be43155eaa1d10a0ec69160ff0415e1ee0512`:

- `LegadoSourceLoginUiV2ServiceTest` is present in the JUnit artifact.
- The complete Third-category app unit suite passed: 332 tests, 0 failures, 0 errors, 0 skipped.
- The suite profile excludes only the three named Fourth-category test classes; it does not exclude Login UI v2 tests.

## No duplicate implementation retained

A proposed broad change to `RhinoSourceScriptRuntime` and two extra tests was briefly committed, then fully reverted. The current runtime and test files are byte-for-byte identical to their contents at `ea795a617bccba85fea2fdf513a6ff2e40cf89e3`. No extra runtime behavior change remains.

## Decision

**Superseded; no new code required.** Keep the existing service-level normalization and regression tests. The current exact-HEAD CI continues to revalidate the existing implementation. No four-source test or fixture was modified.
