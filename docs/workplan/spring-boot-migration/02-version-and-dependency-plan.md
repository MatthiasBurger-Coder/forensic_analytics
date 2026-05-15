# Version And Dependency Plan

## Target Versions

| Component | Target | Current verified state |
|---|---:|---|
| Java | 25 | Root build uses Java 25. |
| Gradle | 9.4.0 | Wrapper uses Gradle 9.4.0. |
| Spring Boot | 4.0.6 | Not present in version catalog or build plugins. |
| JUnit | 6 | Version catalog uses JUnit 6.0.3. |
| gRPC | Existing verified version unless a later slice changes it | Version catalog uses 1.80.0. |

## Version Catalog Changes

Add only the aliases needed by the first Spring Boot slice.

Expected version aliases:

```toml
[versions]
spring-boot = "4.0.6"
spring-framework = "7.0.7"
```

Expected library aliases:

```toml
[libraries]
spring-boot = { module = "org.springframework.boot:spring-boot", version.ref = "spring-boot" }
spring-boot-autoconfigure = { module = "org.springframework.boot:spring-boot-autoconfigure", version.ref = "spring-boot" }
spring-test = { module = "org.springframework:spring-test", version.ref = "spring-framework" }
```

Expected plugin alias:

```toml
[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

Do not add Spring Boot starter aliases in Slice 02. Maven Central metadata for `spring-boot-starter-logging` `4.0.6` lists concrete logging dependencies including Logback and SLF4J bridges, which conflicts with ADR-0006 unless logging is excluded and verified or a later ADR accepts Boot-scoped logging.

Only add web, actuator, JDBC, data, validation, logging bridge, Spring Boot starter or gRPC integration aliases in the slice that actually needs them and has a verified dependency decision.

## Spring Boot Dependency Rules

Allowed initially:

- Spring Boot plugin in the root catalog, applied only where needed.
- Spring Boot runtime dependencies in `forensic-analytics-boot-app` only after the selected coordinates are verified not to introduce forbidden logging providers or after a dedicated ADR accepts the exception.

Forbidden initially:

- Spring dependencies in `forensic-analytics-domain`.
- Spring dependencies in `forensic-analytics-application`.
- Spring dependencies in `forensic-analytics-observability`.
- Spring AOP or AspectJ unless a dedicated ADR accepts it.
- `spring-boot-starter-logging`, Logback, Log4j-to-SLF4J and JUL-to-SLF4J unless explicitly accepted by a later observability ADR or excluded and verified from the Boot app classpath.
- A third-party gRPC Spring Boot starter without verification.
- accidental logging provider or telemetry additions outside the Boot boundary.

## Lombok Dependency Decision

Do not add Lombok for this migration. The Boot boundary has only small wiring classes, so explicit constructors are clearer and avoid a new annotation-processor dependency. Revisit Lombok only in a dedicated future slice with a verified dependency decision.

## Dependency Verification

Because strict dependency verification is enabled, every new external dependency requires metadata updates.

Expected metadata update command after dependency changes:

```bash
./gradlew --write-verification-metadata sha256 <task-that-resolves-the-failing-configuration> --console=plain --stacktrace
```

Then inspect `gradle/verification-metadata.xml` before committing.

Do not commit metadata for dependencies that are not required by the current slice.

After updating metadata, rerun the failing command and the full local quality gate with `--dependency-verification strict`.

## Build Plugin Application

The root build should declare the Spring Boot plugin with `apply false`.

Only `forensic-analytics-boot-app` should apply:

```kotlin
plugins {
    application
    alias(libs.plugins.spring.boot)
}
```

If the Boot plugin changes task behavior or dependency management for all projects, stop and document the impact before proceeding.

## Stop Conditions

Stop if:

- Spring Boot `4.0.6` cannot resolve under strict dependency verification.
- Spring Boot starter logging dependencies appear on any classpath without an accepted ADR or verified exclusions.
- adding Spring Boot introduces Spring classes into domain or application compile classpaths.
- dependency metadata changes include unrelated artifacts.
- Gradle plugin behavior changes the full quality gate unexpectedly.
- Java 25 compatibility errors appear in Spring Boot compilation.
