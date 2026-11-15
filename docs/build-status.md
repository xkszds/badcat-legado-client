# Build status

Last updated: 2026-11-15

## Latest engineering update

- Reviewed the pinned upstream implementation of `BookList`, `BookInfo`, `BookChapterList`, `BookContent`, `AnalyzeRule`, `AnalyzeUrl` and the upstream `BookSource` model.
- Added `docs/legado-runtime-porting-map.md` with the observed operation-level dependency map and a staged porting/acceptance plan.
- This is source review and documentation only; no upstream runtime code was ported and no build/test result is implied.

## Added

- Android application module and manifest; the launcher remains a temporary build-validation shell.
- Kotlin/JVM integration contract module.
- JDK 21 toolchain configuration.
- Kotlin plugin aligned to 2.4.20 and Android Gradle Plugin 8.13.2, matching the pinned Legado upstream baseline.
- Legado book-source JSON import/export codec that retains the complete raw source object, including rule fields unknown to this host, and maps basic metadata into the host source model.
- Re-verified original Badcat APK SHA-256 and recorded screenshot-backed UI acceptance criteria and high-priority resource paths in `docs/badcat-apk-ui-baseline.md`.
- Unit tests for source JSON round-trip/defaults/invalid identity and purification-domain separation.
- GitHub Actions workflow to run contract tests and assemble a debug APK.
- Pinned upstream commit and copied upstream GPL-3.0 license text.

## Not yet verified

- GitHub Actions has not been confirmed green; no successful CI test or package result is claimed.
- No APK artifact has been downloaded or installed.
- No Legado app-layer rule runtime is integrated. The source codec is import/export plumbing, not search or parsing execution.
- Original Badcat UI and its real navigation are not yet present in the new Android module.
- Badcat purification behavior has not yet been ported; only isolated interfaces and contract tests exist.
- No Gradle wrapper has been committed; CI provisions Gradle 8.14.4.

## Next validation

1. Confirm the latest workflow run and inspect logs.
2. Implement the Legado app-layer engine slice and its initialization, network and persistence dependencies.
3. Run source fixture tests for search, detail, catalog and chapter content.
4. Migrate the original UI behavior and verify it on the target Android device.
5. Only then treat a CI-produced APK as a candidate build artifact.
