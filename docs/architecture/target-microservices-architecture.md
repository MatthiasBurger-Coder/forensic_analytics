# Target Microservices Architecture

## Status

Slice 01 target architecture baseline for the microservices ecosystem
conversion workflow.

This document defines the target service landscape. Seven service slices are
now registered Gradle projects with service-local documentation, tests and
Dockerfiles. This does not claim that the full landscape is independently
deployable, health-checkable, containerized for production or complete.

## Architecture Decision

The target service landscape is the active workflow landscape:

- `forensic-gateway-service`
- `forensic-ingestion-service`
- `repository-analysis-service`
- `build-artifact-worker-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`
- `analysis-store-service`
- `graph-replay-service`
- `report-generation-service`
- `frontend-web-app`

This replaces older arc42 planning names such as `forensic-server` and
`*-worker` roots for the active migration workflow. Older names remain
historical context only.

## Current Baseline

The current platform has:

- one Gradle multi-project build;
- one Spring Boot application boundary;
- one manual bootstrap runtime;
- in-process REST and gRPC adapters;
- in-memory persistence;
- one source-owned gRPC proto inside an implementation module;
- a separate `forensic-ui` frontend;
- Boot, Joern and frontend Docker material.

The current platform has seven implemented service slices:

- `forensic-gateway-service`;
- `forensic-ingestion-service`;
- `repository-analysis-service`;
- `analysis-store-service`;
- `java-ast-analysis-service`;
- `joern-cpg-analysis-service`;
- `btm-generation-service`.

The current platform still does not have:

- implemented graph-replay, report-generation or build-artifact-worker
  executable services;
- service-private databases;
- fully verified service-local health checks for the full target landscape;
- production-wide Docker Compose service landscape;
- Docker Swarm or Kubernetes manifests.

Slice 15 verifies a local Docker Compose landscape for the repository-to-BTM
service path only. That evidence does not claim full target-landscape,
Docker Swarm or Kubernetes readiness.

## Target Principles

Each service owns its internal hexagonal architecture:

```text
domain
application
adapter/inbound
adapter/outbound
infrastructure
```

Allowed service integration mechanisms:

- REST/OpenAPI;
- gRPC/protobuf;
- approved event contracts.

Forbidden coupling:

- shared Java implementation modules;
- shared domain model modules;
- shared DTO modules;
- shared repository modules;
- shared service modules;
- shared utility modules;
- shared test-fixture modules;
- shared internal error-model modules;
- direct class dependencies between services;
- direct cross-service database access;
- shared private database tables.

`contracts/` may contain interface contracts and contract documentation only.
Generated Java code, mappers, exceptions, Spring configuration, shared
fixtures and shared observability helpers must not become shared service
runtime libraries.

## Target Repository Shape

Planned target shape:

```text
services/
  forensic-gateway-service/
  forensic-ingestion-service/
  repository-analysis-service/
  build-artifact-worker-service/
  java-ast-analysis-service/
  joern-cpg-analysis-service/
  btm-generation-service/
  analysis-store-service/
  graph-replay-service/
  report-generation-service/
frontend/
  frontend-web-app/
contracts/
  grpc/
  openapi/
  events/
deployment/
  docker-compose/
  docker-swarm/
  kubernetes/
```

Most target roots now exist. Graph-replay and report-generation are README-only
planned roots and are explicitly deferred from repository-to-BTM pipeline
acceptance by Slice 16. Build-artifact-worker, frontend migration and
production deployment roots still require later implementation slices before
they can be treated as executable runtime paths.

## Target Service Responsibilities

| Service | Planned Responsibility | Primary Ownership |
|---|---|---|
| `forensic-gateway-service` | External API, UI/CLI facade, public request/status facade and public BTM delivery facade | Public facade state only, not worker orchestration state, analysis facts or artifact bytes |
| `forensic-ingestion-service` | gRPC intake and validation of plugin, scanner and runtime evidence packages | Raw ingestion intake and upload-session lifecycle |
| `repository-analysis-service` | Repository checkout, branch resolution, workspace preparation and source snapshot preparation | Repository workspaces, leases and checkout diagnostics |
| `build-artifact-worker-service` | Optional sandboxed production of complete build-output packages for pinned source snapshots | Build-output package bytes, manifests, checksums and retrieval references when introduced |
| `java-ast-analysis-service` | JavaParser source scanning, stable source identifiers, source-fact artifact byte retrieval and unresolved-symbol diagnostics | AST execution output and produced source-fact bytes until accepted or transferred through an explicit byte-handoff contract |
| `joern-cpg-analysis-service` | Joern runtime, CPG/CFG/DFG analysis and semantic artifact mapping | Joern execution artifacts and semantic worker output |
| `btm-generation-service` | Deterministic Byteman/BTM artifacts from delivered analysis facts | Generated BTM rule artifacts |
| `analysis-store-service` | Authoritative normalized facts, analysis sessions, jobs, Slice 11 repository-to-BTM orchestration readiness state, Slice 12 source-fact retrieval readiness, incidents, correlations and artifact catalog | Canonical evidence, worker-dispatch/job-graph state and one-writer analysis state |
| `graph-replay-service` | Graph/runtime overlays and exception-centered replay | Rebuildable graph/replay projections |
| `report-generation-service` | Reports, incident context packages and LLM-ready/generated packages | Report artifacts and generated analysis packages |
| `frontend-web-app` | React user interface through Gateway/public APIs only | UI state only, no forensic data ownership |

## Canonical Data Flow

Planned analysis flow:

```text
Frontend / CLI / external client
  -> Gateway
  -> Analysis Store orchestration owner API
  -> Repository Analysis
  -> Build Artifact Worker or verified external artifact producer
  -> Java AST Analysis
  -> Java AST owner source-fact byte retrieval
  -> Joern CPG Analysis
  -> Analysis Store
  -> Graph Replay
  -> Report Generation
  -> Gateway
  -> Frontend / client
```

Planned plugin-ingestion flow:

```text
Plugin / scanner / runtime collector
  -> Forensic Ingestion Service
  -> Analysis Store
  -> downstream projections and reports through contracts
```

Planned replay flow:

```text
Exception or correlation context
  -> Gateway
  -> Graph Replay Service
  -> Analysis Store owner APIs
  -> replay projection and gaps
  -> Gateway / Report Generation
```

Planned report and LLM package flow:

```text
Report request
  -> Gateway
  -> Report Generation Service
  -> Analysis Store owner APIs
  -> Graph Replay APIs
  -> reproducible report or LLM-ready package
```

LLM output remains generated analysis or hypothesis. It is never canonical
evidence.

## Rollback And Strangler Strategy

The migration strategy is strangler-first:

1. Preserve the current modular monolith while target boundaries are
   documented.
2. Create target roots without moving business logic blindly.
3. Define external contracts before service implementations.
4. Add independent service implementations behind contracts.
5. Route one workflow path at a time through a service boundary after tests
   and runtime-start evidence exist.
6. Keep the old in-process path until the replacement path has verified
   contract, integration and quality evidence.
7. Remove obsolete monolith paths only after replacement evidence exists and
   the active workflow's monolith-retirement milestone approves the change.

Rollback for behavior-changing slices must keep the previous in-process path
available until the service path is proven and documented.

## Runtime Readiness Evidence Required Later

Each service must later prove:

- independent build;
- independent start;
- service-local tests;
- configuration ownership;
- observability and diagnostics;
- healthcheck behavior;
- container build;
- Docker Compose participation;
- Docker Swarm readiness;
- Kubernetes readiness where manifests are added.

No later slice may claim readiness before the corresponding evidence exists.

## Quality And Verification

Slice 01 is documentation-only. Required verification:

```bash
git diff --check
```

Production, build, contract, test or deployment changes in later slices must
run the applicable `QUALITY.md` gate and service-specific checks after the
service path exists.
