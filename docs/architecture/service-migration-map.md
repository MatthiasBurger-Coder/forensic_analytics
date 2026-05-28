# Service Migration Map

## Status

FA-MSA-001 service-boundary, migration inventory and data-ownership map with
current FA-MSA-001-LMR S03 repository-source, S04 ingestion, S05 JavaParser,
S06 Joern, S07 orchestration and earlier public API contract ownership evidence
plus retirement inventory revalidation.

This document maps current modular-monolith and transitional service evidence
to FA-MSA-001 target ownership decisions. It does not move production code,
create build projects, create service implementations, define storage
technology or claim runtime readiness. Earlier slice labels in this document
are prior migration evidence; the active final source-tree retirement sequence
uses S04 documentation-stopper cleanup, S05 deletion, S06 architecture closure
and S07 release readiness. S04 records one-writer target data ownership before
persistence migration starts.

Earlier FA-MSA-001-LMR inventories recorded direct legacy Gradle references and
legacy imports before service extraction progressed. That count set is
superseded by final-retirement verification: the active Gradle model is
service-only, active non-legacy build files have no `project(":forensic-analytics-*")`
dependencies, active non-legacy Java sources have no legacy monolith imports,
and S05 checkpoint `d8d9dab` removed all tracked legacy source-tree files.
`forensic-analytics-persistence` is historical legacy or rollback evidence:
productive target services do not import or build-depend on it, but
workspace/project administration, membership, asset, audit, retention and
legacy project-storage behavior still have no mandatory FA-MSA-001 target
owner. The map treats all listed `forensic-analytics-*` names as historical
predecessor evidence after source-tree deletion, not as active implementation
roots.

Earlier public API contract work moved the executable OpenAPI contract test for
the public repository-to-BTM submission/status shape into
`query-report-api-service` and added target CLI HTTP client coverage. It does
not remove
`forensic-analytics-rest`, `forensic-analytics-bootstrap`,
`forensic-analytics-boot-app` or `forensic-analytics-persistence`, and it does
not claim durable persistence, container runtime or live health readiness.

## Target Mapping

| FA-MSA-001 target | Current and predecessor evidence | Current coupling | Planned migration path | Required contract first | Data owner status | Forbidden moves | Verification needed |
|---|---|---|---|---|---|---|---|
| `repository-source-service` | `repository-source-service`; predecessors `forensic-analytics-adapter-repository-source`, `repository-analysis-service` | Earlier FA-MSA-001 service-slice evidence registers the target service locally; the legacy adapter source tree is retired historical evidence after S05 source-tree removal | Move repository access, branch resolution, checkout/fetch, workspace preparation, source package byte custody and source snapshot descriptors into a service-local boundary; later slices must route callers only through approved contracts | Repository source/snapshot REST, gRPC or file contract; the predecessor `repository-analysis.proto` wire shape is transitional external contract evidence only | Owns workspaces, leases, cleanup, source package bytes, source snapshot descriptors and accepted source metadata | No private workspace sharing; no target repository code execution without sandbox approval; no JavaParser or Joern handoff logic inside repository source | Service-local architecture, Git safety, checkout diagnostics, build/start and Dockerfile checks |
| `ingestion-service` | `ingestion-service`; predecessors `forensic-analytics-ingestion-grpc`, `forensic-analytics-ingestion-request`, `forensic-ingestion-service` | Earlier FA-MSA-001 service-slice evidence registers the target service locally; legacy gRPC/request source trees are retired historical evidence after S05 source-tree removal | Move intake, validation, normalization and request import behavior into service-local domain/application/adapters; later slices must route callers only through approved contracts | Ingestion gRPC/API contract; `forensic-ingestion.proto` wire shape is transitional contract evidence | Owns raw intake, upload sessions, raw runtime or analysis payload byte custody before handoff and rejected-ingestion diagnostics | No shared generated Java DTO module; no static/semantic canonical fact writes; no repository checkout responsibility | gRPC/API contract tests, validation tests, request-manifest tests and missing-field diagnostics |
| `java-parser-analysis-service` | `java-parser-analysis-service`; predecessors `forensic-analytics-adapter-javaparser`, `java-ast-analysis-service` | Earlier FA-MSA-001 service-slice evidence registers the target service locally; the legacy adapter source tree is retired historical evidence after S05 source-tree removal | Move JavaParser AST scanning and static source-fact extraction into service-local boundary; later slices must route callers only through approved contracts | JavaParser analysis and source-fact artifact contracts; `java-ast-analysis.proto` wire shape and explicit `sourceRoot` artifact context are transitional contract evidence | Owns canonical static Java source facts it produces, immutable source-fact artifact bytes and producer-local artifact metadata | No runtime execution claims from static facts; no JavaParser API leakage into neutral contracts; unresolved-symbol diagnostics must stay explicit | Source-location, unresolved-symbol, deterministic ID, immutable artifact retrieval and JSON schema tests |
| `joern-analysis-service` | `joern-analysis-service`; predecessors `forensic-analytics-adapter-joern-docker`, `joern-cpg-analysis-service`, `docker/joern/**` | Earlier FA-MSA-001 service-slice evidence registers the target service locally; the legacy adapter source tree is retired historical evidence after S05 source-tree removal | Move Joern Docker control, CPG/CFG/DFG artifact production, semantic artifact byte retrieval and semantic mapping diagnostics into service-local boundary; later slices must route callers only through approved contracts | Joern analysis artifact contract; service-owned `GetSemanticArtifactBytes` retrieval is transitional contract evidence | Owns canonical Joern semantic facts it produces, semantic artifact bytes and producer-local artifact metadata | No shared CPG filesystem coupling; no Repository Source private workspace mounts; no CPG/CFG/DFG facts as runtime trace evidence | Joern unavailable, timeout, incomplete mapping, byte retrieval, Docker-boundary, service-local architecture and build/start tests |
| `analysis-orchestrator-service` | `analysis-orchestrator-service`; retired predecessors `forensic-analytics-engine` and orchestration portions of `forensic-analytics-application`; registered predecessor `analysis-store-service` coordination/status evidence | Earlier FA-MSA-001 service-slice evidence registers the target service locally; `StartRepositoryToBtm` and `GetRepositoryToBtmStatus` are pending/status-only orchestration; the `forensic-analytics-engine` and `forensic-analytics-application` source-tree histories are retired evidence after S05 source-tree removal; `analysis-store-service` remains registered predecessor service-root evidence | Keep orchestration-only service behavior, then route callers through approved contracts only after parity evidence | Analysis orchestration API or event contracts; `analysis-job.proto` wire shape is transitional external contract evidence only | Owns job lifecycle, workflow status, worker leases/attempts, retry/timeout/failure/dead-letter state, correlation references, job-to-artifact references and process-local repository-to-BTM readiness state | No repository checkout, parser, Joern, BTM generation, report, artifact byte custody, producer catalog, canonical fact storage, durable persistence, event outbox or distributed orchestration claim inside orchestrator | Job lifecycle, retry, timeout, status, failure, dead-letter, artifact-reference, repository-to-BTM pending-status, idempotency and no-hidden-monolith tests |
| `query-report-api-service` | `query-report-api-service`; predecessors `forensic-analytics-rest`, public facade parts of `forensic-gateway-service`, report/query concepts | Earlier FA-MSA-001 service-slice evidence registers the target service locally and adds service-local executable OpenAPI contract-test ownership; repository-analysis submission/status routes call `analysis-orchestrator-service` pending readiness; FA-MVP-0001 workspace routes call `repository-source-service` owner APIs while the legacy REST source tree is retired historical evidence after S05 source-tree removal | Keep public API facade behavior, then add report/status owner reads through approved contracts only after caller migration and parity evidence | REST/OpenAPI query/report/workspace contract; `gateway-api.yaml`, `analysis-job.proto` and `repository-analysis.proto` are transitional external contract evidence only | Owns public read models, public cache state, generated report packages, LLM-ready packages and stored generated LLM output only as labeled generated analysis or hypotheses; does not own repository-source workspace state | No analysis execution, checkout, Joern, JavaParser, direct DB access, canonical evidence ownership, worker dispatch, BTM byte custody, repository-source H2 access or private workspace path access | Service-local OpenAPI contract, redaction, error mapping, orchestrator pending-status mapping, repository-source owner API mapping and frontend/CLI compatibility tests |
| `cli-client` | `cli-client`; predecessor `forensic-analytics-cli` | Legacy CLI source tree is retired historical evidence after S05 | Move CLI to public API client behavior and keep local predecessor behavior only as documented compatibility/deprecation evidence | CLI/public API contract | CLI owns no forensic data | No parser, Joern, persistence, service implementation or domain logic in CLI | CLI contract tests, HTTP payload/header coverage, output redaction and compatibility/deprecation tests |
| `observability-stack` | `observability-stack`; predecessors `forensic-analytics-observability`, `forensic-analytics-logging`, deployment docs | Central observability/logging Java source trees are retired historical evidence after S05 | Replace shared Java logging/observability modules with service-local configuration and deployment observability material | Operational configuration contracts where needed | No forensic evidence ownership | No shared Java logging library; no diagnostics as evidence | Dependency scans, logging redaction and deployment-doc verification |
| `testbed` | `testbed`; predecessor `forensic-analytics-testbed`, Compose docs and service-local tests | Legacy testbed source tree is retired historical evidence after S05 | Move system/integration test orchestration to non-production testbed after replacement service E2E exists | Test environment contracts or Compose files when needed | Test data only | No production service dependency on testbed source or fixtures | Service E2E tests, no production dependency checks and Compose validation when used |

## Central Module Retirement Map

Every row in this table is historical source-tree retirement evidence after
ADR-0022. S05 proved caller-free active build/source leakage for the deleted
source trees and removed them. The target decisions and ownership gaps still
matter for future service behavior, but the listed source trees are not active
Gradle projects.

| Current module | Target decision | Retirement gate |
|---|---|---|
| `forensic-analytics-domain` | Split into service-local domain models. | Caller-free evidence across production code, tests, build files and docs; service-local domain parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-application` | Split into service-local application/use-case code. | Caller-free evidence; verified service owners and contracts; replacement parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-persistence` | Replace mandatory repository-to-BTM persistence concerns with service-local state owned by the S04/S11 ownership matrix; workspace/project administration, membership, asset, audit, retention and legacy project-storage behavior remain ownership gaps until a later owner requirement or explicit deprecation exists. | S05 retired the source tree; future behavior changes still require service-local persistence replacement or explicit deprecation, owner decisions, tests and rollback/operator notes. |
| `forensic-analytics-logging` | Replace with service-local logging configuration or `observability-stack` deployment material. | Caller-free evidence; service-local diagnostics or deployment replacement; explicit redaction behavior; rollback/operator note; required quality gate. |
| `forensic-analytics-observability` | Replace with service-local diagnostics/correlation configuration or deployment observability material. | Caller-free evidence; service-local observability replacement; replacement parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-bootstrap` | Retire after service-local bootstraps and runtime start paths are verified. | Caller-free evidence; service-local start/health/container parity; rollback/operator note; required quality gate. |
| `forensic-analytics-boot-app` | Retire after mandatory service runtime paths and rollback evidence exist. | Caller-free evidence; mandatory service runtime parity; rollback/operator note; required quality gate. |
| `forensic-analytics-engine` | Retire or split into `analysis-orchestrator-service` after orchestration ownership is explicit. | Caller-free evidence; orchestration API parity or explicit deprecation; rollback/operator note; required quality gate. |
| `forensic-analytics-rest` | Retire after `query-report-api-service` has public API parity and caller migration. | Caller-free evidence; public API parity or explicit deprecation; rollback/operator note; required quality gate. |

S12 confirms that productive `*-service/src/main, cli-client/src/main, observability-stack/src/main and testbed/src/main` code no longer imports
central `de.burger.forensics.analytics.domain` or
`de.burger.forensics.analytics.application` packages and that productive
service build files do not depend on `forensic-analytics-domain`,
`forensic-analytics-application` or another top-level service implementation
project. The former central domain and application modules are retired source
trees after S05; their behavior remains historical predecessor evidence only.
`testbed` keeps service-root non-production regression coverage, not
productive service coupling.

## Current Implementation Evidence

The legacy `forensic-analytics-*` source trees are retired historical migration
baseline evidence, not the active Gradle implementation baseline. The verified
Gradle project model is service-only as top-level service projects. S02 did not
rename modules, move packages, copy production logic or register service
builds. S03 registers the first target service build,
`repository-source-service`, without removing the predecessor
`repository-analysis-service`. S04 registers
`ingestion-service`, without removing predecessor
`forensic-ingestion-service` or legacy ingestion modules. S05
registers `java-parser-analysis-service`, without removing
predecessor `java-ast-analysis-service` or the legacy JavaParser
adapter. S06 registers `joern-analysis-service`, without removing
predecessor `joern-cpg-analysis-service` or the legacy Joern Docker
adapter. S07 implements repository-to-BTM request acceptance and status lookup
inside `analysis-orchestrator-service` as incomplete readiness state,
while `forensic-analytics-engine`, `forensic-analytics-application` and
`forensic-analytics-domain` remained predecessor evidence at that time. ADR-0022
and S05 now retire those source trees; Analysis Store predecessor paths remain
service-root evidence where they are still registered.

S02 found no direct `project(...)` dependencies in
top-level service build files and no `project(":<service>")` dependencies in
tracked Gradle build files. Transitional service builds that generate code from
`contracts/grpc` are consuming external interface contracts locally; this is
not a shared Java implementation module and still requires S03 contract review.

The current service directories are transitional evidence:

- `forensic-gateway-service`;
- `forensic-ingestion-service`;
- `repository-analysis-service`;
- `analysis-store-service`;
- `java-ast-analysis-service`;
- `joern-cpg-analysis-service`;
- `btm-generation-service`;
- `graph-replay-service`;
- `report-generation-service`.

These directories are not FA-MSA-001 compatibility aliases. Later slices may
move, replace, split or retire them only with verified caller evidence,
contracts, tests and rollback notes.

The first target-name service implementation evidence is:

- `repository-source-service`.
- `ingestion-service`.
- `java-parser-analysis-service`.
- `joern-analysis-service`.

`repository-source-service` owns repository source preparation only.
It does not implement JavaParser analysis handoff, Joern execution, report
generation, BTM generation or direct consumer access to private workspaces.

`ingestion-service` owns raw intake/session state, request validation,
engine request manifest import and accepted raw payload handoff ports only. It
does not implement repository checkout, canonical fact writes, JavaParser
analysis, Joern execution, reporting or orchestration state.

`java-parser-analysis-service` owns JavaParser execution, static Java
method facts, source locations, source-root context, parser diagnostics,
unresolved-symbol limitation diagnostics and source-fact artifact bytes only.
It does not implement repository checkout, runtime execution truth, Joern
semantic analysis, report generation or orchestration state.

`joern-analysis-service` owns Joern runtime invocation, Joern-owned
workspace materialization, CPG/CFG/DFG semantic artifact production,
provenance, diagnostics and service-owned semantic artifact byte retrieval
through `GetSemanticArtifactBytes` only. It does not implement repository
checkout, JavaParser primary analysis, runtime trace truth, report generation
or orchestration state.

`analysis-orchestrator-service` owns analysis job lifecycle,
worker leasing, retry/timeout/failure/dead-letter state, job-to-artifact
references and S07 repository-to-BTM pending readiness status only. It accepts
validated `StartRepositoryToBtm` requests and answers
`GetRepositoryToBtmStatus` from process-local in-memory state, while explicitly
reporting incomplete repository handoff, not-ready BTM delivery and skipped
Joern execution. It does not run repository checkout, JavaParser, Joern, BTM
generation or report rendering and does not claim durable persistence,
event-outbox or distributed orchestration parity.

`forensic-analytics-engine`, orchestration portions of
`forensic-analytics-application` and `forensic-analytics-domain` are retired
historical predecessor evidence after ADR-0022/S05. Future orchestration
behavior must stay service-local or be reintroduced only by an explicit
requirement with owner, contract, test and rollback evidence.

After S12, productive service-local domain and application boundaries are
guarded by service ArchUnit rules plus a build-isolation regression that scans
productive service build files for forbidden central domain/application or
cross-service project dependencies. This evidence supported the final
retirement workflow but does not by itself prove full production microservice
readiness. Physical legacy source-tree deletion completed in S05 after S02
runtime cleanup, S03 regression coverage and S04 documentation-stopper gates.

## Migration Sequencing

1. Keep service-local modules independent.
2. Reconcile target service names in ADR and arc42 documentation.
3. Refresh caller and coupling inventory.
4. Define external contracts before service implementations depend on
   communication behavior.
5. Assign data ownership and persistence boundaries.
6. Add or migrate service-local implementations one service at a time.
7. Verify service independence before routing runtime behavior.
8. Do not restore obsolete monolith paths unless a later requirement supplies
   owner, contract, tests and rollback evidence.

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
