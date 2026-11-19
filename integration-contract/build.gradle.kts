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
tasks.named<org.gradle.api.tasks.bundling.Jar>("jar") {
    from(pinnedLegado.output)
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
