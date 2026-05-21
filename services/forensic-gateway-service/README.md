# Forensic Gateway Service

## Status

Slice 11 current/predecessor Gateway-to-Analysis-Store repository-to-BTM
facade. FA-MSA-001 target public API ownership moves to
`query-report-api-service`, and target orchestration ownership moves to
`analysis-orchestrator-service`.

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
maps it to the current predecessor repository-to-BTM orchestration contract and
returns an accepted public submission envelope. The Gateway does not sequence
workers and does not return repository workspace identifiers, local paths, raw
command output or unredacted downstream diagnostics.
`GET /api/repository-analyses/{analysisRunId}` reads the current predecessor
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

## Local Runtime

Package and build this service for the local repository-to-BTM Compose
landscape:

```bash
./gradlew --no-daemon :services:forensic-gateway-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/forensic-gateway-service/Dockerfile --build-arg SERVICE_JAR=services/forensic-gateway-service/build/libs/forensic-gateway-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/forensic-gateway-service:local .
```

When started through `deployment/docker-compose/repository-to-btm.local.yml`,
the public HTTP facade is published on `127.0.0.1:18080` and the local health
check is:

```bash
curl -fsS http://127.0.0.1:18080/api/health
```
