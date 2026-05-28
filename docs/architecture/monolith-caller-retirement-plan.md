# Monolith Caller Retirement Plan

## Status

FA-MSA-001 Slice 02 caller inventory for workflow
`fa-msa-001-microservice-decomposition-20260521-v1`.

No `forensic-analytics-*` module, package, class or runtime path was removed in
this historical slice. The active final-retirement workflow supersedes this
status: the verified Gradle project model is service-only, legacy source trees
are historical pre-retirement evidence, and S05/S06/S07 own deletion, closure
and release readiness.

## Verification Commands

Caller evidence was gathered with:

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --short --branch
git ls-files "forensic-analytics-*/build.gradle.kts" "*-service/build.gradle.kts" "cli-client/build.gradle.kts" "observability-stack/build.gradle.kts" "testbed/build.gradle.kts" settings.gradle.kts | sort
git ls-files "*build.gradle.kts" | xargs rg -n "project\\(\\\":forensic-analytics-"
git ls-files "*-service/build.gradle.kts" "cli-client/build.gradle.kts" "observability-stack/build.gradle.kts" "testbed/build.gradle.kts" | xargs -r rg -n "project\\(" || true
rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|ingestion\\.request|ingestion\\.grpc)\\b" *-service/src/main cli-client/src/main observability-stack/src/main testbed/src/main -S -g "*.java" || true
rg -n "RunRepositoryAnalysisUseCase|RunRepositoryAnalysisCommand|DefaultRepositoryAnalysisIngestionUseCase|RepositoryAnalysisIngestionUseCase" forensic-analytics-cli forensic-analytics-rest forensic-analytics-bootstrap forensic-analytics-boot-app forensic-analytics-engine forensic-analytics-ingestion-request forensic-analytics-testbed -S -g "*.java"
```

The searches found active production or test callers in the legacy modules.
No path in this inventory is caller-free. The service Gradle scan found no
direct `project(...)` dependencies inside top-level service build files.

## Caller Inventory

| Legacy path | Verified caller evidence | Historical S02/S14 status | Target owner | Required contract | Parity test before removal | Rollback or deprecation strategy | Forbidden changes |
|---|---|---|---|---|---|---|---|
| CLI local `analyze` path in `forensic-analytics-cli` | `ForensicAnalyticsCli` loads `RunRepositoryAnalysisUseCase` through `ServiceLoader`; `AnalyzeCommand` maps to `RunRepositoryAnalysisCommand`; CLI build depended on `forensic-analytics-application` and `forensic-analytics-domain` | Historical caller and migration candidate | `cli-client` as public API consumer; submission/status/report behavior owned by `analysis-orchestrator-service` and `query-report-api-service` after service-boundary approval | Existing `contracts/cli/gateway-cli-contract.md` and `contracts/openapi/gateway-api.yaml` are predecessor evidence | CLI public-API contract tests plus existing local `analyze` regression tests | Preserve historical local `analyze` evidence until explicit deprecation or replacement is documented | Do not silently route local paths to public APIs; do not depend on service Java implementation classes or generated DTO modules; do not expose workspace or checkout paths |
| REST repository analysis adapter in `forensic-analytics-rest` | `RepositoryAnalysisHttpHandler` and `RestApiServerFactory` use `RepositoryAnalysisIngestionUseCase`; REST build depended on application and domain modules | Historical caller and migration candidate | `query-report-api-service` for public query/report API; `analysis-orchestrator-service` for job submission/status | Existing `contracts/openapi/gateway-api.yaml` is predecessor evidence | OpenAPI contract tests and REST endpoint parity tests | Preserve in-process REST evidence until public API parity and client migration are verified | Do not turn REST adapter into worker orchestration; do not claim runtime parity from OpenAPI alone |
| Bootstrap combined runtime in `forensic-analytics-bootstrap` | `ForensicAnalyticsBackendComponents` constructs `DefaultRepositoryAnalysisIngestionUseCase` and wires gRPC, REST, persistence and repository-source adapters | Historical caller; blocked in S02/S14 | Service-owned bootstraps for `repository-source-service`, `ingestion-service`, `java-parser-analysis-service`, `joern-analysis-service`, `analysis-orchestrator-service` and `query-report-api-service` | Service-specific REST/gRPC/event/file contracts | Service-local start tests, health checks and integration evidence for each migrated service | Preserve Bootstrap as historical rollback evidence until final retirement closure | Do not remove while Boot or CLI tests still rely on in-process backend components |
| Spring Boot monolith runtime in `forensic-analytics-boot-app` | Boot configuration constructs `DefaultRepositoryAnalysisIngestionUseCase`; REST and gRPC configurations receive `RepositoryAnalysisIngestionUseCase` | Historical caller; blocked in S02/S14 | Service-local Spring Boot applications for the mandatory FA-MSA-001 services where Spring is retained | Service-specific REST/gRPC contracts and health endpoints | Service boot tests, health tests, container build tests and local Compose parity | Preserve Boot app as historical rollback evidence until independently startable services cover accepted runtime paths | Do not describe the Boot app as a microservice landscape; do not add Spring dependencies to application or domain |
| Repository analysis engine wrapper in `forensic-analytics-engine` | `RepositoryAnalysisEngine` delegates to `RunRepositoryAnalysisUseCase`; engine tests instantiated that path | Historical caller; replacement decision required | `analysis-orchestrator-service` API or explicit deprecation decision | Analysis orchestration contract | Engine replacement or deprecation tests proving equivalent command/result behavior | Preserve wrapper evidence until the target owner and replacement API are explicit | Do not infer an owner API from naming symmetry; do not remove tests before replacement evidence exists |
| Engine request import helper in `forensic-analytics-ingestion-request` | Module build depended on application, domain and observability; CLI depended on this module for `ingest-request` behavior | Historical dependency; CLI/public API decision required | `ingestion-service` for intake, validation and normalization; `analysis-orchestrator-service` for job coordination; `cli-client` consumes only public APIs | Existing engine-request shape plus approved ingestion/orchestration submission contract if routed remotely | CLI `ingest-request` parity tests and request redaction tests | Preserve helper evidence until the public submission path is approved | Do not collapse request-import behavior into repository analysis without a verified contract |
| Testbed monolith E2E and regression paths in `forensic-analytics-testbed` | Testbed depended on CLI, engine, REST, Bootstrap, Boot app, adapters, persistence, application and domain; tests instantiated default use cases directly | Historical test caller; blocked in S02/S14 | `testbed` as non-production integration environment plus service-local integration tests for mandatory target services | Approved service contracts and Compose/test environment contracts where needed | Networked or in-process service E2E tests that cover the same evidence without shared Java implementation modules | Preserve testbed history as regression evidence until service tests are stronger than monolith coverage | Do not share testbed fixtures as service implementation modules |
| Shared `forensic-analytics-application` and `forensic-analytics-domain` dependencies | CLI, REST, Bootstrap, Boot app, Engine, Ingestion Request and Testbed build files depended on these modules directly or as tests | Historical caller; service extraction blocker in S02/S14 | Split into service-owned application and domain models under each mandatory target service boundary | REST/OpenAPI, gRPC/protobuf, messaging or approved file contracts | Per-service architecture tests proving no shared monolith application/domain dependency | Preserve shared-module evidence as historical monolith baseline until consumers are migrated or explicitly deprecated | Do not create shared Java DTO, domain, utility, fixture or internal error-model modules for services |

## Retirement Gates

Any later slice that retires a legacy path must prove all of the following
before deleting code, removing a Gradle dependency or changing default runtime
routing:

- caller-free evidence from `rg` over production code, tests, build files,
  docs and service roots;
- target service owner and data owner;
- contract reference and compatibility rule;
- replacement parity test or explicit deprecation test;
- rollback path or operator-visible migration note;
- required quality gate from `QUALITY.md`;
- no shared Java implementation, DTO, domain, utility, fixture or internal
  error-model module between independently deployable services.

## FA-MSA-001 CLI Candidate

The CLI boundary is a later migrated caller path only for explicit public API
submission. The existing `gateway-submit` command is predecessor evidence for
remote submission, but FA-MSA-001 uses `cli-client` as the target client name
and requires S03-approved public API contracts before expanding or replacing
local behavior.

The local-path `analyze` behavior was an operative predecessor command in this
historical inventory. It was not caller-free at the time and required explicit
deprecation with tests and migration notes before retirement. The active
final-retirement workflow now treats the CLI source tree as retired historical
predecessor evidence after S05, with S06/S07 closing documentation and release
readiness.

## S02 Conditional Retirement Result

S02 completes as `NO_REMOVAL_SAFE` for code, build and module retirement.

No `settings.gradle.kts` entry, module, package, class or runtime path is
removed because caller evidence still found production or test references
for:

- `forensic-analytics-cli` local `analyze` and `ingest-request` behavior;
- `forensic-analytics-rest` repository analysis adapter;
- `forensic-analytics-bootstrap` combined runtime wiring;
- `forensic-analytics-boot-app` Spring Boot runtime wiring;
- `forensic-analytics-engine` repository analysis wrapper;
- `forensic-analytics-ingestion-request` engine request import helper;
- `forensic-analytics-testbed` monolith parity and E2E coverage.

Removal remains blocked until a later slice proves caller-free evidence,
replacement parity, rollback or explicit deprecation strategy and a quality gate
that is not weakened by deleting regression evidence.

## S14 Three Amigos Repair Decision

S14 was repaired from a direct deletion slice into a retirement-readiness gate.
That historical direct deletion requirement was not executable because the
required caller-free evidence was false.

Decision: `NO_REMOVAL_SAFE` for module and source-tree retirement in the
historical S14 workflow state. The active final-retirement workflow supersedes
that state with S04 documentation cleanup, S05 deletion, S06 architecture
closure and S07 release readiness.

Three Amigos findings:

- Requirement: S14 may decide retirement readiness, but it must not delete
  legacy modules with callers. That historical target was to record blockers
  and follow-up slices when callers remained.
- Architecture: the repository was still in a strangler phase. Central
  `forensic-analytics-*` modules were legacy in-process and rollback paths, not
  completed microservices and not caller-free removal candidates.
- Backend: CLI, REST, Bootstrap, Boot, Engine, Ingestion Request, Persistence,
  Application and Domain paths provided verified behavior or ports used by
  current tests and runtime wiring.
- Test: `forensic-analytics-testbed` and `testbed` intentionally keep
  regression coverage over legacy in-process behavior. Removing old modules
  before stronger service or networked E2E evidence exists would delete the
  only verified coverage for several behaviors.
- DevOps: no deletion may be claimed as safe until the Gradle build, start
  paths and rollback/deprecation notes are updated and the full local quality
  gate passes.

S14 repair verification found active coupling evidence:

```bash
git ls-files "*build.gradle.kts" | xargs rg -n "forensic-analytics-(domain|application|persistence|logging|bootstrap|boot-app|engine|rest|observability)" | wc -l
# 58

rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|persistence|logging|observability|rest|bootstrap|boot|engine)\\b" *-service/src/main cli-client/src/main observability-stack/src/main testbed/src/main forensic-analytics-*/src/main -g "*.java" | wc -l
# historical non-zero count; superseded by final-retirement verification

rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|persistence|logging|observability|rest|bootstrap|boot|engine)\\b" *-service/src/test cli-client/src/test observability-stack/src/test testbed/src/test forensic-analytics-*/src/test -g "*.java" | wc -l
# historical non-zero count; superseded by final-retirement verification
```

`testbed` previously had test dependencies on retained legacy modules.
That historical parity evidence is superseded by the current service-root
testbed build, which has no legacy project dependency.

S14 therefore has this executable rule:

1. If scans find active callers, complete S14 as `NO_REMOVAL_SAFE`.
2. Do not remove `settings.gradle.kts` entries, source trees, runtime paths or
   tests in S14.
3. Create follow-up retirement slices for each legacy path with callers.
4. Retire only a path that later proves caller-free evidence, replacement
   parity, rollback or explicit deprecation and the required `QUALITY.md` gate.

## Refined Follow-Up Retirement Slices

Workflow-create refinement after the S14 execution stop is superseded by the
active final-retirement workflow. S04 cleared pre-deletion documentation
blockers, S05 completed source-tree deletion, S06 is architecture/ADR closure
and S07 is release readiness.

| Active slice | Purpose | Required proof before deletion or closure |
|---|---|---|
| `S04` | Clear stale executable/current legacy documentation blockers. | No active service, deployment, architecture or audit docs claim legacy modules as current executable/build/runtime evidence. |
| `S05` | Remove verified legacy source-tree candidates. | Empty active build/source leakage scans, replacement parity, rollback/deprecation notes and required quality gate. |
| `S06` | Close architecture, ADR and arc42 evidence. | Final arc42/ADR/docs sync without overclaiming runtime, Docker, Swarm or Kubernetes readiness. |
| `S07` | Close release readiness. | Full local quality gate and final workflow evidence. |
