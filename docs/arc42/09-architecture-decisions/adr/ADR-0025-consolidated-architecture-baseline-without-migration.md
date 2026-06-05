# ADR-0025: Consolidated architecture baseline without runtime migration strategy

## Status

Accepted

## Date

2026-06-05

## Workflow Version

2026-06-04

## Context

The ADR Baseline Consolidation workflow requires new authoritative
architecture, ADR, requirement, conflict-analysis and final-report output to be
placed under `docs/arc42/`.

S02 verified the existing numbered ADR range from `ADR-0001` through
`ADR-0024` and identified `ADR-0025` as the next candidate number. S03
identified consolidation risks around mixed `AD-*` and `ADR-*` entries,
multiple documentation roots, predecessor source-tree wording, target service
names, H2 and PostgreSQL scope, runtime-readiness claims, and observability
source-tree history.

This ADR consolidates verified active architecture consequences from the
existing ADR set. It does not renumber existing ADRs, rewrite ADR history,
convert `AD-*` arc42 rows into ADR files, or claim implementation evidence that
has not been verified from repository sources.

## Decision

Use this ADR as the consolidated arc42 architecture baseline for the
ADR-0001-through-ADR-0024 decision set.

The consolidated baseline is:

- Plugins are producer-side integration adapters. Server-side analysis owns
  parser execution, Joern execution, BTM generation, normalization,
  persistence, replay, graph projection and LLM analysis.
- The canonical analysis model is the source for normalized analysis facts.
  Graph and vector stores are projections derived from that model, not the
  primary source of truth.
- Runtime events are sensitive by default. Runtime values, prompts, payloads,
  stack traces and source content must not be persisted, indexed, logged or
  exposed unsafely.
- Operational diagnostics support correlation and troubleshooting but are not
  forensic evidence by themselves.
- The FA-MSA-001 service landscape is the active target architecture for
  service-split work:
  `repository-source-service`, `ingestion-service`,
  `java-parser-analysis-service`, `joern-analysis-service`,
  `analysis-orchestrator-service`, `query-report-api-service`, `cli-client`,
  `observability-stack` and `testbed`.
- Services must not share Java implementation, domain, DTO, repository,
  service, utility, fixture, logging, persistence or internal error-model
  modules. Service communication is limited to REST/OpenAPI, gRPC/protobuf,
  approved message contracts or documented file contracts.
- Spring Boot is allowed only at service-local bootstrap, configuration,
  lifecycle and explicitly verified health or readiness boundaries. Service
  domain and application packages remain framework-free.
- Retired predecessor source trees are not active implementation source. They
  may remain only as dated history, provenance, contract vocabulary or ADR
  context.
- `repository-source-service` owns repository checkout workspace metadata.
  PostgreSQL is the runtime metadata store only for that bounded scope, and
  Liquibase owns schema creation and evolution for that schema.
- H2 remains limited to deterministic repository-source adapter tests and
  direct fixtures. It is not runtime storage and does not prove Docker,
  startup or readiness behavior.
- Broader canonical analytics persistence, graph storage, vector storage,
  report storage, LLM package storage and platform workspace administration
  remain open unless another accepted ADR closes those decisions.
- Target service architecture is not production-readiness evidence. Build,
  start, configuration, healthcheck, container, Swarm or Kubernetes readiness
  must be proven separately by repository evidence and recorded commands.
- Authoritative outputs created by this workflow belong under `docs/arc42/`.
  Workflow-control files remain under `docs/workflow/` and
  `docs/workflows/adr-baseline-consolidation-20260604/`.

This ADR is not a runtime migration strategy. It defines no execution sequence,
data movement, runtime switchover, compatibility alias policy or deployment
procedure.

## Consequences

- S05 may move or mirror authoritative ADR content into the arc42 ADR chapter
  while preserving existing ADR history and numbering.
- S06 and S07 may align requirement and arc42 documentation to this
  consolidated baseline without inventing runtime behavior or implementation
  evidence.
- Documents that mention target services must keep target architecture,
  predecessor history and implemented runtime evidence separate.
- Documents that mention persistence must keep the repository-source
  PostgreSQL decision bounded and leave broader analytics persistence open.
- Future workflow slices must stop when a claim would require guessing a
  service boundary, runtime behavior, persistence owner, contract shape,
  readiness state, graph label, storage table, API endpoint or quality command.

## Verified Inputs

- `docs/workflow/workflow.md`
- `docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md`
- `docs/arc42/09-architecture-decisions/conflicts/adr-conflict-analysis-20260604.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/adr/ADR-0001-plugins-are-producers.md`
- `docs/adr/ADR-0002-canonical-analysis-model.md`
- `docs/adr/ADR-0003-runtime-events-are-sensitive.md`
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`
- `docs/adr/ADR-0005-adapter-logging-observability-boundary.md`
- `docs/adr/ADR-0009-no-shared-common-modules.md`
- `docs/adr/ADR-0010-contract-first-rest-and-grpc.md`
- `docs/adr/ADR-0013-data-ownership-per-service.md`
- `docs/adr/ADR-0017-target-microservices-service-landscape.md`
- `docs/adr/ADR-0019-spring-boot-service-bootstrap-boundary.md`
- `docs/adr/ADR-0022-final-modular-monolith-source-tree-retirement.md`
- `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md`
- `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md`
