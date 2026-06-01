# Query Report API Service

## Status

Slice S08/S11 target-name public API facade for FA-MSA-001 and
FA-MVP-0001 repository checkout workspace routes.

The service exposes the currently verified public routes from the transitional
Gateway/OpenAPI contract under the target authority
`query-report-api-service`.

Repository-analysis routes:

- `GET /health`
- `GET /api/health`
- `GET /api/status`
- `POST /api/repository-analyses`
- `GET /api/repository-analyses/{analysisRunId}`

Repository checkout workspace routes:

- `POST /api/workspace-metadata`
- `POST /api/workspaces`
- `GET /api/workspaces/{workspaceId}`
- `GET /api/workspaces/{workspaceId}/checkout-result`
- `POST /api/workspaces/{workspaceId}/branches/{workspaceBranchId}/refresh`

`POST /api/repository-analyses` requires `X-Correlation-Id` and
`Idempotency-Key`, validates a clean external HTTPS Git repository request,
maps it to the `analysis-orchestrator-service` repository-to-BTM pending
readiness contract and returns an accepted public submission envelope.

`GET /api/repository-analyses/{analysisRunId}` reads the current
repository-to-BTM pending readiness state through the same orchestrator
contract and returns a public, redacted status envelope.

The service is facade-only. It does not sequence workers, run analysis, manage
repositories, control JavaParser or Joern, read private service databases,
return local paths, expose raw command output or leak unredacted downstream
diagnostics. For FA-MVP-0001 it may return opaque public repository checkout
workspace IDs and branch IDs received through the repository-source owner API;
those IDs are not filesystem paths, authorization decisions or platform
workspace membership records.

BTM byte delivery, replay, report assembly, broader dashboard/list/query
views and non-workspace frontend integrations remain later workflow slices.
Planned OpenAPI routes are contract design evidence, not runtime
implementation evidence until later slices implement and test them.

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

## Repository Source Workspace Client

The FA-MVP-0001 workspace routes call the repository-source owner API through
service-local generated Protobuf classes from
`contracts/grpc/repository-analysis.proto`. The generated classes stay inside
this service build output and are not shared as Java implementation modules.

`query-report-api-service` validates public REST headers and payloads, maps
public DTOs to repository-source owner requests, redacts diagnostics and
returns sanitized public workspace state. It does not checkout repositories,
read repository-source H2 files, read repository-source workspace directories
or expose raw Git output.

## Repository Source Database Settings

S08 adds operator-protected public Settings routes for repository-source
PostgreSQL configuration status and validation:

- `GET /api/settings/repository-source/database`
- `POST /api/settings/repository-source/database/validation`

Both routes require `X-Correlation-Id` and `X-Operator-Token`. If the operator
token is not configured, the service returns `SETTINGS_AUTH_NOT_CONFIGURED`.
The validation request accepts a write-only password field, delegates syntax
and connectivity validation to the repository-source owner gRPC API and returns
only sanitized settings status. S08 does not persist credentials and does not
hot-apply changed database settings; responses report `RESTART_REQUIRED`.

Configuration keys:

- `forensics.query-report-api.service.http.enabled`
- `forensics.query-report-api.service.http.host`
- `forensics.query-report-api.service.http.port`
- `forensics.query-report-api.service.analysis-orchestrator.grpc.host`
- `forensics.query-report-api.service.analysis-orchestrator.grpc.port`
- `forensics.query-report-api.service.analysis-orchestrator.grpc.deadline-seconds`
- `forensics.query-report-api.service.repository-source.grpc.host`
- `forensics.query-report-api.service.repository-source.grpc.port`
- `forensics.query-report-api.service.repository-source.grpc.deadline-seconds`
- `forensics.query-report-api.service.workspace.schema-version`
- `forensics.query-report-api.service.workspace.metadata.timeout-seconds`
- `forensics.query-report-api.service.workspace.refresh.ephemeral`
- `forensics.query-report-api.service.workspace.refresh.allow-shallow-clone`
- `forensics.query-report-api.service.workspace.refresh.allow-partial-clone`
- `forensics.query-report-api.service.workspace.refresh.allow-sparse-checkout`
- `forensics.query-report-api.service.workspace.refresh.timeout-seconds`
- `forensics.query-report-api.service.workspace.refresh.max-workspace-bytes`
- `forensics.query-report-api.service.settings.operator-token`

## Local Runtime

Package and build this service independently:

```bash
./gradlew --no-daemon :query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

Local operator start command:

```bash
./gradlew :query-report-api-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Service-local health check:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

S08 records the package, start and health-check commands but does not claim a
verified Docker image build, Docker Compose, Docker Swarm or Kubernetes
readiness for the target FA-MSA-001 landscape.
