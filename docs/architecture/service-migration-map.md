# Service Migration Map

## Status

FA-MSA-001 Slice 04 service-boundary, migration inventory and
data-ownership map with Slice 05 repository-source and Slice 06 ingestion
implementation evidence.

This document maps current modular-monolith and transitional service evidence
to FA-MSA-001 target ownership decisions. It does not move production code,
create build projects, create service implementations, define storage
technology or claim runtime readiness. S02 refreshed this map after caller and
Gradle coupling scans and confirmed that no central module retirement is safe
yet. S04 records one-writer target data ownership before persistence migration
starts.

## Target Mapping

| FA-MSA-001 target | Current source evidence | Current coupling | Planned migration path | Required contract first | Data owner status | Forbidden moves | Verification needed |
|---|---|---|---|---|---|---|---|
| `repository-source-service` | `services/repository-source-service`; predecessors `forensic-analytics-adapter-repository-source`, `services/repository-analysis-service` | S05 target service is service-local and registered; legacy adapter paths still have monolith application/domain dependencies and the predecessor service remains as rollback input | Move repository access, branch resolution, checkout/fetch, workspace preparation, source package byte custody and source snapshot descriptors into a service-local boundary; later slices must route callers and retire predecessor paths only after parity evidence | Repository source/snapshot REST, gRPC or file contract; S05 uses predecessor `repository-analysis.proto` wire shape as transitional external contract only | Owns workspaces, leases, cleanup, source package bytes, source snapshot descriptors and accepted source metadata | No private workspace sharing; no target repository code execution without sandbox approval; no JavaParser or Joern handoff logic inside repository source | Service-local architecture, Git safety, checkout diagnostics, build/start and Dockerfile checks |
| `ingestion-service` | `services/ingestion-service`; predecessors `forensic-analytics-ingestion-grpc`, `forensic-analytics-ingestion-request`, `services/forensic-ingestion-service` | S06 target service is service-local and registered; legacy gRPC/request modules still depend on central application/domain modules and predecessor service remains as rollback input | Move intake, validation, normalization and request import behavior into service-local domain/application/adapters; later slices must route callers and retire predecessor paths only after parity evidence | Ingestion gRPC/API contract; S06 keeps `forensic-ingestion.proto` wire shape unchanged | Owns raw intake, upload sessions, raw runtime or analysis payload byte custody before handoff and rejected-ingestion diagnostics | No shared generated Java DTO module; no static/semantic canonical fact writes; no repository checkout responsibility | gRPC/API contract tests, validation tests, request-manifest tests and missing-field diagnostics |
| `java-parser-analysis-service` | `forensic-analytics-adapter-javaparser`, `services/java-ast-analysis-service` | JavaParser adapter shares application/domain models in legacy path | Move JavaParser AST scanning and static source-fact extraction into service-local boundary | JavaParser analysis and source-fact artifact contracts | Owns canonical static Java source facts it produces, source-fact artifact bytes and producer-local artifact metadata | No runtime execution claims from static facts; no JavaParser API leakage into neutral contracts | Source-location, unresolved-symbol, deterministic ID and artifact retrieval tests |
| `joern-analysis-service` | `forensic-analytics-adapter-joern-docker`, `services/joern-cpg-analysis-service`, `docker/joern/**` | Joern adapter shares ports and monolith domain/application models in legacy path | Move Joern Docker control, CPG/CFG/DFG analysis and semantic mapping into service-local boundary | Joern analysis artifact contract | Owns canonical Joern semantic facts it produces, semantic artifact bytes and producer-local artifact metadata | No shared CPG filesystem coupling; no Repository Source private workspace mounts | Joern unavailable, timeout, mapping and Docker-boundary tests |
| `analysis-orchestrator-service` | `forensic-analytics-engine`, orchestration parts of `forensic-analytics-application`, coordination/status parts of `services/analysis-store-service` | Current orchestration is in-process and mixed with application and persistence concerns | Create orchestration-only service after contracts and data ownership are explicit | Analysis orchestration API or event contracts | Owns job lifecycle, workflow status, worker leases/attempts, retry/timeout/failure/dead-letter state, correlation references and job-to-artifact references | No repository checkout, parser, Joern, report, artifact byte custody, producer catalog or canonical fact storage inside orchestrator | Job lifecycle, retry, timeout, status, failure and no-hidden-monolith tests |
| `query-report-api-service` | `forensic-analytics-rest`, public facade parts of `services/forensic-gateway-service`, report/query concepts | Current REST and Gateway-style behavior are not the FA-MSA-001 query/report API target | Create public API facade that queries owner APIs and assembles reports/status only | REST/OpenAPI query/report contract | Owns public read models, public cache state, generated report packages, LLM-ready packages and stored generated LLM output only as labeled generated analysis or hypotheses | No analysis execution, checkout, Joern, JavaParser, direct DB access or canonical evidence ownership | OpenAPI contract, redaction, error mapping and frontend/CLI compatibility tests |
| `cli-client` | `forensic-analytics-cli` | CLI currently has local in-process analysis and ingestion-request dependencies | Move CLI to public API client behavior and retire local business logic only after parity/deprecation tests | CLI/public API contract | CLI owns no forensic data | No parser, Joern, persistence, service implementation or domain logic in CLI | CLI contract tests, output redaction and legacy command parity/deprecation tests |
| `observability-stack` | `forensic-analytics-observability`, `forensic-analytics-logging`, deployment docs | Central observability/logging Java modules are monolith coupling for target services | Replace shared Java logging/observability modules with service-local configuration and deployment observability material | Operational configuration contracts where needed | No forensic evidence ownership | No shared Java logging library; no diagnostics as evidence | Dependency scans, logging redaction and deployment-doc verification |
| `testbed` | `forensic-analytics-testbed`, Compose docs and service-local tests | Testbed depends on many monolith modules for regression coverage | Move system/integration test orchestration to non-production testbed after replacement service E2E exists | Test environment contracts or Compose files when needed | Test data only | No production service dependency on testbed source or fixtures | Service E2E tests, no production dependency checks and Compose validation when used |

## Central Module Retirement Map

Every row in this table is blocked until a later slice proves caller-free
evidence, replacement parity or explicit deprecation, rollback or
operator-visible migration notes and the relevant `QUALITY.md` quality gate.

| Current module | Target decision | Retirement gate |
|---|---|---|
| `forensic-analytics-domain` | Split into service-local domain models. | Caller-free evidence across production code, tests, build files and docs; service-local domain parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-application` | Split into service-local application/use-case code. | Caller-free evidence; verified service owners and contracts; replacement parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-persistence` | Replace with service-local persistence adapters owned by the services named in the S04 ownership matrix. | Caller-free evidence; service-local persistence replacement for each S04 owner; replacement parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-logging` | Replace with service-local logging configuration or `observability-stack` deployment material. | Caller-free evidence; service-local diagnostics or deployment replacement; explicit redaction behavior; rollback/operator note; required quality gate. |
| `forensic-analytics-observability` | Replace with service-local diagnostics/correlation configuration or deployment observability material. | Caller-free evidence; service-local observability replacement; replacement parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-bootstrap` | Retire after service-local bootstraps and runtime start paths are verified. | Caller-free evidence; service-local start/health/container parity; rollback/operator note; required quality gate. |
| `forensic-analytics-boot-app` | Retire after mandatory service runtime paths and rollback evidence exist. | Caller-free evidence; mandatory service runtime parity; rollback/operator note; required quality gate. |
| `forensic-analytics-engine` | Retire or split into `analysis-orchestrator-service` after orchestration ownership is explicit. | Caller-free evidence; orchestration API parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-rest` | Retire after `query-report-api-service` has public API parity and caller migration. | Caller-free evidence; public API parity or explicit deprecation; rollback/operator note; required quality gate. |

## Current Implementation Evidence

The existing `forensic-analytics-*` Gradle modules remain the current
implementation baseline until later slices move behavior behind verified
contracts. S02 did not rename modules, move packages, copy production logic or
register service builds. S05 registers the first target service build,
`services:repository-source-service`, without removing the predecessor
`services:repository-analysis-service`. S06 registers
`services:ingestion-service`, without removing predecessor
`services:forensic-ingestion-service` or legacy ingestion modules.

S02 found no direct `project(...)` dependencies in
`services/*/build.gradle.kts` and no `project(":services:...")` dependencies in
tracked Gradle build files. Transitional service builds that generate code from
`contracts/grpc` are consuming external interface contracts locally; this is
not a shared Java implementation module and still requires S03 contract review.

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

The first target-name service implementation evidence is:

- `services/repository-source-service`.
- `services/ingestion-service`.

`services/repository-source-service` owns repository source preparation only.
It does not implement JavaParser analysis handoff, Joern execution, report
generation, BTM generation or direct consumer access to private workspaces.

`services/ingestion-service` owns raw intake/session state, request validation,
engine request manifest import and accepted raw payload handoff ports only. It
does not implement repository checkout, canonical fact writes, JavaParser
analysis, Joern execution, reporting or orchestration state.

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
