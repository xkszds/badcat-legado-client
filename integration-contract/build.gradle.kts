import java.util.zip.ZipFile

plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

val pinnedLegado by sourceSets.creating {
    // Compile selected original files from the pinned upstream app tree.
    java.srcDir("$rootDir/upstream/legado/app/src/main/java")
    java.include(
        "io/legado/app/model/analyzeRule/AnalyzeByJSoup.kt",
        "io/legado/app/model/analyzeRule/AnalyzeByRegex.kt",
        "io/legado/app/model/analyzeRule/RuleAnalyzer.kt",
        "io/legado/app/model/analyzeRule/SourceHtmlParser.kt",
    )
    // Add only the platform-adapted upstream files from the isolated host tree.
    // Keeping them out of java.srcDir prevents the upstream originals with the
    // same FQCN from being compiled a second time.
    tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compilePinnedLegadoKotlin") {
        source(fileTree("$projectDir/src/pinnedLegado/kotlin") {
            include(
                "io/legado/app/model/analyzeRule/AnalyzeByJSonPath.kt",
                "io/legado/app/model/analyzeRule/AnalyzeByXPath.kt",
                "io/legado/app/model/analyzeRule/PinnedLegadoJsoupBridge.kt",
                "io/legado/app/model/analyzeRule/PinnedLegadoJsonPathBridge.kt",
                "io/legado/app/model/analyzeRule/PinnedLegadoXPathBridge.kt",
                "io/legado/app/utils/PrintOnDebug.kt",
            )
        })
    }
    // AnalyzeRule's app-coupled lifecycle remains a separate integration task.
}

dependencies {
    add(pinnedLegado.implementationConfigurationName, "androidx.annotation:annotation:1.10.0")
    add(pinnedLegado.implementationConfigurationName, "org.jsoup:jsoup:1.23.2")
    add(pinnedLegado.implementationConfigurationName, "cn.wanghaomiao:JsoupXpath:2.5.3")
    add(pinnedLegado.implementationConfigurationName, "com.jayway.jsonpath:json-path:3.0.0")
    implementation(pinnedLegado.output)
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    // Match the pinned Legado network client baseline.
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    // Match the pinned Legado baseline; do not upgrade independently.
    implementation("org.jsoup:jsoup:1.23.2")
    implementation("androidx.annotation:annotation:1.10.0")
    // Match the pinned Legado parser dependency baseline.
    implementation("cn.wanghaomiao:JsoupXpath:2.5.3")
    implementation("com.jayway.jsonpath:json-path:3.0.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

// The app consumes this module as a project dependency. Package the separately
// compiled pinned Legado classes into the published module JAR as well.
val pinnedLegadoKotlinCompile =
    tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compilePinnedLegadoKotlin")

// Publish the custom upstream Kotlin output as a real main classes directory.
// Android project variants consume SourceSetOutput.classesDirs; registering this
// as output.dir(...) would classify the .class files as resources and leave the
// Android classes-directory variant without the pinned RuleAnalyzer definition.
sourceSets.named("main") {
    (output.classesDirs as org.gradle.api.file.ConfigurableFileCollection)
        .from(pinnedLegadoKotlinCompile.map { it.destinationDirectory })
}

tasks.named<org.gradle.api.tasks.bundling.Jar>("jar") {
    dependsOn(pinnedLegadoKotlinCompile)
    // SourceSetOutput does not reliably expose the custom Kotlin destination to
    // Android's project runtime variant. Add the actual KotlinCompile output
    // directory explicitly so the pinned upstream classes enter the JAR.
    from(pinnedLegadoKotlinCompile.map { it.destinationDirectory })
    from(pinnedLegado.output)
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}

val verifyPinnedLegadoRuntimeJar = tasks.register("verifyPinnedLegadoRuntimeJar") {
    dependsOn(tasks.named("jar"))
    doLast {
        val archive = tasks.named<org.gradle.api.tasks.bundling.Jar>("jar")
            .get().archiveFile.get().asFile
        val requiredClass = "io/legado/app/model/analyzeRule/RuleAnalyzer.class"
        val mainClassDirs = sourceSets.named("main").get().output.classesDirs.files
        val publishedClassDirs = mainClassDirs.filter { it.resolve(requiredClass).isFile }
        check(publishedClassDirs.isNotEmpty()) {
            "Pinned Legado runtime classes are missing $requiredClass from main classesDirs: " +
                mainClassDirs.joinToString()
        }
        logger.lifecycle(
            "PINNED_RULE_ANALYZER_MAIN_CLASSES_DIRS=" +
                publishedClassDirs.joinToString { it.absolutePath }
        )
        ZipFile(archive).use { zip ->
            check(zip.getEntry(requiredClass) != null) {
                "Pinned Legado runtime JAR is missing $requiredClass: $archive"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyPinnedLegadoRuntimeJar)
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
