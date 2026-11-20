plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "io.legado.runtime"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets.getByName("main").java {
        // Compile the actual pinned Legado sources from the git submodule.
        // This first gate deliberately includes only the six source-rule models
        // and their exact Gson contract; lifecycle/runtime closure is a later gate.
        srcDir("$rootDir/upstream/legado/app/src/main/java")
        include(
            "io/legado/app/data/entities/rule/BookListRule.kt",
            "io/legado/app/data/entities/rule/ExploreRule.kt",
            "io/legado/app/data/entities/rule/SearchRule.kt",
            "io/legado/app/data/entities/rule/BookInfoRule.kt",
            "io/legado/app/data/entities/rule/TocRule.kt",
            "io/legado/app/data/entities/rule/ContentRule.kt",
            "io/legado/app/data/entities/rule/ReviewRule.kt",
            "io/legado/app/utils/GsonExtensions.kt",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("androidx.annotation:annotation:1.10.0")
    testImplementation("junit:junit:4.13.2")
}
