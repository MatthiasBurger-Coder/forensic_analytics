# Service Migration Map

## Status

Slice 01 service-boundary and migration planning map.

This document maps current modular-monolith evidence to target service
ownership candidates. It does not move production code, create build projects,
create service implementations or claim runtime readiness.

Workflow v3 inserts a Repository-to-BTM orchestration contract and
artifact-readiness bridge before end-to-end routing. That bridge must assign
the orchestration owner API, keep Gateway facade-only, preserve Java AST
`ArtifactByteAccess`, define public Gateway diagnostic redaction and represent
unavailable Joern/build-artifact inputs as explicit incomplete diagnostics
before any monolith runtime path can be retired on that basis.

Workflow v4 inserts a source-fact byte retrieval and Java AST handoff contract
before end-to-end routing. That bridge must make Java AST source-fact bytes
retrievable through a verified owner API, expose Repository Analysis to Java AST
handoff completion through a reviewed gRPC service contract and define
deterministic local fixtures that do not depend on external Git network access,
Docker, Jenkins, Artifactory or credentials by default.

Workflow v5 Slice 16 explicitly defers `graph-replay-service` and
`report-generation-service` from repository-to-BTM pipeline acceptance. The
accepted BTM path ends at deterministic BTM artifact generation and public
delivery readiness. Graph/replay and report services remain planned projection
and generated-artifact services until later contracts, owner-query APIs,
storage decisions and tests are approved.

Slice 18 isolates the remaining `forensic-analytics-*` runtime paths as
legacy in-process and rollback paths. No current monolith module is retired in
Slice 18 because caller verification still finds active CLI, REST, Bootstrap,
Boot, engine, ingestion-request and testbed dependencies.

## Mapping

| Target Service | Current Source Evidence | Current Coupling | Planned Migration Path | Required Contract First | Data Owner | Forbidden Moves | Verification Needed |
|---|---|---|---|---|---|---|---|
| `forensic-gateway-service` | `forensic-analytics-rest`, `forensic-analytics-cli`, Boot REST lifecycle | Current REST and CLI are in-process adapters | Create Gateway facade from public API contracts, then route UI/CLI through it | Gateway OpenAPI | Gateway owns facade state only | No AST, Joern, BTM, storage, replay or reporting logic | Gateway contract tests and service-local start test |
| `forensic-ingestion-service` | `forensic-analytics-ingestion-grpc`, `forensic_ingestion.proto`, `ForensicIngestionGrpcService` | Current module has an `api` dependency on application | Extract contract, then create service-local gRPC adapter and ingestion domain | Ingestion gRPC proto | Ingestion owns raw intake and upload sessions | No shared generated Java DTO module; no canonical fact ownership | gRPC contract tests and ingestion validation tests |
| `repository-analysis-service` | `forensic-analytics-adapter-repository-source`, checkout/workspace application ports | Checkout is in-process and shares application/domain models | The initial Repository Analysis service implementation creates an independent service-local repository-preparation API, workspace model, Git checkout adapter and source snapshot handoff; later slices may wire callers to it | `contracts/grpc/repository-analysis.proto` plus analysis job handoff contracts | Repository service owns workspaces and source snapshots | No direct workspace path sharing; no local-path checkout policy in the networked service | Service-local gRPC, architecture, workspace, Git security, coverage and container build tests |
| `build-artifact-worker-service` | No verified standalone source yet; build context exists as request metadata only | Build execution is not a service boundary today | Add a service boundary only after Slice 07 defines source snapshot, complete build-output package, sandbox and byte-access contracts | planned build artifact worker contract or owner API | Build worker owns produced build-output package bytes when introduced | No Repository Analysis private workspace access, no shared caches as hidden coupling, no direct Analysis Store writes | Sandbox, manifest, checksum, build-system detection and byte-access tests |
| `java-ast-analysis-service` | `forensic-analytics-adapter-javaparser`, `JavaParserSourceScanner` | Adapter shares application/domain models | Create service-local AST model, source-fact result contract and source-fact byte retrieval owner API | Analysis job/result contract and Java AST source-fact retrieval contract | AST service owns worker output and produced source-fact bytes until accepted or transferred | No runtime execution claims from static facts and no private workspace reads by consumers | Source-location, unresolved-symbol and byte-access retrieval tests |
| `joern-cpg-analysis-service` | `forensic-analytics-adapter-joern-docker`, `docker/joern/**` | Joern adapter is in-process and uses shared ports | Create Joern service with container-contained runtime and semantic result contract | Analysis job/result contract | Joern service owns execution artifacts until accepted | No shared CPG filesystem coupling | Joern unavailable, timeout and artifact mapping tests |
| `btm-generation-service` | `RuleGenerationPort`, `RuleGenerationRequest`, `RuleGenerationResult`, `.btm` tests | No standalone generator module exists | Create deterministic generation service from delivered fact references | Rule generation contract | BTM service owns generated bytes until explicit byte handoff; Analysis Store owns accepted artifact metadata only | No repository scanning or runtime trace invention | Rule ID stability and deterministic output tests |
| `analysis-store-service` | `forensic-analytics-persistence`, application ports, domain session/artifact models | Current monolith persistence is in-memory and in-process | The current Analysis Store service implementation creates an independent service-local job lifecycle and artifact metadata API; later slices add durable normalized facts, incidents and correlations | `contracts/grpc/analysis-job.proto` for implemented job/artifact operations; future store/query contracts and events | Analysis Store owns canonical job lifecycle and planned canonical evidence state | No shared entities, repositories or direct DB access | Service-local gRPC, architecture, coverage and container build tests |
| `graph-replay-service` | arc42 graph/replay concepts, semantic graph domain model | No standalone graph/replay runtime service exists; deferred by Slice 16 | Create projection service reading owner APIs and exposing replay/graph APIs in a future slice | Replay/graph API contract | Graph/replay owns projections only | No projection as source of truth | Missing-evidence and deterministic replay tests |
| `report-generation-service` | arc42 report/LLM concepts | No standalone report runtime service exists; deferred by Slice 16 | Create report service reading Analysis Store and Graph Replay APIs in a future slice | Report API contract | Report service owns report artifacts and LLM packages | No generated text as evidence | Report determinism and evidence-label tests |
| `frontend-web-app` | `forensic-ui`, API adapter under `/api` | Current root is outside planned `frontend/**` | Move or wrap frontend only after Gateway contracts and implementation stabilize in the active workflow | Gateway OpenAPI | Frontend owns UI state only | No direct internal worker calls | Frontend tests against Gateway API adapter |

## Current Modules That Remain In Place

The existing `forensic-analytics-*` Gradle modules remain the current
implementation baseline until later slices move behavior behind verified
contracts. Slice 01 does not rename modules, move packages, copy production
logic or register service builds.

## Remaining Monolith Module Owner Map

| Current Module | Target Owner / Decision |
|---|---|
| `forensic-analytics-domain` | Split into service-owned domain models during Slice 17/18 only after caller parity is verified; no shared domain module in target services |
| `forensic-analytics-application` | Split by service use case owner during service migration; Analysis Store owns canonical jobs/facts, Repository Analysis owns checkout, AST/Joern/BTM services own worker behavior |
| `forensic-analytics-engine` | Retire or isolate after Gateway, Analysis Store and worker-service orchestration parity exists |
| `forensic-analytics-logging` | Replace with service-local logging/diagnostic configuration; no shared runtime logging module between services |
| `forensic-analytics-observability` | Replace with service-local correlation and diagnostics contracts/configuration; no shared observability implementation module between services |
| `forensic-analytics-cli` | Gateway/public API client adapter after Slice 16 if CLI remains in repository scope |
| `forensic-analytics-testbed` | Retain as monolith test evidence until Slice 17/18 decides parity or retirement; do not share as service fixture module |
| `forensic-analytics-ingestion-request` | Map request-import behavior to Gateway or Ingestion contract path after Slice 02 clarifies public submission semantics |
| `forensic-analytics-bootstrap` | Retire after service runtime path and deployment readiness are verified |
| `forensic-analytics-boot-app` | Retire after implemented services cover the accepted runtime path and rollback evidence exists |

Slice 18 records the current isolation decision in
`docs/architecture/monolith-runtime-isolation.md`: all rows above remain
registered until a later slice proves replacement ownership, caller removal,
parity or explicit deprecation tests and rollback instructions.

Slice 19 reviewed the same module set for removal and did not remove any
module from `settings.gradle.kts`. No current module is both replaced and
caller-free.

## Migration Sequencing

1. Keep current modules unchanged.
2. Prepare target roots and documentation.
3. Define Gateway HTTP and public gRPC BTM delivery contracts in Slice 02.
4. Define artifact-byte and instrumentation-target ownership contracts in
   Slice 03.
5. Define repository source package, complete build-output package and Joern
   materialization contracts in Slice 07.
6. Add service-local implementations one service slice at a time.
7. Verify service independence before routing runtime behavior.
8. Remove obsolete monolith paths only after replacement evidence exists.

The current `analysis-store-service` implementation establishes the first
runtime boundary for analysis jobs and artifact metadata. It is not a
migration of the existing monolith persistence module and does not claim a
durable fact database yet.

The current `repository-analysis-service` implementation establishes the
initial repository runtime boundary. It prepares service-owned workspaces from
clean HTTPS remotes, reports opaque source snapshot metadata and does not
migrate or remove the current monolith repository-source adapter yet.

## Stop Conditions

Stop a later migration step when:

- a target owner is unclear;
- a contract would require guessing fields or endpoints;
- a service would depend on another service's Java classes;
- shared common Java modules are proposed;
- direct cross-service database access is proposed;
- runtime readiness is claimed without build, start, test, healthcheck,
  container and deployment evidence.
