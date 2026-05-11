# Slice 13 Standalone Engine Request CLI Result

Date: 2026-05-11

## Scope Executed

- Added coverage for the standalone CLI ServiceLoader path.
- Verified that `ingest-request` can run through `ForensicAnalyticsCli.runWithServiceLoader(...)` without a configured `RunRepositoryAnalysisUseCase` provider.
- Kept the existing failure expectation for `analyze` when no analysis use-case provider is configured.
- Used a real temporary `engine-request.json` fixture with a real local payload file.
- Extended `MIGRATION_WORKPLAN.md` with Slice 13.

## Boundary Decision

Slice 13 is a wiring-confidence slice.

The production code already routes `ingest-request` through the default local request importer, while the `analyze` command still requires a configured repository analysis use case. The slice adds regression coverage for that distinction without changing production wiring.

## Evidence Integrity

- The test fixture writes an explicit request file and payload file.
- The CLI imports the request through the same path used by the standalone `main` wiring.
- The generated summary is verified for request path, completion status, and uploaded payload count.
- No runtime evidence, graph relationships, replay steps, or analysis findings are fabricated.

## Scope Deliberately Not Executed

- No production code was changed.
- No gRPC client was added.
- No remote transport was added.
- No Gradle or Maven build-tool adapter was moved into the Engine.
- No new external dependency was added.
- No schema, persistence baseline, Java baseline, Gradle baseline, or `forensics_tracing` code changed.

## Workplan Alignment

This slice hardens the local handoff path after:

- Slice 10 added request ingestion.
- Slice 11 exposed request ingestion through the CLI.
- Slice 12 covered the handoff path in the testbed.

## Verification

Executed in `D:\Projects\forensic_analytics`:

```text
.\gradlew.bat :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: passed.

Notes:

- The full gate emitted Java 25 deprecation warnings from gRPC/Protobuf usage of `sun.misc.Unsafe`.
- No test, dependency verification, JaCoCo, or package-coverage failure occurred.

Sonar:

- Skipped because `SONAR_TOKEN` was not set in the local environment.
