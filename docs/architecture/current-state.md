# Current State

## Status

Post-S05 current-state closure for the legacy source-tree retirement workflow.

This document began as the Slice 00 baseline for the microservices ecosystem
conversion workflow. ADR-0022 and workflow slice S05 supersede that baseline
for the Java implementation tree: the former `forensic-analytics-*` source
trees were removed in checkpoint `d8d9dab`, and the active Gradle Java model is
service-only as top-level service projects. Historical module names in this
document are
pre-retirement predecessor evidence, not active source roots or runtime units.

## Verification Sources

Verified from:

- `settings.gradle.kts`
- root and module `build.gradle.kts` files
- `QUALITY.md`
- `docs/workflow/workflow.md`
- `docs/workflow/execution-report.md`
- `docs/architecture/microservice-governance.md`
- `docs/arc42/**`
- `docs/adr/**`
- service source and test trees under top-level service roots (`*-service/**`, `cli-client/**`, `observability-stack/**`, `testbed/**`)
- external contracts under `contracts/**`
- ADR-0022 final source-tree retirement decision
- `docker/**`
- `forensic-ui/**`

Required Slice 00 role reviews were completed by Senior System Architect,
Microservice Senior Expert, Senior Java Backend Developer, Senior DevOps
Engineer and Senior Tester. The reviews reported no blocker for documenting
the current state.

## Repository Shape

| Area | Current Evidence | Current Status |
|---|---|---|
| Java build | `settings.gradle.kts`, root `build.gradle.kts` | One Gradle multi-project build |
| Frontend | `forensic-ui/package.json` | Separate Vite/React project, not a Gradle module |
| Boot runtime | Historical `forensic-analytics-boot-app` predecessor; source tree retired by S05 | No active monolith Boot runtime source tree remains |
| Bootstrap runtime | Historical `forensic-analytics-bootstrap` predecessor; source tree retired by S05 | No active combined gRPC/REST bootstrap source tree remains |
| gRPC contracts | `contracts/grpc/**` plus service-local generated code | External contract documentation and service-local consumption only |
| REST API | `contracts/openapi/**`, `query-report-api-service` | Public API facade evidence for repository-analysis submission/status and FA-MVP-0001 workspace routes, not the deleted monolith REST adapter |
| Persistence | Service-local stores where implemented; `repository-source-service` uses service-local H2 file persistence for FA-MVP-0001 repository checkout workspace, branch and idempotency state; no shared Java persistence module | Durable production analytics persistence remains future work |
| Docker | `docker/boot-app`, `docker/joern`, `forensic-ui/Dockerfile` | Existing container material, not a service ecosystem |
| Service roots | top-level `*-service/`, `cli-client/`, `observability-stack/`, `testbed/` | Eighteen registered service-root projects; graph-replay/report-generation remain planned roots and the build-artifact worker has no root yet |
| External contracts root | `contracts/` | Present with gRPC, OpenAPI and event contract documentation |
| Deployment roots | `deployment/**` planning roots | Compose transition descriptor and Swarm/Kubernetes README roots exist; production stack files, manifests, charts and validation commands are not verified |

## Current Gradle Modules

Verified modules from `settings.gradle.kts`:

| Module | Current Responsibility |
|---|---|
| `analysis-orchestrator-service` | Repository-to-BTM request acceptance and process-local status/readiness state |
| `analysis-store-service` | Transitional job and artifact metadata service evidence |
| `btm-generation-service` | Registered BTM generation service evidence |
| `cli-client` | Public API client boundary |
| `forensic-gateway-service` | Predecessor public gateway service evidence |
| `forensic-ingestion-service` | Predecessor ingestion service evidence |
| `ingestion-service` | Target raw intake and validation service |
| `java-ast-analysis-service` | Predecessor Java AST service evidence |
| `java-parser-analysis-service` | Target JavaParser source-fact service |
| `joern-analysis-service` | Target Joern semantic-analysis service |
| `joern-cpg-analysis-service` | Predecessor Joern CPG service evidence |
| `observability-stack` | Operational observability boundary, not shared Java runtime code |
| `query-report-api-service` | Public query/report API facade |
| `repository-analysis-service` | Predecessor repository-analysis service evidence |
| `repository-source-service` | Target repository source and workspace preparation service |
| `testbed` | Non-production integration and system-test boundary |

## Current Capability Map

| Capability | Verified Current Evidence | Migration Note |
|---|---|---|
| Repository checkout and workspace preparation | `repository-source-service` with FA-MVP-0001 H2 repository checkout workspace state | Target source/workspace ownership evidence; H2 is Docker-local MVP persistence only |
| Java AST analysis | `java-parser-analysis-service` | Target JavaParser source-fact evidence |
| Joern / CPG semantic analysis | `joern-analysis-service`, `docker/joern/**` | Target service plus optional Joern Docker infrastructure evidence |
| BTM / rule generation | `btm-generation-service`, `contracts/grpc/btm-generation.proto`, arc42 notes | Registered service evidence and provisional contract only; production readiness remains unverified |
| gRPC ingestion | `contracts/grpc/**`, `ingestion-service` | Target intake service consumes external contracts with service-local generated code |
| REST API | `contracts/openapi/**`, `query-report-api-service` | Public API facade evidence for repository-analysis submission/status and FA-MVP-0001 workspace routes |
| Persistence | Service-local stores where implemented, including repository-source H2 for repository checkout workspace state | Durable production database ownership remains unverified |
| Boot runtime | Historical Boot predecessor source tree retired by S05 | No active monolith Boot source tree remains |
| Manual server runtime | Historical Bootstrap predecessor source tree retired by S05 | No active combined runtime source tree remains |
| CLI | `cli-client` | Public API client boundary; local predecessor CLI source tree retired by S05 |
| Frontend | `forensic-ui` | Existing Vite/React frontend outside planned `frontend/frontend-web-app` root |
| Observability and logging | `observability-stack`, service-local diagnostics | No shared Java logging/observability module remains in the active build |
| Graph, replay, report and LLM target concepts | arc42 docs, ADRs and partial domain/application model evidence | Not verified as standalone runtime capabilities or services |

## Current Runtime Composition

The active Java runtime evidence is service-local in top-level service roots. S05
removed the former Boot, Bootstrap, REST, CLI, engine, ingestion-request and
testbed source trees. Their behavior remains historical predecessor evidence in
documentation and git history only. ADR-0022 defines rollback as reverting the
S05 checkpoint commit, not keeping active legacy runtime source trees.

The repository still does not prove independently deployable production
microservices for the whole FA-MSA-001 target landscape. Runtime, container,
healthcheck, Swarm and Kubernetes readiness require later verified deployment
work and must not be inferred from source-tree deletion.

## Current Frontend

The verified frontend root is `forensic-ui`.

Verified scripts from `forensic-ui/package.json`:

- `npm run dev`
- `npm run build`
- `npm run preview`
- `npm test`
- `npm run test:watch`

`forensic-ui/Dockerfile` builds the frontend with `npm ci` and
`npm run build`, then serves the built assets with nginx. The planned
`frontend/frontend-web-app` root exists only as a planned placeholder. It has
no frontend implementation, package metadata, build configuration or tests.

## Current Deployment Material

Verified existing deployment-related material:

- `docker/boot-app/README.md` as historical Boot-app Docker documentation
- `docker/joern/Dockerfile`
- `docker/joern/docker-compose.joern.yml`
- `docker/joern/scripts/**`
- `forensic-ui/Dockerfile`
- `deployment/docker-compose/repository-to-btm.local.yml`
- `deployment/docker-swarm/README.md` as planning documentation only
- `deployment/kubernetes/README.md` as planning documentation only

Missing target material:

- production-wide `deployment/docker-compose/docker-compose.yml`
- Docker Swarm stack files and validation commands
- Kubernetes manifests, Helm charts and validation commands
- CI workflow files under `.github/workflows`

Docker Compose model validation is verified for Joern tooling and the
Docker-local repository-to-BTM descriptor. Slice S09 verified
`deployment/docker-compose/repository-to-btm.local.yml` with
`docker compose ... config` and added repository-source private workspace and
H2 data volumes. That is Docker-local MVP configuration evidence only; image
builds, startup, health-check smoke tests, production-wide Compose, Swarm
stacks, Kubernetes manifests and complete service-by-service container
readiness remain unverified.

## Microservice Readiness Baseline

The current repository has limited platform-level runtime evidence:

- service-local bootstrap/runtime evidence where implemented;
- service-local Dockerfiles for implemented services;
- external gRPC contracts under `contracts/grpc/**` plus service-local generated code;
- one React frontend package and container baseline;
- Joern Docker tooling.

The current repository does not have verified per-service:

- service-private production databases;
- production-wide Docker Compose service landscape;
- Docker Swarm stack files, Kubernetes manifests or Helm charts.

Eighteen registered service projects have service-local READMEs, and sixteen
have implementation/test trees.
Thirteen registered service projects have service-local Dockerfiles;
`cli-client`, `observability-stack` and `testbed`
intentionally have no Dockerfile in the current repository state.
Graph-replay and report-generation remain planned roots with placeholder build
scripts and no implementation. The
build-artifact worker is a planned target with no service root yet. Health
check and production runtime readiness evidence remains incomplete outside
service-specific README evidence, Joern Compose model validation and the S09
Docker-local repository-to-BTM Compose model validation.

Future slices must treat README-only service roots as planned targets. Registered
service projects still need explicit start, healthcheck, orchestration and
deployment evidence before production readiness is claimed.
