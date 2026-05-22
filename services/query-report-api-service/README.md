# Query Report API Service

## Status

Slice S10 target-name public API facade for FA-MSA-001.

The service exposes the currently verified public repository-analysis routes
from the transitional Gateway/OpenAPI contract under the target authority
`query-report-api-service`:

- `GET /health`
- `GET /api/health`
- `GET /api/status`
- `POST /api/repository-analyses`
- `GET /api/repository-analyses/{analysisRunId}`

`POST /api/repository-analyses` requires `X-Correlation-Id` and
`Idempotency-Key`, validates a clean external HTTPS Git repository request,
maps it to the current predecessor repository-to-BTM owner contract and
returns an accepted public submission envelope.

`GET /api/repository-analyses/{analysisRunId}` reads the current predecessor
repository-to-BTM readiness state through the same owner contract and returns a
public, redacted status envelope.

The service is facade-only. It does not sequence workers, run analysis, manage
repositories, control JavaParser or Joern, read private service databases,
return workspace identifiers, return local paths, expose raw command output or
leak unredacted downstream diagnostics.

BTM byte delivery, replay, reporting and frontend integration remain later
workflow slices. Planned OpenAPI routes are contract design evidence, not
runtime implementation evidence until later slices implement and test them.

## Analysis Store Owner API Client

The service uses service-local generated Protobuf classes from
`contracts/grpc/analysis-job.proto`; those generated classes stay inside this
service build output and are not shared as Java implementation modules.

The current outbound adapter calls the predecessor Analysis Store owner API
because the S09 target orchestrator deliberately does not implement
repository-to-BTM submission/status RPCs yet. Repointing this facade to
`analysis-orchestrator-service` requires a later contract-first slice and
target endpoint verification.

Configuration keys:

- `forensics.query-report-api.service.http.enabled`
- `forensics.query-report-api.service.http.host`
- `forensics.query-report-api.service.http.port`
- `forensics.query-report-api.service.analysis-store.grpc.host`
- `forensics.query-report-api.service.analysis-store.grpc.port`
- `forensics.query-report-api.service.analysis-store.grpc.deadline-seconds`

## Local Runtime

Package and build this service independently:

```bash
./gradlew --no-daemon :services:query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/query-report-api-service/Dockerfile --build-arg SERVICE_JAR=services/query-report-api-service/build/libs/query-report-api-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/query-report-api-service:local .
```

Local operator start command:

```bash
./gradlew :services:query-report-api-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Service-local health check:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

S10 records the start command but does not claim Docker Compose, Docker Swarm
or Kubernetes readiness for the target FA-MSA-001 landscape.
