# Forensic Gateway Service

## Status

Slice 05 Gateway-to-Repository-Analysis facade.

This service is the external API and UI/CLI facade shell. It currently exposes
Gateway-local health/status endpoints and an HTTP repository-analysis
submission endpoint:

- `GET /health`
- `GET /api/health`
- `GET /api/status`
- `POST /api/repository-analyses`

`POST /api/repository-analyses` requires `X-Correlation-Id` and
`Idempotency-Key`, validates a clean external HTTPS Git repository request,
maps it to the `repository-analysis-service` gRPC `PrepareRepository` contract
and returns an accepted repository-to-BTM submission envelope. The Gateway does
not return or store repository workspace paths.

Worker orchestration, Analysis Store queries, BTM byte delivery, replay,
reporting and frontend integration are later workflow slices.
The Gateway must not contain AST, Joern, BTM, storage, replay or reporting
business logic and must not depend on worker service implementation classes.

## Repository Analysis Client

The Gateway uses service-local generated Protobuf classes from
`contracts/grpc/repository-analysis.proto`; those generated classes stay inside
the Gateway service build output and are not shared as Java implementation
modules.

Configuration keys:

- `forensics.gateway.service.repository-analysis.grpc.host`
- `forensics.gateway.service.repository-analysis.grpc.port`
- `forensics.gateway.service.repository-analysis.grpc.deadline-seconds`
