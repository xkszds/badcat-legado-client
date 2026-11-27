plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "io.legado.runtime.poc"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    // Real upstream external dependencies; unresolved app-internal imports are intentionally not stubbed.
    implementation("androidx.room:room-runtime:2.8.4")
    testAnnotationProcessor("androidx.room:room-compiler:2.8.4")
    implementation("androidx.annotation:annotation:1.10.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("cn.hutool:hutool-crypto:5.8.22")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation(project(":legado-rhino"))

    // Quarantined storage PoC only; these dependencies are not exposed to production modules.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}

// G2d stage 1: compile an independently bounded, authentic upstream source group.
// The previous two-file BaseSource/BookSource failure remains the control evidence
// (G2c artifact 14635151778); these roots are reintroduced only in a later increment.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name == "compileDebugKotlin" || name == "compileReleaseKotlin") {
        source(fileTree("$rootDir/upstream/legado/app/src/main/java") {
            include(
                "io/legado/app/data/entities/rule/BookListRule.kt",
                "io/legado/app/data/entities/rule/ExploreRule.kt",
                "io/legado/app/data/entities/rule/SearchRule.kt",
                "io/legado/app/data/entities/rule/BookInfoRule.kt",
                "io/legado/app/data/entities/rule/TocRule.kt",
                "io/legado/app/data/entities/rule/ContentRule.kt",
                "io/legado/app/data/entities/rule/ReviewRule.kt",
                "io/legado/app/utils/GsonExtensions.kt",
                "io/legado/app/constant/AppPattern.kt",
                "io/legado/app/constant/BookSourceType.kt",
                "io/legado/app/exception/NoStackTraceException.kt",
                "io/legado/app/utils/MapExtensions.kt",
            )
        })
    }
}
