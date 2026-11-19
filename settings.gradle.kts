pluginManagement {
    repositories {
        google()
        // Maven Central mirror for the pinned native archive AAR; keep the
        // fallback scoped to this module so other dependencies retain normal resolution.
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
            content {
                includeModule("me.zhanghai.android.libarchive", "library")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Legado pins a custom Rhino fork that is not published to Maven Central.
        maven { url = uri("$rootDir/third_party/maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "BadcatLegadoClient"
include(":app")
include(":integration-contract")
include(":legado-rhino")
