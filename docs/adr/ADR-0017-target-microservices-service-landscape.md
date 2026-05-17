# ADR-0017: Use the target microservices service landscape

## Status

Accepted

## Context

The active workflow plans a broader microservices ecosystem than older arc42
deployment notes. Older notes named `forensic-server` and several worker roots.
The active workflow names Gateway, ingestion, repository analysis, Java AST,
Joern CPG, BTM generation, analysis store, graph/replay, report generation and
frontend boundaries.

Slice 00 verified that the current repository is still a modular monolith. The
service landscape is therefore a target architecture decision, not an
implementation claim.

## Decision

Use the active workflow service landscape as the target for the microservices
ecosystem conversion:

- `forensic-gateway-service`
- `forensic-ingestion-service`
- `repository-analysis-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`
- `analysis-store-service`
- `graph-replay-service`
- `report-generation-service`
- `frontend-web-app`

The target services must communicate only through REST/OpenAPI,
gRPC/protobuf or approved event contracts. They must not share Java
implementation, domain, DTO, repository, service, utility, fixture or internal
error-model modules.

## Consequences

- Older arc42 service-root names are historical planning context only.
- Slice 02 may prepare target roots using the names above.
- Slice 03 must define contracts before service implementations depend on
  communication behavior.
- Service implementation slices must keep service-owned domain models and
  service-local generated code.
- Runtime readiness cannot be claimed until each service has independent build,
  start, test, configuration, healthcheck, container and deployment evidence.

## Related Documents

- `docs/architecture/target-microservices-architecture.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/service-communication-matrix.md`
- `docs/architecture/data-ownership.md`
