# Quality Gates

`QUALITY.md` is the authoritative quality contract.

Repository commands on this Windows host must run through WSL from:

```text
/mnt/d/Projects/forensic_analytics
```

## Minimum Quality Command

Run before broader validation:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Quality Gate

The full local gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Do not claim this passed unless the exact command, or a documented equivalent with the same tasks and strict dependency verification, was executed.

## Spring Boot Slice Commands

The `forensic-analytics-boot-app` commands below are future conditional commands. They must not be executed or reported as available until `settings.gradle.kts` includes the module and the module directory exists.

After the boot module exists:

```bash
./gradlew :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
```

After gRPC Spring lifecycle work:

```bash
./gradlew :forensic-analytics-boot-app:test :forensic-analytics-ingestion-grpc:test --dependency-verification strict --console=plain --stacktrace
```

After REST strategy work:

```bash
./gradlew :forensic-analytics-rest:test :forensic-analytics-boot-app:test --dependency-verification strict --console=plain --stacktrace
```

After observability work:

```bash
./gradlew :forensic-analytics-observability:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-testbed:test --tests '*LoggingArchitectureTest' --dependency-verification strict --console=plain --stacktrace
```

After architecture boundary changes:

```bash
./gradlew :forensic-analytics-application:test :forensic-analytics-ingestion-grpc:test :forensic-analytics-rest:test :forensic-analytics-observability:test :forensic-analytics-testbed:test --tests '*ArchitectureTest' --dependency-verification strict --console=plain --stacktrace
```

## Dependency Verification Commands

When new dependencies are added:

```bash
./gradlew --write-verification-metadata sha256 <task-that-resolves-the-failing-configuration> --console=plain --stacktrace
```

Then inspect:

```text
gradle/verification-metadata.xml
```

Only metadata for approved dependencies may remain.

After updating metadata, rerun the failing command and the full local quality gate with `--dependency-verification strict`.

## Optional Or Conditional Checks

Run `validatePlugins` only when plugin metadata, Gradle task inputs, task outputs or plugin implementation classes are changed:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

Do not document `dependencyCheckAnalyze` as a required command unless the repository actually adds and verifies that task.

Do not document `bootRun`, `bootJar` or Actuator health endpoints as available until the Spring Boot module introduces and verifies them.

## Quality Log Rule

Every slice implementation should append to:

```text
docs/workplan/spring-boot-migration/quality-log.md
```

Record:

- date
- slice
- command
- result
- failure summary if failed
- whether failure was caused by the slice
- remaining blocker

## Known Initial Blocker

During workplan creation, task discovery through Gradle failed because the Gradle file hash cache was locked:

```text
/mnt/d/Projects/forensic_analytics/.gradle/9.4.0/fileHashes/fileHashes.lock
```

Future implementation must retry Gradle task discovery after the lock clears.
