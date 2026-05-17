# Forensic Ingestion Service

## Status

Slice 04 initial independent service.

This service receives and validates plugin, scanner and runtime evidence
packages over gRPC. It owns raw intake sessions only. Canonical normalized facts
belong to `analysis-store-service`, and repository checkout belongs to
`repository-analysis-service`.

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
./gradlew :services:forensic-ingestion-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:forensic-ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The service starts with gRPC on port `9090` and a JDK HTTP health endpoint on
port `8081` by default. Tests use ephemeral ports.

## Docker

Build after the service jar exists:

```bash
docker build -f services/forensic-ingestion-service/Dockerfile .
```
