# Quality Gates

## Command Environment

On this Windows host, repository commands must run through WSL from:

```bash
cd /mnt/d/Projects/forensic_analytics
```

Do not use Windows-native Gradle commands for repository verification unless WSL becomes unavailable and the blocker is reported first.

## Narrow Verification Commands

Use the narrowest meaningful command after each implementation slice.

Potential commands by affected area:

```bash
./gradlew :forensic-analytics-rest:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-ingestion-grpc:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-analytics-bootstrap:test --dependency-verification strict --console=plain --stacktrace
```

If a new observability module is added, also run:

```bash
./gradlew :<selected-observability-module>:test --dependency-verification strict --console=plain --stacktrace
```

Replace `<selected-observability-module>` only after Slice 02 verifies the actual module name.

## Minimum Repository Verification

Before broader verification, run the repository minimum command from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Quality Gate

The authoritative full local quality gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Do not claim this passed unless the exact command, or a repository-documented equivalent with the same tasks and strict dependency verification, was executed successfully.

## Dependency Verification

If a dependency is added, strict dependency verification may fail until the verification metadata is updated through a deliberate dependency-governance step.

Do not bypass strict dependency verification. Do not introduce a logging binding to make tests pass.

## Test Expectations

Tests should cover:

- correlation ID generation
- preservation of incoming REST correlation ID
- nested correlation scope cleanup
- failure cleanup after thrown exceptions
- sanitized operation event shape
- no args/results in default logs
- REST response contract preservation
- gRPC status mapping preservation
- CLI stdout/stderr contract preservation
- architecture boundary preservation

## Failure Reporting

If a command fails, report:

- exact command executed
- failing task or test
- failure summary
- whether the failure appears caused by the logging integration
- remaining blocker
