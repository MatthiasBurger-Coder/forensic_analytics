# Slice 12 Engine Request Handoff Testbed Result

Date: 2026-05-11

## Scope Executed

- Added a testbed scenario for the local Engine Request handoff path.
- The scenario creates a temporary `engine-request.json` fixture.
- The fixture references two real local payload files:
  - `source-facts.json`
  - `rules.btm`
- The testbed runs the existing CLI command:
  - `ingest-request --request <engine-request.json> --output <directory>`
- The test verifies the generated `engine-request-import-summary.txt`.
- Extended `MIGRATION_WORKPLAN.md` with Slice 12.

## Boundary Decision

Slice 12 verifies the Slice 10 and Slice 11 path through the existing testbed module.

The testbed remains a test-only integration layer. It does not introduce new production modules, new transport code, or new external services. The CLI remains the inbound adapter and the request importer remains responsible for reading payload bytes from the explicit files referenced by the handoff request.

## Evidence Integrity

- Payload files are created as explicit temporary test fixtures.
- The CLI imports the request from an actual file path.
- The test verifies the imported payload count from the import summary.
- The scenario does not fabricate runtime evidence, stack frames, graph edges, or replay paths.

## Scope Deliberately Not Executed

- No Docker or Joern process is started.
- No gRPC client was added.
- No remote transport was added.
- No Gradle or Maven build-tool adapter was moved into the Engine.
- No new external dependency was added.
- No schema, persistence baseline, Java baseline, Gradle baseline, or `forensics_tracing` code changed.

## Workplan Alignment

This slice adds broader local confidence for the plugin handoff path after:

- Slice 10 added request ingestion.
- Slice 11 exposed request ingestion through the CLI.

## Verification

Executed in `D:\Projects\forensic_analytics`:

```text
.\gradlew.bat :forensic-analytics-testbed:test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: passed.

Notes:

- The full gate emitted Java 25 deprecation warnings from gRPC/Protobuf usage of `sun.misc.Unsafe`.
- No test, dependency verification, JaCoCo, or package-coverage failure occurred.

Sonar:

- Skipped because `SONAR_TOKEN` was not set in the local environment.
