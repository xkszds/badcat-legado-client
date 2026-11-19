plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

val pinnedLegado by sourceSets.creating {
    // Keep the host's compatibility-adjusted copies isolated from the upstream
    // source root. Adding the whole upstream tree here caused duplicate FQCNs
    // whenever a pinned file had a platform adaptation.
    java.srcDir("$projectDir/src/pinnedLegado/kotlin")
    // The pinned source directory contains the selected upstream files,
    // including the narrowly platform-adapted XPath/JSONPath copies. Do not
    // also register the upstream app source root: it duplicates those FQCNs.
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
