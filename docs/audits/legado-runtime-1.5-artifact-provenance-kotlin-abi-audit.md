# Runtime-1.5 Existing Artifact Provenance and Kotlin/JVM ABI Audit

- **Result:** PARTIAL / ACCEPTANCE NOT MET.
- **Branch:** `integration/legado-upstream-runtime`
- **Starting code/documentation HEAD:** `06712ecfa821222fefdb764ea315a015aac9cbc7`
- **Registration commit:** `5cc570d406af979a209e1f6ac1d1f60f9c1fd25b`
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- **Scope:** read-only existing artifact/provenance and source-inventory audit. No new compilation, tests, production source changes or build workflow changes.

## 1. Executive result

1. **The 122-vs-118 count is narrowed to a scope distinction.** A lexical scan of the pinned `JsExtensions.kt` file finds 122 `fun` tokens total: 121 direct members inside `interface JsExtensions`, plus one top-level `internal` helper. The 121 interface members are 118 public declarations plus three private helpers. Thus 122 is not the count of public `JsExtensions` interface methods; 118 is. The old counting script was not preserved, so this explains the arithmetic and scope difference but cannot prove which exact historical command produced 122.
2. **An upstream release APK is associated with the exact pinned tag.** The `beta` tag resolves directly to the pinned commit. The matching GitHub Release contains universal APK assets with GitHub-provided SHA-256 digests. The pinned release workflow checks out the selected commit, builds release APKs, moves the `beta` tag to that checked-out HEAD and publishes the APKs. This is a strong provenance chain, but no run ID/log was retrieved and the APK binary could not be downloaded/inspected in this environment.
3. **No compiled upstream app extension class/JAR/AAR or Kotlin module metadata is checked into the pinned source tree.** The existing JARs are dependencies (Cronet, Rhino engine, HtmlUnit CoreJS and Gradle wrapper), not compiled `JsExtensions`/app extension output.
4. **Badcat has a CI step designed to produce host ABI evidence** using `javap -p -s -v` on `RhinoSafeSourceJavaExtensions.class` and upload it as `legado-host-jvm-abi` for 14 days. The workflow definition is present, but the available status/PR-run lookups returned no status or run ID for the audited commits, so no such artifact was retrieved. This step covers the host bridge only, not upstream Legado classes.
5. **Exact JVM ABI remains unverified.** No authentic upstream class/DEX or host `javap` output was inspected. Do not infer descriptors, generated overloads, bridges, suspend continuations or runtime annotation inheritance from Kotlin source.

## 2. Declaration-count reconciliation

### 2.1 Pinned upstream source

- File: `app/src/main/java/io/legado/app/help/JsExtensions.kt`
- Blob: `f6808edd2a0d312e8bd13649935dea444cb82581`
- File length: 1,390 lines.
- Interface declaration begins at line 125 and ends at line 1,389.
- Top-level helper: `internal fun parseJsRequestHeaders(...)` at line 98.

A lexical scan was performed against the exact blob. It skips line comments, nested block comments, normal/character/triple-quoted strings, and counts the Kotlin `fun` keyword as a token with identifier boundaries. Brace depth distinguishes top-level declarations from direct interface members.

| Count scope | Count | Interpretation |
|---|---:|---|
| All `fun` tokens in the file | 122 | File-wide declarations; includes private members and the top-level helper |
| Direct members in `JsExtensions` | 121 | The interface body only |
| Public direct interface members | 118 | Actual public source declaration count, overloads counted |
| Private direct interface helpers | 3 | `canOpenSourceUi`, `functionThisObj`, `sourceConcurrencyKey` |
| Top-level non-public helper | 1 | `parseJsRequestHeaders`, declared `internal` |

This accounts for the four-count difference exactly: 122 minus 118 is the three private interface helpers plus one top-level internal helper. The 122 historical number therefore matches a file-wide function count, whereas 118 is the public interface-member count. Because the original Runtime-1.2 counter/script was not retained, the exact historical counting implementation cannot be independently authenticated; the discrepancy is now narrowed to scope rather than four missing public methods.

### 2.2 Inheritance and other JS extension scopes

| Scope | Source blob | Direct public declarations | Notes |
|---|---|---:|---|
| `JsExtensions` | `f6808edd2a0d312e8bd13649935dea444cb82581` | 118 | Direct public declarations; 3 private helpers excluded |
| `JsEncodeUtils` | `c75b61650db2abdd64a09d2bd8340ef14c2947b3` | 30 | Inherited by `JsExtensions`; 27 distinct names |
| `RegexJsExtensions` | `b01d0bbc6b606832b390723269e4dbcbb834de69` | 6 | Separate replacement helper |
| `WebJsExtensions` | `dd6532396b8f02e37376885aa2c6c1e430af1ad9` | 13 | Separate WebView receiver |
| `SourceLoginJsExtensions` | `a14f01b3774998d09619a79ce977fc593b6a28f1` | 6 | Separate login receiver |
| `RssJsExtensions` | `8e01035e8d5802ed931ac46050cd45cfb0254701` | 17 | Separate RSS receiver; also implements inherited JS methods |

The direct source declaration count for the inherited `JsExtensions` surface is 148 (118 + 30), before compiler lowering, synthetic methods or runtime wrapper behavior. This is still not a JVM method count.

### 2.3 Checked-in source inventory script

- File: `.github/scripts/legado-js-abi-inventory.py`
- Blob: `cb665e60da7a0a6d263b2e909a6d717c6689fdb1`

The script explicitly describes itself as a Kotlin source declaration inventory, not a JVM descriptor proof. Its `collect_scope` parser finds a class/interface declaration and scans four-space-indented member declarations with a regex. It separately adds `JsEncodeUtils` because `JsExtensions` inherits it. Its `UPSTREAM_SCOPES` includes `JsExtensions`, `JsEncodeUtils`, `RegexJsExtensions`, `WebJsExtensions` and `SourceLoginJsExtensions`, but does not list `RssJsExtensions`.

Limitations:
- This is a line-oriented regular-expression parser, not Kotlin PSI/compiler analysis.
- It does not derive JVM descriptors, inherited bytecode methods, compiler-generated overloads/default bridges, synthetic methods or suspend continuation signatures.
- It does not provide a compiler-resolved method graph.
- Its declaration signatures should not be confused with Rhino- or WebView-visible method sets.

## 3. Upstream artifact inventory

Pinned recursive Git tree:
- 3,298 entries, `truncated=false`.
- 11 paths matching `.class/.jar/.aar/.apk/.aab/.dex/.kotlin_module/.kotlin_metadata/.klib/.pom/.module`.
- No app extension class files, app AAR, APK, DEX, Kotlin module metadata or compiled runtime artifact is checked into the tree.

### 3.1 All matching checked-in artifacts

| Path | Git blob | Size (bytes) | Classification |
|---|---|---:|---|
| `app/cronetlib/cronet_api.jar` | `b86c836222b8fa7fbff4746dbabb447ebc71dfd7` | 199,848 | Cronet dependency |
| `app/cronetlib/cronet_impl_common_java.jar` | `1ea77137a8d846e9bb07e8cdb754e12c4cb742f1` | 210,814 | Cronet dependency |
| `app/cronetlib/cronet_impl_native_java.jar` | `b8ce5409b34301b101c75ad03c73bf7cb2ef3b72` | 1,003,550 | Cronet dependency |
| `app/cronetlib/cronet_impl_native_sentinel_java.jar` | `520e8002262d316e3343e06de99981de7f2ecaac` | 563 | Cronet dependency |
| `app/cronetlib/cronet_impl_platform_java.jar` | `37209b06e7d825f574ec8dec6941132e14047b5b` | 80,638 | Cronet dependency |
| `app/cronetlib/cronet_shared_java.jar` | `c84a9b76b34b7185629e31f0e66d17aab7c29d12` | 3,405,949 | Cronet dependency |
| `app/cronetlib/httpengine_native_provider_java.jar` | `ead5919081db0f531393ef0545d74ed567785faa` | 88,950 | Cronet dependency |
| `gradle/wrapper/gradle-wrapper.jar` | `f6b961fd5a86aa5fbfe90f707c3138408be7c718` | 54,329 | Gradle wrapper |
| `modules/rhino/lib/rhino-1.7.14.jar` | `3c4e55ca3ce6d23f112d693f6cd5d5d4c9659351` | 1,442,544 | Rhino engine dependency |
| `third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.jar` | `d240cdd66f5269ed33da53cde7e4e690cee6e162` | 1,900,654 | HtmlUnit CoreJS engine dependency |
| `third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.pom` | `ec4125a89902f4ade16a07d8fc201150e92c3e8c` | 987 | Dependency metadata, not compiled app code |

None of these files contains the compiled application extension classes as a published artifact. The Rhino and HtmlUnit JARs provide the JS engine/dependency implementation, not the Legado application-side `JsExtensions` or `BaseSource` classes.

### 3.2 Release APK associated with the pinned commit

GitHub release metadata:
- Release: [Legado beta](https://github.com/LegadoTeam/legado/releases/tag/beta)
- Release ID: `368765927`
- Tag: `beta`
- Tag ref object type: `commit`
- Tag target: `330e606c4d57e08adc056ff8ab7a4132c784da9a` (exact pinned SHA)
- Release assets were created on 2026-11-01 around 09:12 UTC.

| Asset | Asset ID | Size (bytes) | GitHub-provided SHA-256 | Inspection status |
|---|---:|---:|---|---|
| `legado_app_3.26110117_universal_release_vc38521.apk` | 697843833 | 27,857,704 | `31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3` | Metadata verified; APK bytes not retrieved |
| `legado_app_3.26110117_universal_releaseA_vc38521.apk` | 697843874 | 27,857,698 | `e9752d4d5448d0bcc6fb5d579b3344417eef2fc683c5c4c5f651ac2a4325523b` | Metadata verified; APK bytes not retrieved |
| `legado_app_3.26110117_release_vc38521.apk` | 697843935 | 21,906,878 | `e5cfb391939d1f68ae846527503d115a10ed5cb94ba83ccf1ec58e1ab6566f3d` | Metadata verified; APK bytes not retrieved |
| `legado_app_3.26110117_releaseA_vc38521.apk` | 697843967 | 21,906,884 | `67025136ef9f1c43c036944749e36afb84ad15714104e18afdb557955356df86` | Metadata verified; APK bytes not retrieved |

The digest values above are the GitHub Release API's `digest` fields; they were not recomputed locally.

### 3.3 Release workflow provenance

Pinned workflow/config blobs:

| File | Blob |
|---|---|
| `.github/workflows/TestRelease.yml` | `6444200fa65edb8a871fab40954a1ccd7429d8d5` |
| `app/build.gradle` | `eb26ab098e63401a3c506fbd21ca8fdc95616aa7` |
| `gradle/libs.versions.toml` | `88170041bfea4b98b3269955f5db3704691134aa` |
| `gradle/wrapper/gradle-wrapper.properties` | `c33653ad28b591972da59f9fac6da1571b5f636e` |

Direct workflow/config evidence:
- `TestRelease.yml` triggers on pushes to `master` and manual dispatch.
- The prepare job checks out `inputs.commit_sha || github.sha`; the build job checks out the exact commit output by prepare.
- The build job configures Temurin JDK 21 and runs `:app:assembleRelease`, collects arm and universal APKs, validates the expected APK set and uploads the APK artifacts.
- The publish job checks out the same selected commit, downloads the build artifacts, force-updates `beta` to `git rev-parse HEAD`, and publishes those APKs to the `beta` release.
- The `beta` tag currently resolves to the exact pinned SHA, and the release assets exist on that release.

Declared toolchain at the pinned commit:
- Kotlin Gradle plugin: `2.4.20`
- Android Gradle Plugin: `8.13.2`
- Gradle wrapper: `8.14.4`
- JDK: `21`

This gives a strong source/tag/workflow/release association for the APK candidate. However, the exact workflow run ID and logs were not retrieved; therefore this audit does not claim a fully independently verified run record or a locally validated APK hash. The APK binary could not be fetched through the available GitHub/browser artifact actions in this session, so no DEX class inventory, JVM descriptor extraction or embedded Kotlin metadata inspection was performed.

### 3.4 Maven/package publication

The pinned root, app, `modules/book` and `modules/rhino` build scripts/settings were inspected for `maven-publish`, `publishing`, `MavenPublication`, publish tasks and explicit artifact coordinates. No application/runtime publication configuration was found. The checked-in HtmlUnit POM describes the local JS-engine dependency only; it is not a publication of Legado app extension classes.

## 4. Badcat host artifact inventory

Registered host baseline: `06712ecfa821222fefdb764ea315a015aac9cbc7`.

- Recursive tree: 488 entries, `truncated=false`.
- One matching path: `third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.pom` (same dependency metadata blob as upstream).
- No checked-in `.class`, `.jar`, `.aar`, `.apk`, `.aab`, `.dex`, `.kotlin_module` or Kotlin metadata artifact.
- No GitHub Releases were published by the Badcat repository at audit time.
- Host project declares AGP `8.13.2`, Kotlin `2.4.20`, JVM target 21. Its CI configures JDK 21 and Gradle `8.14.4`.

### 4.1 Existing CI ABI capture configuration

Workflow:
- `.github/workflows/android-build.yml`
- Blob: `671d28f257db3d36ba767729fed83b0e24f12573`

The workflow is configured to:
1. Build/test the host modules and assemble `:app:assembleDebug`.
2. Locate `RhinoSafeSourceJavaExtensions.class`.
3. Run `javap -p -s -v` against that class.
4. Save `RhinoSafeSourceJavaExtensions.javap.txt` and a SHA-256 manifest.
5. Upload the result as `legado-host-jvm-abi` with 14-day retention.

This would provide useful authentic **host** JVM descriptor/annotation evidence if a successful or partially successful run artifact exists. It does not compile or capture upstream `JsExtensions`, `JsEncodeUtils`, `WebJsExtensions` or `SourceLoginJsExtensions` classes.

For commits `06712ecfa821222fefdb764ea315a015aac9cbc7` and registration commit `5cc570d406af979a209e1f6ac1d1f60f9c1fd25b`:
- Combined status lookup returned no status entries.
- The available commit-workflow-run lookup returned zero PR-triggered runs; that connector lookup is limited to PR-triggered runs and does not enumerate push-triggered runs.
- No workflow run ID or artifact ID was available to retrieve.
- Therefore no claim is made that the workflow did not run; the status is **not retrievable / unverified**, not “no run exists”.

## 5. Machine-readable artifact inventory

```csv
repository,artifact,kind,source_commit,toolchain,sha256_or_git_blob,size_bytes,provenance_status,abi_inspection
LegadoTeam/legado,legado_app_3.26110117_universal_release_vc38521.apk,release-apk,330e606c4d57e08adc056ff8ab7a4132c784da9a,"JDK21;Gradle8.14.4;Kotlin2.4.20;AGP8.13.2",31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3,27857704,tag-and-workflow-associated;run-id-not-retrieved,binary-not-inspected
LegadoTeam/legado,legado_app_3.26110117_universal_releaseA_vc38521.apk,release-apk,330e606c4d57e08adc056ff8ab7a4132c784da9a,"JDK21;Gradle8.14.4;Kotlin2.4.20;AGP8.13.2",e9752d4d5448d0bcc6fb5d579b3344417eef2fc683c5c4c5f651ac2a4325523b,27857698,tag-and-workflow-associated;run-id-not-retrieved,binary-not-inspected
LegadoTeam/legado,modules/rhino/lib/rhino-1.7.14.jar,dependency-jar,330e606c4d57e08adc056ff8ab7a4132c784da9a,,3c4e55ca3ce6d23f112d693f6cd5d5d4c9659351,1442544,checked-in-source-blob,not-app-extension-classes
LegadoTeam/legado,third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.jar,dependency-jar,330e606c4d57e08adc056ff8ab7a4132c784da9a,,d240cdd66f5269ed33da53cde7e4e690cee6e162,1900654,checked-in-source-blob,not-app-extension-classes
xkszds/badcat-legado-client,legado-host-jvm-abi,actions-artifact,06712ecfa821222fefdb764ea315a015aac9cbc7,"JDK21;Gradle8.14.4;Kotlin2.4.20;AGP8.13.2",,,"workflow-configured;run-id-and-artifact-not-retrieved",not-inspected
```

## 6. Remaining gaps and decision

1. **Upstream APK bytes not inspected.** Need the exact official beta APK bytes, verify their SHA-256 against the GitHub Release digest, then inspect `classes*.dex` for the extension classes, descriptors, annotations and embedded Kotlin metadata. APK/DEX is the next available authentic artifact path; do not infer contents from its filename.
2. **Host ABI artifact not retrieved.** Need the GitHub Actions push-run ID and artifact ID for the Badcat baseline (or a supplied downloaded `legado-host-jvm-abi` artifact). Its workflow configuration already specifies the `javap -p -s -v` capture.
3. **Upstream compiled class artifact is still absent as a standalone JAR/AAR.** The official APK may provide DEX-level ABI evidence, but it is not equivalent to a raw JVM `.class` file for every compiler metadata question.
4. **Compiler metadata is declared, not execution-proven.** Kotlin/AGP/Gradle/JDK versions are read from pinned build configuration. The specific release run log/toolchain provenance was not retrieved.
5. **Historical count method is not preserved.** The 122 count is now exactly reproducible as file-wide `fun` tokens; 118 is the public interface count. This resolves the numerical scope discrepancy, but not the provenance of the old inventory command.

**Decision: NO-GO for a new compiler experiment at this point.** Existing official APK and configured host ABI artifact are the preferable evidence sources and should be inspected first. A fresh compilation is unnecessary until those existing artifacts are exhausted or proven insufficient. Runtime-1.5 remains **PARTIAL / ACCEPTANCE NOT MET**; G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.

## 7. Validation and change log

- Pinned upstream and host recursive tree metadata inspected; no source code changed.
- Release metadata, tag ref, build workflow and declared toolchain inspected.
- Declaration count reproduced by lexical token/depth scan; no compiler was invoked.
- No APK was downloaded or unpacked; no DEX/JVM descriptors were inspected.
- No compilation, Gradle task, tests, CI run, APK build, device test or live-source verification was performed by this audit.
- No production source, test, Gradle, workflow, database/schema/migration, persistence, UI or Reader files changed.
- Only this report and the Runtime category checklist result are authorized outputs.


## 8. 2026-11-21 follow-up — retrieved host CI ABI artifact

The previously missing Badcat host artifact has now been found and downloaded from the exact Runtime-1.5 result HEAD.

- Branch: `integration/legado-upstream-runtime`
- Exact workflow run: [Android debug build #3028](https://github.com/xkszds/badcat-legado-client/actions/runs/44787339966)
- Run ID: `44787339966`; event: `push`; conclusion: `success`; head SHA: `25dad001a1ae99fcc0763cfd7eb983ab584e422b`.
- Successful job: `assemble-debug` (`136465872354`). The job steps report success for contract unit tests, authentic Legado runtime model contract tests, G2d stage-1 compiler probe, `Capture compiled host JavaScript bridge JVM ABI`, `Upload compiled host JavaScript bridge JVM ABI`, and `Assemble debug APK`.
- Artifact: `legado-host-jvm-abi`; artifact ID `14653689795`; size 165,211 bytes; created 2026-11-20T17:56:26Z; expires 2026-12-04T17:56:25Z; GitHub digest `sha256:707fb4f19f1e70c93f3938c158e884a8b65b5bee6ec6af8530a1c96b0e8a6c4f`.
- Downloaded archive SHA-256 recomputed locally: `707fb4f19f1e70c93f3938c158e884a8b65b5bee6ec6af8530a1c96b0e8a6c4f` (exact match).
- Archive contents: `RhinoSafeSourceJavaExtensions.javap.txt` (1,361,755 bytes) and `SHA256SUMS.txt`. The embedded manifest SHA-256 for the javap report is `ee881435ef053c96b7f09b25abafe045e7c1ea7040892408a1e164317284b60c`; locally recomputed file hash matches exactly.
- Classfile header: `app.badcat.client.runtime.RhinoSafeSourceJavaExtensions`, classfile major version 65 (Java 21), 70 fields and 234 methods. The dump contains JVM descriptors, generic signatures, bytecode, parameter/return annotations and one `kotlin.Metadata` annotation. Fourteen methods carry `android.webkit.JavascriptInterface` annotations.

**Scope limitation:** this is authentic compiled evidence for the Badcat host bridge at the exact Badcat HEAD above. It is not an upstream Legado `JsExtensions`/subinterface class artifact and does not establish the upstream Kotlin/JVM ABI, Rhino overload dispatch, or upstream WebView annotation inheritance. It closes the *retrieval* blocker for the host artifact only, not the upstream APK/DEX blocker.

The same successful CI run also executed the existing workflow's G2d stage-1 compile probe, contract tests, model contract tests and Debug APK assembly. This is recorded as an observed CI fact; it is not a claim that G2d/G2d full runtime, G3/G4, Release APK or device acceptance passed.

The official Legado beta APK binary remains unavailable through the current download path. Direct retrieval of `legado_app_3.26110117_universal_release_vc38521.apk` failed; its GitHub release metadata digest has not been locally recomputed and no DEX inspection has been performed.

**Updated decision:** host artifact retrieval is complete and host JVM ABI evidence is now available. Runtime-1.5 remains **PARTIAL / ACCEPTANCE NOT MET** until the official pinned-upstream APK is obtained and inspected and the upstream-specific ABI gaps are addressed. No new build or compiler experiment was initiated in this follow-up.


## 9. 2026-11-21 follow-up — official pinned APK and DEX inspection

### 9.1 APK provenance and integrity

- User supplied `legado_app_3.26110117_universal_release_vc38521.apk` (27,857,704 bytes).
- Locally recomputed SHA-256: `31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3`; exact match with the GitHub Release API digest recorded in section 3.2.
- APK `META-INF/version-control-info.textproto` embeds revision `330e606c4d57e08adc056ff8ab7a4132c784da9a`, exactly matching the pinned upstream SHA.
- APK `META-INF/com/android/build/gradle/app-metadata.properties` declares AGP `8.13.2`.
- APK contains `classes.dex` (9,412,728 bytes), `classes2.dex` (714,716 bytes), and `classes3.dex` (10,780,088 bytes). All three DEX files were parsed. The parser enumerated 19,663 class definitions and 137,238 method IDs across the three DEX files.

### 9.2 DEX class and descriptor findings

Authentic class definitions found in the pinned APK include:

| Class | DEX | Defined methods | Public methods | Method annotation entries | `JavascriptInterface` methods |
|---|---|---:|---:|---:|---:|
| `io.legado.app.help.JsExtensions` | classes.dex | 148 | 148 | 56 | 54 |
| `io.legado.app.help.RegexJsExtensions` | classes.dex | 40 | 36 | not separately summarized | not separately summarized |
| `io.legado.app.help.webView.WebJsExtensions` | classes.dex | 67 | 54 | 12 | 12 |
| `io.legado.app.ui.login.SourceLoginJsExtensions` | classes3.dex | 12 | 12 | 1 | 0 |
| `io.legado.app.ui.rss.read.RssJsExtensions` | classes3.dex | 190 | 185 | 71 | 64 |
| `io.legado.app.data.entities.BaseSource` | classes.dex | 71 | 71 | class-level MemberClasses annotation present | not used as a WebView count |
| `io.legado.app.model.analyzeRule.AnalyzeRule` | classes3.dex | 213 | — | class-level Keep annotation | — |
| `io.legado.app.model.analyzeRule.AnalyzeUrl` | classes3.dex | 209 | — | class-level Keep/SuppressLint/MemberClasses annotations | — |

The DEX method tables expose concrete parameter and return descriptors for every listed defined method. For example, `JsExtensions` contains overloaded `ajax` descriptors `(Ljava/lang/Object;)Ljava/lang/String;` and `(Ljava/lang/Object;Ljava/lang/Long;)Ljava/lang/String;`, and overloaded `base64Decode` descriptors with String, int and String parameters. The complete descriptor inventory is derived directly from DEX method_ids and class_data encoded method lists; it is not inferred from Kotlin source signatures.

Annotation directory inspection distinguishes class annotations from method annotations. The `JsExtensions` interface has 54 methods explicitly carrying `android.webkit.JavascriptInterface`; `WebJsExtensions` has 12; `RssJsExtensions` has 64. `SourceLoginJsExtensions` has no method carrying that annotation in this DEX class. These are direct DEX annotations only; they do not by themselves establish runtime registration or inherited exposure through a WebView object.

### 9.3 Kotlin metadata and limitations

- No DEX string-pool entry for `Lkotlin/Metadata;` or `kotlin/Metadata` was found in any of the three DEX files. Therefore this APK does not provide retained Kotlin Metadata annotation evidence for reconstructing Kotlin-level declarations/default-argument semantics.
- DEX does provide authentic compiled method descriptors and direct annotation placement, including synthetic/default bridge methods where retained in the class data. It does not preserve the original JVM `.class` file representation or all Kotlin compiler metadata.
- A custom structural DEX parser was used to read DEX string/type/proto/method/class tables, class_data and annotation directories. It does not decompile method bodies into source, emulate Rhino overload selection, prove dynamic JavaScript calls, or establish all runtime registration paths.
- R8/DEX method counts must not be compared one-to-one with Kotlin source `fun` counts: inherited members, overloads, generated methods, synthetic accessors, default bridges, and shrinker transformations affect the inventories.

### 9.4 Combined host and upstream evidence status

- The host `legado-host-jvm-abi` artifact retrieval and local digest verification are complete (section 8). It supplies authentic host JVM class-file descriptors and annotations for `RhinoSafeSourceJavaExtensions` at Badcat HEAD `25dad001a1ae99fcc0763cfd7eb983ab584e422b`.
- The official pinned APK retrieval, local digest verification, embedded source-revision check, and DEX class/descriptor/annotation inspection are now complete.
- The two artifacts describe different sides: host JVM class-file ABI versus upstream Android DEX ABI. They must not be conflated as a direct binary-compatible pair. A descriptor-level crosswalk and actual Rhino/WebView dispatch behavior still require method-by-method semantic analysis and runtime evidence.

**Updated decision:** the two artifact-acquisition blockers are closed. Runtime-1.5 is still **PARTIAL / ACCEPTANCE NOT MET** because the DEX evidence does not resolve Rhino overload selection, full WebView registration/inheritance semantics, Kotlin metadata/default semantics removed from DEX, all dynamic source entry points/effects, or the complete compatibility call graph. The next permissible step is to update the registered report/checklist with this evidence and then propose a separately preregistered bounded semantic/dispatch audit if needed. No new compiler experiment, build, test, production code change, DB/schema change, UI/Reader change, or G3/G4 work was performed in this follow-up.
