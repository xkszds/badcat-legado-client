import org.gradle.api.tasks.testing.Test
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "app.badcat.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.badcat.client"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-integration"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            // Hutool crypto and core both ship this non-runtime JAR index metadata.
            excludes += "/META-INF/INDEX.LIST"
            // Bouncy Castle modules embed the identical root LICENSE.md in every JAR.
            pickFirsts += "/META-INF/LICENSE.md"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Temporary Runtime validation profile: keep Fourth-category tests out of the Third-category lane.
// Omit -PbadcatThirdCategoryTestProfile=true to run every test again for Fourth-category validation.
val badcatThirdCategoryTestProfile =
    providers.gradleProperty("badcatThirdCategoryTestProfile").orNull == "true"

tasks.withType<Test>().configureEach {
    if (badcatThirdCategoryTestProfile) {
        filter {
            excludeTestsMatching("app.badcat.client.BadcatPurificationModelsTest")
            excludeTestsMatching("app.badcat.client.BadcatAdMarkCatalogTest")
            excludeTestsMatching("app.badcat.client.runtime.BadcatWebViewPurificationAdapterTest")
        }
    }
}

configurations.configureEach {
    if (name == "debugUnitTestRuntimeClasspath") {
        // Production and test compilation use the Android AAR; unit-test execution substitutes
        // the same OkHttp release's JVM artifact so Robolectric can load its packaged PSL resource.
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
}

// Verify the Android runtime variant itself, not only integration-contract:jar.
// This catches project-variant selection that omits custom pinned upstream outputs.
val verifyPinnedLegadoRuleAnalyzerRuntimeClasspath =
    tasks.register("verifyPinnedLegadoRuleAnalyzerRuntimeClasspath") {
        dependsOn(":integration-contract:verifyPinnedLegadoRuntimeJar")
        doLast {
            val classEntry = "io/legado/app/model/analyzeRule/RuleAnalyzer.class"
            // Resolve the Android classpath as the same transformed class-JAR artifact
            // type consumed by D8. Reading Configuration.files directly leaves AGP's
            // multiple debugRuntimeElements artifact variants ambiguous.
            val runtimeFiles = configurations.getByName("debugRuntimeClasspath")
                .incoming.artifactView {
                    attributes {
                        attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                            "android-classes-jar",
                        )
                    }
                }
                .artifacts.artifactFiles.files
            val containing = runtimeFiles.filter { artifact ->
                when {
                    artifact.isDirectory -> artifact.resolve(classEntry).isFile
                    artifact.isFile && artifact.extension.equals("jar", ignoreCase = true) ->
                        ZipFile(artifact).use { it.getEntry(classEntry) != null }
                    else -> false
                }
            }
            check(containing.isNotEmpty()) {
                val projectArtifacts = runtimeFiles.filter {
                    it.name.contains("integration-contract", ignoreCase = true) ||
                        it.path.contains("integration-contract", ignoreCase = true)
                }.joinToString { it.absolutePath }
                "Android debugRuntimeClasspath is missing $classEntry; " +
                    "integration-contract artifacts: [$projectArtifacts]"
            }
            containing.forEach {
                logger.lifecycle("PINNED_RULE_ANALYZER_RUNTIME_CLASSPATH=${it.absolutePath}")
            }
            val report = layout.buildDirectory
                .file("reports/legado-runtime-3.2f.46/runtime-classpath.json")
                .get().asFile
            report.parentFile.mkdirs()
            report.writeText(
                groovy.json.JsonOutput.toJson(
                    mapOf(
                        "task" to "Runtime-3.2f.46",
                        "configuration" to "debugRuntimeClasspath",
                        "artifactType" to "android-classes-jar",
                        "classEntry" to classEntry,
                        "status" to "pass",
                        "artifacts" to containing.map { it.absolutePath },
                    )
                ) + "\\n"
            )
            logger.lifecycle("PINNED_RULE_ANALYZER_REPORT=${report.absolutePath}")
        }
    }

dependencies {
    implementation(project(":integration-contract"))
    // Compile and package the authentic pinned Legado rule-model slice; runtime orchestration is not wired yet.
    implementation(project(":legado-runtime"))
    implementation(project(":legado-rhino"))
    // The login-form host validates JSON state and action envelopes with the same parser baseline.
    implementation("com.google.code.gson:gson:2.14.0")
    // Pinned Legado rule models use the upstream Flexbox style contract.
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    // Pinned Legado crypto runtime dependencies (Hutool 5.8.22 / Bouncy Castle 1.86).
    implementation("cn.hutool:hutool-crypto:5.8.22")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    // Parser dependencies match the pinned upstream Legado source revision.
    implementation("org.jsoup:jsoup:1.23.2")
    implementation("cn.wanghaomiao:JsoupXpath:2.5.3")
    implementation("com.jayway.jsonpath:json-path:3.0.0")
    implementation("androidx.annotation:annotation:1.10.0")
    // The app hosts coroutine-based source catalog operations and background engine calls.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // The Android host supplies a persistent CookieJar to the shared source HTTP client.
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    // Pinned upstream Legado uses libarchive for RAR and 7z source helpers.
    implementation("me.zhanghai.android.libarchive:library:1.1.6")
    // Legado uses this dictionary-backed converter for its @js t2s/s2t helpers.
    implementation("com.github.liuyueyi:quick-transfer-core:0.2.17")
    testImplementation("junit:junit:4.13.2")
    // Android Base64 behavior in crypto bridge tests must run against real Android shadows.
    testImplementation("org.robolectric:robolectric:4.14.1")
    // JVM unit tests need OkHttp's classpath-backed official PSL resource, not Android AAR assets.
    testRuntimeOnly("com.squareup.okhttp3:okhttp-jvm:5.4.0")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}


// Explicit Stage-0 guard: ensure pinned Legado rule/Gson declarations have one
// compiler owner across the app's own Debug output and its Android runtime classpath.
// This task is intentionally standalone and is not attached to check/assemble/CI.
val verifyNoDuplicatePinnedLegadoRuleClasses =
    tasks.register("verifyNoDuplicatePinnedLegadoRuleClasses") {
        dependsOn("compileDebugKotlin")
        doLast {
            val expectedClasses = listOf(
                "io/legado/app/data/entities/rule/BookListRule.class",
                "io/legado/app/data/entities/rule/BookInfoRule.class",
                "io/legado/app/data/entities/rule/ContentRule.class",
                "io/legado/app/data/entities/rule/ExploreRule.class",
                "io/legado/app/data/entities/rule/ReviewRule.class",
                "io/legado/app/data/entities/rule/SearchRule.class",
                "io/legado/app/data/entities/rule/TocRule.class",
                "io/legado/app/utils/GsonExtensionsKt.class",
                "io/legado/app/utils/IntJsonDeserializer.class",
                "io/legado/app/utils/StringJsonDeserializer.class",
                "io/legado/app/utils/MapDeserializerDoubleAsIntFix.class",
                "io/legado/app/utils/LocalDateTypeAdapter.class",
            )
            val owners = expectedClasses.associateWith { mutableListOf<String>() }

            val appKotlinClasses =
                layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
            expectedClasses.forEach { classEntry ->
                if (appKotlinClasses.resolve(classEntry).isFile) {
                    owners.getValue(classEntry).add(appKotlinClasses.absolutePath)
                }
            }

            val runtimeFiles = configurations.getByName("debugRuntimeClasspath")
                .incoming.artifactView {
                    attributes {
                        attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                            "android-classes-jar",
                        )
                    }
                }
                .artifacts.artifactFiles.files

            runtimeFiles.forEach { artifact ->
                when {
                    artifact.isDirectory -> expectedClasses.forEach { classEntry ->
                        if (artifact.resolve(classEntry).isFile) {
                            owners.getValue(classEntry).add(artifact.absolutePath)
                        }
                    }
                    artifact.isFile && artifact.extension.equals("jar", ignoreCase = true) ->
                        ZipFile(artifact).use { zip ->
                            expectedClasses.forEach { classEntry ->
                                if (zip.getEntry(classEntry) != null) {
                                    owners.getValue(classEntry).add(artifact.absolutePath)
                                }
                            }
                        }
                }
            }

            val missing = owners.filterValues { it.isEmpty() }
            val duplicates = owners.filterValues { it.size > 1 }
            check(missing.isEmpty()) {
                "Pinned Legado rule/Gson classes missing from Debug classpath: " +
                    missing.keys.joinToString()
            }
            check(duplicates.isEmpty()) {
                "Duplicate pinned Legado rule/Gson class definitions: " +
                    duplicates.map { (name, foundPaths) -> "$name => ${foundPaths.joinToString()}" }
                        .joinToString("; ")
            }
            owners.forEach { (name, foundPaths) ->
                logger.lifecycle("PINNED_LEGADO_CLASS_OWNER=$name|${foundPaths.single()}")
            }
            logger.lifecycle("PINNED_LEGADO_DUPLICATE_CLASS_GUARD=PASS")
        }
    }
