# Services

## Status

Service roots for the microservices ecosystem conversion workflow.

Some directories are still planning placeholders. Implemented service slices are
listed below and must remain independently buildable, testable and
containerizable without shared Java implementation modules.

## Implemented Service Slices

| Service | Status |
|---|---|
| `forensic-ingestion-service` | Slice 04 initial independent gRPC ingestion service |
| `repository-analysis-service` | Slice 06 initial independent gRPC repository preparation service |
| `analysis-store-service` | Slice 05 initial independent gRPC analysis job and artifact metadata service |
| `java-ast-analysis-service` | Slice 07 initial independent gRPC JavaParser AST analysis service |
| `joern-cpg-analysis-service` | Slice 08 initial independent gRPC Joern CPG/CFG/DFG semantic artifact service |
| `btm-generation-service` | Slice 09 initial independent gRPC Byteman/BTM generation service |

## Target Services

- `forensic-gateway-service`
- `forensic-ingestion-service`
- `repository-analysis-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `analysis-store-service`
- `graph-replay-service`
- `report-generation-service`

Each service must own its domain, application behavior, adapters, configuration,
tests, README, Dockerfile and health checks before runtime readiness is claimed.
Services must not share Java implementation modules.
