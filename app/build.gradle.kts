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
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-integration"
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

dependencies {
    implementation(project(":integration-contract"))
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
}
