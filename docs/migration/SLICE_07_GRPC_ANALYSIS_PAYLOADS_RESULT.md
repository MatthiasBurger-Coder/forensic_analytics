# Slice 07 gRPC Analysis Payloads Result

Date: 2026-05-10

## Scope Executed

- Extended `AnalysisDataEnvelope` with `AnalysisPayloadDescriptor`.
- Added stable payload kinds for source facts, semantic artifacts, rule artifacts, runtime traces, and diagnostic reports.
- Replaced the application and domain ingestion payload type string with a domain-owned payload descriptor.
- Mapped protobuf payload metadata in the gRPC adapter before calling the application use case.
- Added validation for required payload descriptor fields and supported payload kinds.
- Added tests for descriptor validation, payload-kind mapping, domain immutability, and ingestion flow propagation.

## Boundary Decision

The protobuf DTO remains inside `forensic-analytics-ingestion-grpc`.

The application and domain layers use `AnalysisPayloadDescriptor` and `AnalysisPayloadKind` from the domain module. This keeps the inbound gRPC schema from leaking inward while still giving ingestion payloads stable, typed metadata.

## Compatibility Notes

The existing protobuf `payload_type` field remains on field number `6` and is marked deprecated. It is no longer mapped into the application command. New upload envelopes must provide `payload_descriptor`.

## Scope Deliberately Not Executed

- No persistence schema was added.
- No plugin-side gRPC client changes were made.
- No server/bootstrap wiring changes were required.
- No graph, replay, report, LLM, JavaParser, Joern, Gradle, Maven, or `forensics_tracing` behavior was changed.

## Workplan Alignment

This slice follows Slice 07 from `MIGRATION_WORKPLAN.md`:

- It extends the existing gRPC ingestion contract with stable analysis payload metadata.
- It keeps protobuf DTOs out of the domain and application layers.

## Verification

Executed commands:

```text
.\gradlew.bat :forensic-analytics-domain:test :forensic-analytics-application:test :forensic-analytics-persistence:test :forensic-analytics-ingestion-grpc:test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result:

- The targeted domain, application, persistence, and gRPC tests passed.
- The first targeted run exposed a missing explicit `forensic-analytics-domain` dependency in the gRPC adapter after adding the domain descriptor mapping. The adapter dependency was added and the targeted tests were rerun successfully.
- The documented minimum quality command passed.
- The full local quality gate passed.
- SonarCloud was not executed because `SONAR_TOKEN` was not configured locally.
