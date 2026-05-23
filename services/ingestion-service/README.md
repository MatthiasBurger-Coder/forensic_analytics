# Ingestion Service

## Status

Slice 06 FA-MSA-001 target-name ingestion service.

This service receives and validates plugin, scanner and runtime evidence
packages over gRPC. It owns raw intake sessions, upload-session lifecycle,
rejected-intake diagnostics and raw runtime or analysis payload byte custody
until an explicit handoff transfers custody. Canonical normalized facts are
target-owned by the FA-MSA-001 service that produces or accepts the evidence
category, and repository checkout belongs to `repository-source-service`.
The current predecessor pipeline may still hand accepted intake to
`analysis-store-service` until later migration slices replace that path.
`services/forensic-ingestion-service` remains predecessor and rollback
evidence; it is not an alias for this target service.

## Boundaries

- Service-local Spring Boot bootstrap lives under `bootstrap`.
- Service-owned domain and application models live under this service root.
- Generated protobuf and gRPC classes are produced service-locally from
  `contracts/grpc/forensic-ingestion.proto`.
- No Java implementation module from another service or from the current
  modular-monolith domain/application/persistence modules is used.
- `AnalyzeRepository` is retained by the v1 contract but returns an explicit
  gRPC `UNIMPLEMENTED` status in this service because repository checkout is
  not an ingestion responsibility. The exact status description is
  `AnalyzeRepository is not implemented by ingestion-service; repository checkout is owned by repository-source-service`.
- Engine ingestion request manifests are parsed service-locally from verified
  fields only and imported through this service's application boundary. Missing
  fields, unsupported payload kinds and missing payload files remain explicit
  errors.

## Local Commands

```bash
./gradlew :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

Local operator start command:

```bash
./gradlew :services:ingestion-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The service starts with gRPC on port `9090` and a JDK HTTP health endpoint on
port `8081` by default. Tests use ephemeral ports.

## Docker

Build after the service jar exists:

```bash
docker build -f services/ingestion-service/Dockerfile .
```
