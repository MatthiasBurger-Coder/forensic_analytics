# Forensic Gateway Service

## Status

Slice 11 Gateway-to-Analysis-Store repository-to-BTM facade.

This service is the external API and UI/CLI facade shell. It currently exposes
Gateway-local health/status endpoints and an HTTP repository-analysis
submission endpoint:

- `GET /health`
- `GET /api/health`
- `GET /api/status`
- `POST /api/repository-analyses`
- `GET /api/repository-analyses/{analysisRunId}`

`POST /api/repository-analyses` requires `X-Correlation-Id` and
`Idempotency-Key`, validates a clean external HTTPS Git repository request,
maps it to the Analysis Store-owned repository-to-BTM orchestration contract
and returns an accepted public submission envelope. The Gateway does not
sequence workers and does not return repository workspace identifiers, local
paths, raw command output or unredacted downstream diagnostics.
`GET /api/repository-analyses/{analysisRunId}` reads the Analysis Store-owned
repository-to-BTM readiness state through the same owner contract and returns
the public, redacted status envelope.

BTM byte delivery, replay, reporting and frontend integration are later
workflow slices.
The Gateway must not contain AST, Joern, BTM, storage, replay or reporting
business logic and must not depend on worker service implementation classes.

## Analysis Store Orchestration Client

The Gateway uses service-local generated Protobuf classes from
`contracts/grpc/analysis-job.proto`; those generated classes stay inside
the Gateway service build output and are not shared as Java implementation
modules.

Configuration keys:

- `forensics.gateway.service.analysis-store.grpc.host`
- `forensics.gateway.service.analysis-store.grpc.port`
- `forensics.gateway.service.analysis-store.grpc.deadline-seconds`
