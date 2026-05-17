# Monorepo Service Build Strategy

## Status

Slice 02 build strategy for planned service roots.

No build files are changed in Slice 02. The existing root Gradle build remains
the only executable repository build until service build files are created and
registered in later slices.

## Decision

The target strategy is root-included Gradle service projects once real service
build files exist.

Rationale:

- the repository already centralizes Java 25 toolchain configuration;
- `QUALITY.md` defines repository-wide Gradle verification with strict
  dependency verification;
- JaCoCo and package coverage policy are already centralized;
- service inclusion can be verified by the same local quality gate;
- independent deployability can be enforced by service-local source,
  configuration, tests, Dockerfiles and dependency rules without duplicating
  Gradle wrapper and verification metadata per service.

This strategy does not allow shared Java runtime implementation modules between
services.

## Slice 02 Build Rule

Slice 02 must not edit:

- `settings.gradle.kts`;
- root `build.gradle.kts`;
- `gradle/libs.versions.toml`;
- `gradle/verification-metadata.xml`;
- module `build.gradle.kts` files;
- service-local build files.

Later service implementation slices may add service-local build files and root
includes only after the service boundary, contract impact, test impact and
quality commands are verified.

## Service Build Guardrails

Future service projects must:

- own service-local source, resources and tests;
- own service-local Spring Boot application entrypoint when the service is a
  backend runtime;
- own service-local generated contract code;
- own service-local Dockerfile;
- run service-local tests without depending on another service's Java classes;
- use external contracts for cross-service behavior;
- remain independently startable and containerizable.

Future service projects must not depend on:

- `project(":forensic-analytics-domain")`;
- `project(":forensic-analytics-application")`;
- `project(":forensic-analytics-persistence")`;
- `project(":forensic-analytics-observability")`;
- another service project as a runtime implementation dependency;
- shared DTO, entity, utility, fixture or error-model modules.

If a future service needs concepts currently in domain or application modules,
the service must define service-owned models and map through external
contracts.

## Contract Code Generation

Central `contracts/**` files may be inputs to service-local generation.

Generated code must be local to the consuming service build. Generated Java
classes must not be committed as a shared module or published as a shared DTO
library between services.

## Planned Command Status

Commands such as:

```bash
./gradlew :services:forensic-ingestion-service:test
```

are planned only. They must not be documented as executable until the relevant
service project is registered and the command has been run successfully.

## Quality Gate Impact

For Slice 02 documentation and placeholder scaffolding:

```bash
git diff --check
```

For later service build-file changes:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

For commit readiness after production, build, contract or test changes:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Open Follow-Ups

- Define service-local Gradle plugin usage when the first backend service is
  created.
- Define service-local Spring Boot dependency rules when the first backend
  service is created.
- Define frontend build integration only after the `frontend-web-app` migration
  slice verifies the relationship with current `forensic-ui`.
- Define Docker and deployment checks only after service Dockerfiles and
  healthchecks exist.
