# ADR-0017: Use the FA-MSA-001 target microservices service landscape

## Status

Accepted

Post-S05 closure note: ADR-0022 closes this ADR's conditional source-tree
retirement path. The central `forensic-analytics-*` modular-monolith source
trees named below are no longer tracked active implementation inputs after
checkpoint `d8d9dab`; the target service landscape and service-autonomy rules in
this ADR remain active.

## Context

The repository previously documented a target landscape using names such as
`forensic-gateway-service`, `repository-analysis-service`,
`java-ast-analysis-service`, `joern-cpg-analysis-service`,
`analysis-store-service`, `graph-replay-service`, `report-generation-service`
and `frontend-web-app`.

FA-MSA-001 superseded that naming direction for the active microservice
decomposition workflow. At original acceptance time, the repository still
contained older service slice directories and central `forensic-analytics-*`
modular-monolith modules. After ADR-0022/S05, the older service slice
directories remain migration evidence where registered, while the central
source trees are retired historical predecessor evidence.

The repository still has partial service slices. The service landscape is
therefore a target architecture decision, not a production-readiness claim.

## Decision

Use the FA-MSA-001 service landscape as the active target for the microservices
ecosystem conversion:

- `repository-source-service`
- `ingestion-service`
- `java-parser-analysis-service`
- `joern-analysis-service`
- `analysis-orchestrator-service`
- `query-report-api-service`
- `cli-client`
- `observability-stack`
- `testbed`

Optional later services may be added through separate requirements or slices,
including:

- `btm-generation-service`
- `graph-replay-service`
- `incident-analysis-service`

The target services must communicate only through REST/OpenAPI,
gRPC/protobuf, approved message contracts or documented file contracts. They
must not share Java implementation, domain, DTO, repository, service, utility,
fixture, logging, persistence or internal error-model modules.

## Superseded Current Names

The following currently implemented or planned names remain current-state
evidence until migrated. They are not target aliases:

| Current or predecessor name | FA-MSA-001 target decision |
|---|---|
| `forensic-gateway-service` | Split by responsibility into `query-report-api-service`, `analysis-orchestrator-service` facade interaction and `cli-client` public API consumption where applicable. |
| `forensic-ingestion-service` | Migrates to `ingestion-service`. |
| `repository-analysis-service` | Migrates to `repository-source-service` for repository access and source snapshot ownership. |
| `java-ast-analysis-service` | Migrates to `java-parser-analysis-service`. |
| `joern-cpg-analysis-service` | Migrates to `joern-analysis-service`. |
| `analysis-store-service` | Its data ownership must be resolved by the FA-MSA-001 data-ownership slice before mapping to service-local persistence or orchestration ownership. |
| `btm-generation-service` | Optional later service unless a later requirement makes it mandatory. |
| `graph-replay-service` | Optional later service unless a later requirement makes it mandatory. |
| `report-generation-service` | Report/query responsibilities move first to `query-report-api-service`; standalone report generation is optional later work unless required explicitly. |
| `frontend-web-app` | Adjacent frontend boundary, not a mandatory FA-MSA-001 service root. |

## Consequences

- Older arc42 service-root names are historical or current-state context only.
- Slice 01 of the FA-MSA-001 workflow updates architecture documentation to use
  the target names above while preserving current implementation evidence.
- Service implementation slices must keep service-owned domain models and
  service-local generated code.
- Contract slices must define REST, gRPC, messaging or file contracts before
  service implementations depend on communication behavior.
- Runtime readiness cannot be claimed until each mandatory service has
  independent build, start, test, configuration, healthcheck, container and
  deployment evidence.
- Central monolith modules may be removed only after caller-free evidence,
  replacement tests, rollback notes and the required quality gate exist.

## Related Documents

- `docs/architecture/target-microservices-architecture.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/service-communication-matrix.md`
- `docs/architecture/data-ownership.md`
- `docs/workflow/workflow.md`
