# Legado Runtime (Android)

This module is the dedicated Android-side integration boundary for the complete
pinned Legado book-source runtime. It intentionally does not contain or depend
on Legado UI, bookshelf UI, or Reader UI.

## Pinned upstream

- Repository: https://github.com/LegadoTeam/legado
- Revision: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- License: GPL-3.0

## Current implementation gate: Runtime-0.1

The module compiles the actual upstream source files for all six source-rule
models and the upstream Gson adapters directly from the pinned git submodule.
No shadow rule DTOs or replacement serializers are used.

This is only the first compile/contract gate. It is **not** the complete
Legado source runtime. The following remain blocking: BookSource/BaseSource,
AnalyzeRule/AnalyzeUrl, Rhino/JsExtensions ABI and lifecycle, HTTP/Cookie/login,
source variables, request/session behavior, business orchestration, cache and
all required transitive dependencies.

## Build

```sh
gradle :legado-runtime:testDebugUnitTest
gradle :legado-runtime:assembleDebug
```
