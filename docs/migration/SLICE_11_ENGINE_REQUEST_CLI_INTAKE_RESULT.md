# Slice 11 Engine Request CLI Intake Result

Date: 2026-05-11

## Scope Executed

- Added `ingest-request` to `forensic-analytics-cli`.
- Added CLI argument parsing for:
  - `--request <engine-request.json>`
  - `--output <directory>`
- Wired the command to `EngineIngestionRequestImporter`.
- Used `DefaultForensicIngestionUseCase` with `InMemoryIngestionSessionRepository` for local default CLI execution.
- Added a structured import summary file:
  - `engine-request-import-summary.txt`
- Added tests for parser behavior, command path normalization, successful local import, missing request files, help output, and error reporting.
- Extended `MIGRATION_WORKPLAN.md` with Slice 11.

## Boundary Decision

Slice 11 adds a local CLI entry point for the handoff artifact from Slice 10.

The CLI remains an inbound adapter. It delegates request parsing and payload import to `forensic-analytics-ingestion-request` and delegates ingestion behavior to the existing application use case. The domain and application layers do not depend on CLI, persistence, request files, or filesystem-specific adapter code.

## Evidence Integrity

- The CLI does not modify the request payloads.
- Payload bytes are still read by the Slice 10 importer from the explicit files referenced in `engine-request.json`.
- Missing request files fail explicitly.
- The generated summary records the request file, session id, completion status, and uploaded payload count.

## Scope Deliberately Not Executed

- No gRPC client was added.
- No remote transport was added.
- No Gradle or Maven build-tool adapter was moved into the Engine.
- No new external dependency was added.
- No schema, persistence baseline, Java baseline, Gradle baseline, or `forensics_tracing` code changed.

## Workplan Alignment

This slice makes the Slice 10 local Engine Request intake usable from the existing CLI module.

It does not replace the existing `analyze` command and does not change repository analysis orchestration.

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
