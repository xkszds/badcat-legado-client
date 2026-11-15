# Build status

Last updated: 2026-11-15

## Added

- Android application module and manifest.
- Kotlin/JVM integration contract module.
- JDK 21 toolchain configuration.
- GitHub Actions workflow to run contract tests and assemble a debug APK.
- A temporary launcher activity for build validation only.

## Not yet verified

- No Gradle wrapper has been committed; CI currently provisions Gradle 8.14.4.
- The GitHub Actions workflow has not yet been observed running.
- No successful compile/package result is claimed.
- No APK artifact has been downloaded or installed.
- The temporary launcher is not the original Badcat UI.
- Legado upstream source is not yet incorporated.

## Next validation

Run the workflow from GitHub Actions, inspect the actual build logs, fix any failures, and only then treat the debug APK as a build artifact. Device installation and UI regression remain separate gates.
