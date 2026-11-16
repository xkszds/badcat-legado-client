plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    // Match the pinned Legado network client baseline.
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    // Match the pinned Legado baseline; do not upgrade independently.
    implementation("org.jsoup:jsoup:1.23.2")
    // Match the pinned Legado parser dependency baseline.
    implementation("cn.wanghaomiao:JsoupXpath:2.5.3")
    implementation("com.jayway.jsonpath:json-path:3.0.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}
