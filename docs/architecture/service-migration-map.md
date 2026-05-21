# Service Migration Map

## Status

FA-MSA-001 Slice 01 service-boundary and migration planning map.

This document maps current modular-monolith and transitional service evidence
to FA-MSA-001 target ownership candidates. It does not move production code,
create build projects, create service implementations or claim runtime
readiness.

## Target Mapping

| FA-MSA-001 target | Current source evidence | Current coupling | Planned migration path | Required contract first | Data owner status | Forbidden moves | Verification needed |
|---|---|---|---|---|---|---|---|
| `repository-source-service` | `forensic-analytics-adapter-repository-source`, `services/repository-analysis-service` | Checkout and source snapshot behavior still has monolith application/domain dependencies in legacy adapter paths | Move repository access, branch resolution, checkout/fetch, workspace preparation and source snapshot descriptors into a service-local boundary | Repository source/snapshot REST, gRPC or file contract | Repository service owns workspaces and source snapshots | No private workspace sharing; no target repository code execution without sandbox approval | Service-local architecture, Git safety, checkout diagnostics, build/start and Dockerfile checks |
| `ingestion-service` | `forensic-analytics-ingestion-grpc`, `forensic-analytics-ingestion-request`, `services/forensic-ingestion-service` | Legacy gRPC/request modules still depend on central application/domain modules | Move intake, validation, normalization and request import behavior into service-local domain/application/adapters | Ingestion gRPC/API contract | Raw intake and upload sessions owned by ingestion; canonical ownership resolved by S04 | No shared generated Java DTO module; no canonical fact writes without owner decision | gRPC/API contract tests, validation tests and missing-field diagnostics |
| `java-parser-analysis-service` | `forensic-analytics-adapter-javaparser`, `services/java-ast-analysis-service` | JavaParser adapter shares application/domain models in legacy path | Move JavaParser AST scanning and static source-fact extraction into service-local boundary | JavaParser analysis and source-fact artifact contracts | JavaParser service owns worker output until accepted or transferred | No runtime execution claims from static facts; no JavaParser API leakage into neutral contracts | Source-location, unresolved-symbol, deterministic ID and artifact retrieval tests |
| `joern-analysis-service` | `forensic-analytics-adapter-joern-docker`, `services/joern-cpg-analysis-service`, `docker/joern/**` | Joern adapter shares ports and monolith domain/application models in legacy path | Move Joern Docker control, CPG/CFG/DFG analysis and semantic mapping into service-local boundary | Joern analysis artifact contract | Joern service owns execution artifacts until accepted or transferred | No shared CPG filesystem coupling; no Repository Source private workspace mounts | Joern unavailable, timeout, mapping and Docker-boundary tests |
| `analysis-orchestrator-service` | `forensic-analytics-engine`, orchestration parts of `forensic-analytics-application`, coordination/status parts of `services/analysis-store-service` | Current orchestration is in-process and mixed with application and persistence concerns | Create orchestration-only service after contracts and data ownership are explicit | Analysis orchestration API or event contracts | Orchestration state owner must be confirmed by S04 | No repository checkout, parser, Joern, report or persistence internals inside orchestrator | Job lifecycle, retry, timeout, status, failure and no-hidden-monolith tests |
| `query-report-api-service` | `forensic-analytics-rest`, public facade parts of `services/forensic-gateway-service`, report/query concepts | Current REST and Gateway-style behavior are not the FA-MSA-001 query/report API target | Create public API facade that queries owner APIs and assembles reports/status only | REST/OpenAPI query/report contract | Public facade state only unless S04 assigns a narrow owner | No analysis execution, checkout, Joern, JavaParser or direct DB access | OpenAPI contract, redaction, error mapping and frontend/CLI compatibility tests |
| `cli-client` | `forensic-analytics-cli` | CLI currently has local in-process analysis and ingestion-request dependencies | Move CLI to public API client behavior and retire local business logic only after parity/deprecation tests | CLI/public API contract | CLI owns no forensic data | No parser, Joern, persistence, service implementation or domain logic in CLI | CLI contract tests, output redaction and legacy command parity/deprecation tests |
| `observability-stack` | `forensic-analytics-observability`, `forensic-analytics-logging`, deployment docs | Central observability/logging Java modules are monolith coupling for target services | Replace shared Java logging/observability modules with service-local configuration and deployment observability material | Operational configuration contracts where needed | No forensic evidence ownership | No shared Java logging library; no diagnostics as evidence | Dependency scans, logging redaction and deployment-doc verification |
| `testbed` | `forensic-analytics-testbed`, Compose docs and service-local tests | Testbed depends on many monolith modules for regression coverage | Move system/integration test orchestration to non-production testbed after replacement service E2E exists | Test environment contracts or Compose files when needed | Test data only | No production service dependency on testbed source or fixtures | Service E2E tests, no production dependency checks and Compose validation when used |

## Central Module Retirement Map

| Current module | Target decision |
|---|---|
| `forensic-analytics-domain` | Split into service-local domain models; remove only after all callers are migrated or deprecated. |
| `forensic-analytics-application` | Split into service-local application/use-case code; remove only after service owners and contracts are verified. |
| `forensic-analytics-persistence` | Replace with service-local persistence adapters after S04 assigns one-writer ownership. |
| `forensic-analytics-logging` | Replace with service-local logging configuration or `observability-stack` deployment material. |
| `forensic-analytics-observability` | Replace with service-local diagnostics/correlation configuration or deployment observability material. |
| `forensic-analytics-bootstrap` | Retire after service-local bootstraps and runtime start paths are verified. |
| `forensic-analytics-boot-app` | Retire after mandatory service runtime paths and rollback evidence exist. |
| `forensic-analytics-engine` | Retire or split into `analysis-orchestrator-service` after orchestration ownership is explicit. |
| `forensic-analytics-rest` | Retire after `query-report-api-service` has public API parity and caller migration. |

## Current Implementation Evidence

The existing `forensic-analytics-*` Gradle modules remain the current
implementation baseline until later slices move behavior behind verified
contracts. S01 does not rename modules, move packages, copy production logic or
register service builds.

The current service directories are transitional evidence:

- `services/forensic-gateway-service`;
- `services/forensic-ingestion-service`;
- `services/repository-analysis-service`;
- `services/analysis-store-service`;
- `services/java-ast-analysis-service`;
- `services/joern-cpg-analysis-service`;
- `services/btm-generation-service`;
- `services/graph-replay-service`;
- `services/report-generation-service`.

These directories are not FA-MSA-001 compatibility aliases. Later slices may
move, replace, split or retire them only with verified caller evidence,
contracts, tests and rollback notes.

## Migration Sequencing

1. Keep current modules unchanged.
2. Reconcile target service names in ADR and arc42 documentation.
3. Refresh caller and coupling inventory.
4. Define external contracts before service implementations depend on
   communication behavior.
5. Assign data ownership and persistence boundaries.
6. Add or migrate service-local implementations one service at a time.
7. Verify service independence before routing runtime behavior.
8. Remove obsolete monolith paths only after replacement evidence exists.

## Stop Conditions

Stop a later migration step when:

- a target owner is unclear;
- a data owner or write path is unclear;
- a contract would require guessing fields, endpoints, topics or files;
- a service would depend on another service's Java classes;
- shared common Java modules are proposed;
- direct cross-service database access is proposed;
- runtime readiness is claimed without build, start, test, healthcheck,
  container and deployment evidence.
