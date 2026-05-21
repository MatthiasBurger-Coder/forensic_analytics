# Services

## Status

Service roots for the FA-MSA-001 microservice decomposition workflow.

The current repository contains transitional service slices created by earlier
workflows. They are implementation evidence and migration inputs, not the final
FA-MSA-001 service names and not production-readiness claims.

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

Each productive service must own its domain, application behavior, adapters,
configuration, tests, README, Dockerfile and health checks before runtime
readiness is claimed. Services must not share Java implementation modules.

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

These target-name service roots are additive migration evidence. Their
predecessor service directories and legacy modules remain rollback/current
state evidence until later retirement slices prove caller migration, parity and
quality gates.

## Local Runtime Evidence

The existing local repository-to-BTM service landscape is documented in
`deployment/docker-compose/repository-to-btm.local.yml`. It is current
evidence for transitional service slices only. It does not claim that the
FA-MSA-001 target landscape, Docker Swarm or Kubernetes deployment is ready.
