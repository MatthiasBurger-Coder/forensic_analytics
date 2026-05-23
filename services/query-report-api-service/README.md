# Query Report API Service

## Status

Slice S08 target-name public API facade for FA-MSA-001.

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
maps it to the `analysis-orchestrator-service` repository-to-BTM pending
readiness contract and returns an accepted public submission envelope.

`GET /api/repository-analyses/{analysisRunId}` reads the current
repository-to-BTM pending readiness state through the same orchestrator
contract and returns a public, redacted status envelope.

The service is facade-only. It does not sequence workers, run analysis, manage
repositories, control JavaParser or Joern, read private service databases,
return workspace identifiers, return local paths, expose raw command output or
leak unredacted downstream diagnostics.

BTM byte delivery, replay, report assembly and full frontend integration
remain later workflow slices. Planned OpenAPI routes are contract design
evidence, not runtime implementation evidence until later slices implement and
test them.

S18 adds service-local executable OpenAPI contract ownership for the current
repository-to-BTM submission/status routes. The service test
`GatewayOpenApiContractTest` reads `contracts/openapi/gateway-api.yaml` and
keeps the target public API contract executable outside
`forensic-analytics-rest`. This does not change the REST shape and does not
claim additional boot, container, persistence or frontend readiness.

## Analysis Orchestrator API Client

The service uses service-local generated Protobuf classes from
`contracts/grpc/analysis-job.proto`; those generated classes stay inside this
service build output and are not shared as Java implementation modules.

The current outbound adapter calls the S07 `analysis-orchestrator-service`
`StartRepositoryToBtm` and `GetRepositoryToBtmStatus` RPCs. The orchestrator
returns pending/status-only readiness: incomplete repository handoff, no source
snapshot availability, not-ready BTM delivery and skipped Joern execution. The
public API maps that pending state to `ACCEPTED` with explicit incomplete
diagnostics; it does not claim worker execution, generated BTM bytes, report
readiness or artifact custody.

S16 keeps this as accepted/pending state only. Extension routes for jobs,
results, replay and reports remain unavailable until a later slice implements
and tests owner-backed behavior. The facade still does not claim worker
execution, generated BTM bytes, report readiness, replay readiness or semantic
graph parity.

Configuration keys:

- `forensics.query-report-api.service.http.enabled`
- `forensics.query-report-api.service.http.host`
- `forensics.query-report-api.service.http.port`
- `forensics.query-report-api.service.analysis-orchestrator.grpc.host`
- `forensics.query-report-api.service.analysis-orchestrator.grpc.port`
- `forensics.query-report-api.service.analysis-orchestrator.grpc.deadline-seconds`

## Local Runtime

Package and build this service independently:

```bash
./gradlew --no-daemon :services:query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

Local operator start command:

```bash
./gradlew :services:query-report-api-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Service-local health check:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

S08 records the package, start and health-check commands but does not claim a
verified Docker image build, Docker Compose, Docker Swarm or Kubernetes
readiness for the target FA-MSA-001 landscape.
