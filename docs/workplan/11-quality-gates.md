# Quality Gates

`QUALITY.md` is the authoritative quality contract. The future implementation must verify commands from repository files instead of relying on memory.

## Verified Baseline

Repository inspection confirms:

- Java baseline: 25
- Gradle wrapper: 9.4.0
- JUnit Platform is configured for tests
- JaCoCo is configured in subprojects
- root task `checkPackageCoverage` is registered
- Gradle dependency verification is required in strict mode

## Minimum Verification

Run the repository minimum verification before broader validation:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

This is the documented minimum command in `QUALITY.md`.

## Requested Clean Check Diagnostic

The workplan implementation must also run at least:

```bash
./gradlew clean check --dependency-verification strict --console=plain --stacktrace
```

This command is useful for general build health. It is not automatically the full local gate unless `checkPackageCoverage` is wired into `check`.

## Full Local Quality Gate

The full local gate from `QUALITY.md` is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Use this before final commit when practical. Do not claim it passed unless this exact command or a repository-documented equivalent was executed successfully.

## Additional Task Checks

Check these tasks when relevant:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew jacocoTestReport --dependency-verification strict --console=plain --stacktrace
./gradlew jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

`validatePlugins` is required only when plugin metadata, Gradle task inputs, task outputs or plugin implementation classes are changed and the task exists in the verified Gradle task graph. If a task is absent, document the checked command and the absence. Do not blindly replace it with another task.

Recommended task-existence check:

```bash
./gradlew tasks --all --console=plain
```

## Slice-Specific Verification

Slice 1:

- proto generation
- gRPC mapper tests
- request validator tests

Slice 2:

- gRPC service tests
- adapter architecture tests

Slice 3:

- domain model tests

Slice 4:

- Git adapter tests with local temporary repositories

Slice 5:

- application service tests with fake ports

Slice 6:

- plugin client mapping tests and fake-server tests

Slice 7:

- mini end-to-end test

Slice 8:

- session and persistence adapter tests

Slice 9:

- source-root detection fixtures

Slices 10 and 11:

- opt-in WildFly hardening scenario only

Slice 12:

- full quality gate or documented blocker

## Failure Reporting

When a command fails, report:

- command executed
- failing task or test
- short failure summary
- whether the failure appears caused by the current slice
- remaining blocker

Do not stage or commit while required quality gates fail unless the blocker is explicitly accepted and documented by the repository workflow.
