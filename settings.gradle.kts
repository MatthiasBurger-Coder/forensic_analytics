pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "forensic-analytics"

include(
    "forensic-analytics-domain",
    "forensic-analytics-application",
    "forensic-analytics-persistence",
    "forensic-analytics-ingestion-grpc",
    "forensic-analytics-bootstrap",
)
