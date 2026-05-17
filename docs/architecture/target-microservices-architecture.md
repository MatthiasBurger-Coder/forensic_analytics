# Target Microservices Architecture

## Status

Slice 01 target architecture baseline for the microservices ecosystem
conversion workflow.

This document defines the planned service landscape. It does not claim that
the services are implemented, independently deployable, health-checkable,
containerized or production-ready. Slice 00 verified that the current
repository is still a modular monolith.

## Architecture Decision

The target service landscape is the active workflow landscape:

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

The current platform does not have:

- `services/**` roots;
- external `contracts/**` roots;
- service-private databases;
- service-local health checks;
- service-local Dockerfiles under `services/**`;
- service-local READMEs;
- Docker Compose service landscape;
- Docker Swarm or Kubernetes manifests.

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

Creating these roots is Slice 02 or later work. This Slice 01 document only
defines the target boundaries.

## Target Service Responsibilities

| Service | Planned Responsibility | Primary Ownership |
|---|---|---|
| `forensic-gateway-service` | External API and UI/CLI facade; analysis-job orchestration facade | Public API surface and orchestration state, not analysis facts |
| `forensic-ingestion-service` | gRPC intake and validation of plugin, scanner and runtime evidence packages | Raw ingestion intake and upload-session lifecycle |
| `repository-analysis-service` | Repository checkout, branch resolution, workspace preparation and source snapshot preparation | Repository workspaces, leases and checkout diagnostics |
| `java-ast-analysis-service` | JavaParser source scanning, stable source identifiers and unresolved-symbol diagnostics | AST execution output until accepted by Analysis Store |
| `joern-cpg-analysis-service` | Joern runtime, CPG/CFG/DFG analysis and semantic artifact mapping | Joern execution artifacts and semantic worker output |
| `btm-generation-service` | Deterministic Byteman/BTM artifacts from delivered analysis facts | Generated BTM rule artifacts |
| `analysis-store-service` | Authoritative normalized facts, analysis sessions, jobs, incidents, correlations and artifact catalog | Canonical evidence and one-writer analysis state |
| `graph-replay-service` | Graph/runtime overlays and exception-centered replay | Rebuildable graph/replay projections |
| `report-generation-service` | Reports, incident context packages and LLM-ready/generated packages | Report artifacts and generated analysis packages |
| `frontend-web-app` | React user interface through Gateway/public APIs only | UI state only, no forensic data ownership |

## Canonical Data Flow

Planned analysis flow:

```text
Frontend / CLI / external client
  -> Gateway
  -> Ingestion / analysis job APIs
  -> Repository Analysis
  -> Java AST Analysis
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
7. Remove obsolete monolith paths only in Slice 17 after replacement evidence.

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
