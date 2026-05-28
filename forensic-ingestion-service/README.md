# Forensic Ingestion Service

## Status

Slice 04 initial independent service.

This service receives and validates plugin, scanner and runtime evidence
packages over gRPC. It owns raw intake sessions, upload-session lifecycle,
rejected-intake diagnostics and raw runtime or analysis payload byte custody
until an explicit handoff transfers custody. Canonical normalized facts are
target-owned by the FA-MSA-001 service that produces or accepts the evidence
category, and repository checkout belongs to `repository-source-service`.
The current predecessor pipeline may still hand accepted intake to
`analysis-store-service` until later migration slices replace that path.

## Boundaries

- Service-local Spring Boot bootstrap lives under `bootstrap`.
- Service-owned domain and application models live under this service root.
- Generated protobuf and gRPC classes are produced service-locally from
  `contracts/grpc/forensic-ingestion.proto`.
- No Java implementation module from another service or from the current
  modular-monolith domain/application/persistence modules is used.
- `AnalyzeRepository` is retained by the v1 contract but returns an explicit
  gRPC `UNIMPLEMENTED` status in this service because repository checkout is
  not an ingestion responsibility.

## Local Commands

```bash
./gradlew :forensic-ingestion-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :forensic-ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The service starts with gRPC on port `9090` and a JDK HTTP health endpoint on
port `8081` by default. Tests use ephemeral ports.

## Docker

Build after the service jar exists:

```bash
docker build -f forensic-ingestion-service/Dockerfile .
```
