plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.badcat.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.badcat.client"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-integration"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(project(":integration-contract"))
}
