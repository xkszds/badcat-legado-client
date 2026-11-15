# Legado upstream baseline

## Pinned revision

- Repository: https://github.com/LegadoTeam/legado
- Commit: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Commit subject: paragraph-reader background synchronization fix
- License: GPL-3.0
- Pin file: `gradle/legado-upstream.properties`

The pin is immutable. Do not silently move the integration to the upstream default branch. Any upgrade must update the pin, re-review dependency and license changes, and pass the adapter tests.

## Verified upstream build facts at this revision

- Gradle wrapper distribution: 8.14.4
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.4.20
- Compile SDK: 36
- App minimum SDK: 23
- `:modules:book` and `:modules:rhino` minimum SDK: 21
- Gradle modules: `:app`, `:modules:book`, `:modules:rhino`
- `:modules:rhino` exposes the HtmlUnit fork of Rhino, coroutines, OkHttp and AndroidX collection.
- `:modules:book` is a local parsing library; it is not the complete online book-source engine.

## Critical engine boundary

Online book-source execution is in the Legado `:app` layer, not just the two library modules. The upstream application initializes Rhino in its Application startup and the source engine relies on app-level entities, database, HTTP/network helpers, rule models, coroutine context and application globals. The online search/detail/catalog/content pipeline includes app-layer components such as `BookChapterList` and `BookContent`.

Therefore, adding only `:modules:book` and `:modules:rhino` as dependencies would not deliver Legado-compatible online book sources. A valid in-process integration must either:
1. port the necessary app-layer engine slice together with its real dependencies and initialization, or
2. adopt the Legado app as the build base and migrate the Badcat host UI into it.

The first option preserves this repository's current host direction but requires careful dependency extraction and parity tests. The second is a larger architecture change and must not discard the Badcat UI behavior.

## Current state

The upstream commit and build versions are recorded. No upstream runtime code has been copied or compiled into this project yet. No engine functionality is claimed complete.
