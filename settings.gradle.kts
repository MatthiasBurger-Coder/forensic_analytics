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
    "services:btm-generation-service",
    "services:joern-cpg-analysis-service",
    "services:joern-analysis-service",
    "services:java-parser-analysis-service",
    "services:java-ast-analysis-service",
    "services:repository-source-service",
    "services:repository-analysis-service",
    "services:analysis-orchestrator-service",
    "services:analysis-store-service",
    "services:ingestion-service",
    "services:forensic-ingestion-service",
    "services:query-report-api-service",
    "services:cli-client",
    "services:observability-stack",
    "services:testbed",
    "services:forensic-gateway-service",
)
