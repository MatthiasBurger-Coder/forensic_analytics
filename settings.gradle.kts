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
    "forensic-analytics-engine",
    "forensic-analytics-logging",
    "forensic-analytics-observability",
    "forensic-analytics-adapter-repository-source",
    "forensic-analytics-adapter-javaparser",
    "forensic-analytics-adapter-joern-docker",
    "forensic-analytics-cli",
    "forensic-analytics-testbed",
    "forensic-analytics-persistence",
    "forensic-analytics-ingestion-grpc",
    "forensic-analytics-ingestion-request",
    "forensic-analytics-rest",
    "forensic-analytics-bootstrap",
    "forensic-analytics-boot-app",
)
