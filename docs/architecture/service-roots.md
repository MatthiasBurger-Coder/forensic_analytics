# Service Roots

## Status

Top-level service roots for the FA-MSA-001 microservice decomposition workflow.

The current repository contains top-level service projects and transitional
service slices created by earlier workflows. They are implementation evidence
and migration inputs, not production-readiness claims.

## Transitional Service Slices

| Current service directory | Status |
|---|---|
| `forensic-gateway-service` | Transitional public facade slice |
| `forensic-ingestion-service` | Transitional independent gRPC ingestion slice |
| `repository-analysis-service` | Transitional repository preparation slice |
| `analysis-store-service` | Transitional analysis job and artifact metadata slice |
| `java-ast-analysis-service` | Transitional JavaParser AST analysis slice |
| `joern-cpg-analysis-service` | Transitional Joern CPG/CFG/DFG semantic artifact slice |
| `btm-generation-service` | Transitional BTM generation slice; optional for mandatory FA-MSA-001 closure unless a later requirement makes it mandatory |

These slices must not be treated as compatibility aliases for the FA-MSA-001
target names. Later workflow slices may move, split, replace or retire them
only after contracts, caller evidence, tests and rollback notes are verified.

## FA-MSA-001 Target Services

- `repository-source-service`
- `ingestion-service`
- `java-parser-analysis-service`
- `joern-analysis-service`
- `analysis-orchestrator-service`
- `query-report-api-service`
- `cli-client`
- `observability-stack`
- `testbed`

Each productive backend service must own its domain, application behavior,
adapters, configuration, tests, README, Dockerfile and health checks before
runtime readiness is claimed. Services must not share Java implementation
modules. `cli-client` is a public API client boundary, not a productive backend
service.

## Optional Later Services

- `btm-generation-service`
- `graph-replay-service`
- `report-generation-service`
- `incident-analysis-service`

Optional services are added only by later requirements or workflow slices.
`report-generation-service` is not a FA-MSA-001 mandatory service; report and
query API responsibility moves first to `query-report-api-service`.

## Implemented Target Service Evidence

- `repository-source-service`
- `ingestion-service`
- `java-parser-analysis-service`
- `joern-analysis-service`
- `analysis-orchestrator-service`
- `query-report-api-service`
- `cli-client` (public API client boundary, not a productive backend service)
- `observability-stack` (deployment observability boundary, not a productive backend service)
- `testbed` (non-production integration and system-test boundary)

These target-name service roots, the public API client boundary and the
deployment observability boundary are additive migration evidence. Their
predecessor service directories and legacy source trees are historical
pre-retirement evidence until the final legacy source-tree retirement workflow
proves caller migration, deletion, architecture closure and quality gates.

S11 creates `cli-client` as an independently buildable public API
client. It must not be treated as a productive backend service. The legacy
CLI source tree records historical local `analyze` and `ingest-request`
behavior before final retirement; target client behavior must not be silently
routed through the public API.

S12 creates `observability-stack` as deployment-oriented
observability policy material. It is not a shared Java logging or
observability module, and it does not claim Docker Compose, Swarm or
Kubernetes runtime readiness.

S13 creates `testbed` as non-production integration and system-test
infrastructure. It preserves predecessor testbed coverage in a service-root
location as service-root regression evidence; the legacy testbed source tree is
historical rollback evidence pending final retirement.

S14 records `NO_REMOVAL_SAFE` for direct legacy module retirement. The
previously retained legacy source trees are historical in-process and rollback
evidence, not shared service implementation modules for productive target
services. The final legacy source-tree retirement workflow must remove only
verified caller-free paths.

## Local Runtime Evidence

The existing local repository-to-BTM service landscape is documented in
`deployment/docker-compose/repository-to-btm.local.yml`. It is current
Docker-local configuration evidence for transitional repository-to-BTM service
slices and for the FA-MVP-0001 `repository-source-service` private workspace
volume and PostgreSQL metadata configuration. It does not claim image startup, health-check smoke
testing, the full FA-MSA-001 target landscape, Docker Swarm or Kubernetes
deployment readiness.
