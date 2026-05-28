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
    "analysis-orchestrator-service",
    "analysis-store-service",
    "btm-generation-service",
    "cli-client",
    "forensic-gateway-service",
    "forensic-ingestion-service",
    "graph-replay-service",
    "ingestion-service",
    "java-ast-analysis-service",
    "java-parser-analysis-service",
    "joern-analysis-service",
    "joern-cpg-analysis-service",
    "observability-stack",
    "query-report-api-service",
    "report-generation-service",
    "repository-analysis-service",
    "repository-source-service",
    "testbed",
)
