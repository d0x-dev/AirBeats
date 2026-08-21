@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.10.0")
}


rootProject.name = "AirBeats"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":material-color-utilities")
include(":jossredconnect")
include(":betterlyrics")
include(":shazamkit")

include(":shazamkit")

