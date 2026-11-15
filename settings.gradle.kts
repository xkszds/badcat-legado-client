pluginManagement {
    repositories {
        google()
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
