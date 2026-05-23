# Target Microservices Architecture

## Status

FA-MSA-001 Slice 01 target architecture baseline with S02 transitional
contract/runtime parity evidence.

This document defines the active target service landscape for the monolith
decomposition workflow. It is an architecture target and records slice-level
implementation evidence only where the workflow has verified it. It is not a
production-readiness claim.

FA-MSA-001-LMR S02 verified that registered target-name service test tasks and
the repository minimum test gate pass on the active workflow branch. ADR-0022
and S05 then retired the remaining legacy source trees. That evidence is still
transitional: it proves testable current surfaces and source-tree retirement,
not full target runtime parity or independent production readiness.

## Architecture Decision

ADR-0017 defines the FA-MSA-001 target service landscape:

- `repository-source-service`
- `ingestion-service`
- `java-parser-analysis-service`
- `joern-analysis-service`
- `analysis-orchestrator-service`
- `query-report-api-service`
- `cli-client`
- `observability-stack`
- `testbed`

Optional later service candidates remain outside mandatory FA-MSA-001 closure
unless a later requirement adds them:

- `btm-generation-service`
- `graph-replay-service`
- `incident-analysis-service`

Older names such as `forensic-gateway-service`,
`forensic-ingestion-service`, `repository-analysis-service`,
`java-ast-analysis-service`, `joern-cpg-analysis-service`,
`analysis-store-service`, `graph-replay-service`,
`report-generation-service` and `frontend-web-app` are current-state or
historical planning evidence only. They are not compatibility aliases for the
FA-MSA-001 target names.

## Current Baseline

The current platform has:

- one Gradle multi-project build;
- service-only Java project registration under `services:*`;
- retired historical `forensic-analytics-*` predecessor source trees after
  ADR-0022/S05;
- service-local REST/gRPC/API evidence where implemented;
- service-local in-memory persistence where implemented;
- partial service slice directories under `services/**`;
- Docker material for some current surfaces;
- no verified Docker Swarm or Kubernetes manifests for the target landscape.

The currently registered service slices are implementation evidence only:

- `repository-source-service`;
- `ingestion-service`;
- `java-parser-analysis-service`;
- `joern-analysis-service`;
- `analysis-orchestrator-service`;
- `query-report-api-service`;
- `cli-client`;
- `observability-stack`;
- `testbed`;
- `forensic-gateway-service`;
- `forensic-ingestion-service`;
- `repository-analysis-service`;
- `analysis-store-service`;
- `java-ast-analysis-service`;
- `joern-cpg-analysis-service`;
- `btm-generation-service`.

The registered FA-MSA-001 target-name services are implementation evidence for
the target landscape. The predecessor and current-state services remain
rollback and migration evidence. Together they do not prove the FA-MSA-001
target service landscape is complete, independently deployable,
health-checkable, containerized for production, runtime-parity complete or
free of monolith callers.

## Target Principles

Each productive FA-MSA-001 service owns its internal hexagonal architecture:

```text
domain
application
adapter/in
adapter/out
bootstrap
```

Allowed service integration mechanisms:

- REST/OpenAPI;
- gRPC/protobuf;
- approved message contracts;
- documented file contracts with explicit ownership and provenance.

Forbidden coupling:

- shared Java implementation modules;
- shared domain model modules;
- shared DTO modules;
- shared repository modules;
- shared service modules;
- shared utility modules;
- shared test-fixture modules;
- shared logging, persistence or internal error-model modules;
- direct Gradle project dependencies between services;
- direct cross-service database access;
- shared private database tables;
- private workspace or filesystem coupling.

`contracts/` may contain interface contracts and contract documentation only.
Generated Java code, mappers, exceptions, Spring configuration, shared fixtures
and shared observability helpers must remain service-local implementation
details.

## Target Repository Shape

Mandatory target shape:

```text
services/
  repository-source-service/
  ingestion-service/
  java-parser-analysis-service/
  joern-analysis-service/
  analysis-orchestrator-service/
  query-report-api-service/
  cli-client/
  observability-stack/
  testbed/
contracts/
  grpc/
  openapi/
  events/
deployment/
  docker-compose/
  docker-swarm/
  kubernetes/
```

Optional later service roots may be added only when a later requirement or
slice explicitly introduces them.

## Target Service Responsibilities

| Service | Planned Responsibility | Primary Ownership |
|---|---|---|
| `repository-source-service` | Repository access, branch and commit resolution, checkout/fetch, workspace preparation and source snapshot descriptors | Repository workspaces, leases, source snapshots and checkout diagnostics |
| `ingestion-service` | gRPC or API intake, validation and normalization of analysis and runtime data | Raw intake, upload sessions and rejected-ingestion diagnostics |
| `java-parser-analysis-service` | JavaParser-based AST analysis and deterministic Java source facts | Java AST/source fact output, stable source IDs and unresolved-symbol diagnostics |
| `joern-analysis-service` | Joern runtime control and CPG/CFG/DFG semantic analysis | Joern execution artifacts, semantic facts and mapping diagnostics |
| `analysis-orchestrator-service` | Analysis job coordination across repository, ingestion, JavaParser and Joern services | Orchestration state, status, retry and failure coordination only |
| `query-report-api-service` | Public query/report REST API, client status views and report aggregation through owner APIs | Public API facade state and report/query response assembly |
| `cli-client` | Command-line client for public APIs | CLI state only; no analysis, parser, Joern or persistence ownership |
| `observability-stack` | Logging, metrics, tracing, dashboards and deployment observability configuration | Operational configuration and dashboards, not shared Java runtime code |
| `testbed` | Non-production integration/system test environment | Test orchestration, fixtures and environment wiring only |

## Current-To-Target Migration View

| Current evidence | FA-MSA-001 target |
|---|---|
| `services/repository-source-service` plus predecessors `forensic-analytics-adapter-repository-source`, `services/repository-analysis-service` | `repository-source-service` |
| `services/ingestion-service` plus predecessors `forensic-analytics-ingestion-grpc`, `forensic-analytics-ingestion-request`, `services/forensic-ingestion-service` | `ingestion-service` |
| `services/java-parser-analysis-service` plus predecessors `forensic-analytics-adapter-javaparser`, `services/java-ast-analysis-service` | `java-parser-analysis-service` |
| `services/joern-analysis-service` plus predecessors `forensic-analytics-adapter-joern-docker`, `services/joern-cpg-analysis-service` | `joern-analysis-service` |
| `services/analysis-orchestrator-service` plus predecessors `forensic-analytics-engine`, orchestration portions of historical application code and applicable `analysis-store-service` coordination state | `analysis-orchestrator-service` for job lifecycle, worker leases, retries, failures, dead-letter state, correlation references and job-to-artifact references |
| Predecessor `forensic-analytics-rest`, public API portions of `forensic-gateway-service` and report/query API behavior | `query-report-api-service` |
| Predecessor `forensic-analytics-cli` | `cli-client` |
| Predecessors `forensic-analytics-observability`, `forensic-analytics-logging`, deployment observability docs | `observability-stack` |
| Predecessor `forensic-analytics-testbed` | `testbed` |
| Predecessors `forensic-analytics-domain`, `forensic-analytics-application`, `forensic-analytics-persistence`, `forensic-analytics-bootstrap`, `forensic-analytics-boot-app` | Retired by ADR-0022/S05; future behavior must stay service-local or be explicitly reintroduced by requirement |

## Canonical Target Flow

```text
CLI / UI / external client
  -> query-report-api-service
  -> analysis-orchestrator-service
  -> repository-source-service
  -> java-parser-analysis-service
  -> joern-analysis-service
  -> analysis-orchestrator-service
  -> query-report-api-service
  -> client

producer / scanner / runtime collector
  -> ingestion-service
  -> analysis-orchestrator-service for workflow coordination
  -> producer-owned APIs, events, artifact references or explicit handoff contracts
```

The orchestrator coordinates only. It does not own repository checkout,
JavaParser scanning, Joern execution, public report rendering, artifact byte
custody, producer-local artifact catalogs, canonical analysis facts or another
service's private persistence.

LLM output, reports and graph/replay projections remain generated analysis or
projection output. They are never canonical evidence.

## Rollback And Strangler Strategy

The migration strategy is strangler-first:

1. Preserve historical predecessor evidence in documentation and git history
   while target boundaries are documented.
2. Reconcile ADR and arc42 names before moving code.
3. Define external contracts before service implementations depend on
   communication behavior.
4. Assign data ownership before persistence is split.
5. Add service-local implementations one service at a time.
6. Route one workflow path at a time through a service boundary after tests and
   runtime-start evidence exist.
7. Keep compatibility vocabulary only where contract governance requires it.
8. Restore or reimplement obsolete monolith behavior only with caller-free
   proof, owner decisions, contracts, tests and rollback evidence.

## Runtime Readiness Evidence Required Later

Each productive target service must later prove:

- independent build;
- independent start;
- service-local tests;
- configuration ownership;
- observability and diagnostics;
- healthcheck behavior;
- Dockerfile and container build;
- Docker Compose participation where applicable;
- Docker Swarm or Kubernetes readiness only when repository manifests and
  validation commands exist.

No slice may claim readiness before the corresponding evidence exists.

## Quality And Verification

Slice 01 is documentation-only. Required verification:

```bash
git diff --check
```

Production, build, contract, test or deployment changes in later slices must
run the applicable `QUALITY.md` gate and service-specific checks after the
service path exists.
