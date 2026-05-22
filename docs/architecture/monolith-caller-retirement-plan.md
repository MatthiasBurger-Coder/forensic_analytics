# Monolith Caller Retirement Plan

## Status

FA-MSA-001 Slice 02 caller inventory for workflow
`fa-msa-001-microservice-decomposition-20260521-v1`.

No `forensic-analytics-*` module, package, class or runtime path is removed in
this slice. The current modular-monolith paths remain registered until a later
slice proves caller removal, replacement parity, rollback or explicit
deprecation evidence.

## Verification Commands

Caller evidence was gathered with:

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --short --branch
git ls-files "forensic-analytics-*/build.gradle.kts" "services/*/build.gradle.kts" settings.gradle.kts | sort
git ls-files "*build.gradle.kts" | xargs rg -n "project\\(\\\":forensic-analytics-"
git ls-files "services/*/build.gradle.kts" | xargs -r rg -n "project\\(" || true
rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|ingestion\\.request|ingestion\\.grpc)\\b" services -S -g "*.java" || true
rg -n "RunRepositoryAnalysisUseCase|RunRepositoryAnalysisCommand|DefaultRepositoryAnalysisIngestionUseCase|RepositoryAnalysisIngestionUseCase" forensic-analytics-cli forensic-analytics-rest forensic-analytics-bootstrap forensic-analytics-boot-app forensic-analytics-engine forensic-analytics-ingestion-request forensic-analytics-testbed -S -g "*.java"
```

The searches found active production or test callers in the legacy modules.
No path in this inventory is caller-free. The service Gradle scan found no
direct `project(...)` dependencies inside `services/*/build.gradle.kts`.

## Caller Inventory

| Legacy path | Verified caller evidence | Current status | Target owner | Required contract | Parity test before removal | Rollback or deprecation strategy | Forbidden changes |
|---|---|---|---|---|---|---|---|
| CLI local `analyze` path in `forensic-analytics-cli` | `ForensicAnalyticsCli` loads `RunRepositoryAnalysisUseCase` through `ServiceLoader`; `AnalyzeCommand` maps to `RunRepositoryAnalysisCommand`; CLI build depends on `forensic-analytics-application` and `forensic-analytics-domain` | Active caller and migration candidate | `cli-client` as public API consumer; submission/status/report behavior owned by `analysis-orchestrator-service` and `query-report-api-service` after S03/S04 | Existing `contracts/cli/gateway-cli-contract.md` and `contracts/openapi/gateway-api.yaml` are predecessor evidence; S03 must approve FA-MSA-001 public API contracts | CLI public-API contract tests plus existing local `analyze` regression tests | Keep local `analyze` available while any remote public API command or mode is opt-in | Do not silently route local paths to public APIs; do not depend on service Java implementation classes or generated DTO modules; do not expose workspace or checkout paths |
| REST repository analysis adapter in `forensic-analytics-rest` | `RepositoryAnalysisHttpHandler` and `RestApiServerFactory` use `RepositoryAnalysisIngestionUseCase`; REST build depends on application and domain modules | Active caller and migration candidate | `query-report-api-service` for public query/report API; `analysis-orchestrator-service` for job submission/status only after S03/S04 approve the boundary | Existing `contracts/openapi/gateway-api.yaml` is predecessor evidence; S03 must approve FA-MSA-001 query/report and orchestration contracts | OpenAPI contract tests and REST endpoint parity tests | Keep in-process REST adapter behind Boot/Bootstrap until public API parity and client migration are verified | Do not turn REST adapter into worker orchestration; do not claim runtime parity from OpenAPI alone |
| Bootstrap combined runtime in `forensic-analytics-bootstrap` | `ForensicAnalyticsBackendComponents` constructs `DefaultRepositoryAnalysisIngestionUseCase` and wires gRPC, REST, persistence and repository-source adapters | Active caller and blocked for removal | Service-owned bootstraps for `repository-source-service`, `ingestion-service`, `java-parser-analysis-service`, `joern-analysis-service`, `analysis-orchestrator-service` and `query-report-api-service` | S03-approved service-specific REST/gRPC/event/file contracts | Service-local start tests, health checks and integration evidence for each migrated service | Keep Bootstrap as rollback path until service runtime and deployment evidence are complete | Do not remove while Boot or CLI tests still rely on in-process backend components |
| Spring Boot monolith runtime in `forensic-analytics-boot-app` | Boot configuration constructs `DefaultRepositoryAnalysisIngestionUseCase`; REST and gRPC configurations receive `RepositoryAnalysisIngestionUseCase` | Active caller and blocked for removal | Service-local Spring Boot applications for the mandatory FA-MSA-001 services where Spring is retained | Service-specific REST/gRPC contracts and health endpoints | Service boot tests, health tests, container build tests and local Compose parity | Keep Boot app until all accepted runtime paths are covered by independently startable services | Do not describe the Boot app as a microservice landscape; do not add Spring dependencies to application or domain |
| Repository analysis engine wrapper in `forensic-analytics-engine` | `RepositoryAnalysisEngine` delegates to `RunRepositoryAnalysisUseCase`; engine tests still instantiate that path | Active caller and blocked pending replacement decision | `analysis-orchestrator-service` API or explicit deprecation decision | Analysis orchestration contract chosen by S03/S04 | Engine replacement or deprecation tests proving equivalent command/result behavior | Keep the wrapper until the target owner and replacement API are explicit | Do not infer an owner API from naming symmetry; do not remove tests before replacement evidence exists |
| Engine request import helper in `forensic-analytics-ingestion-request` | Module build depends on application, domain and observability; CLI currently depends on this module for `ingest-request` behavior | Active dependency and blocked pending CLI/public API decision | `ingestion-service` for intake, validation and normalization; `analysis-orchestrator-service` for job coordination; `cli-client` consumes only public APIs | Existing engine-request shape plus S03-approved ingestion/orchestration submission contract if routed remotely | CLI `ingest-request` parity tests and request redaction tests | Keep the helper as current local import path until the public submission path is approved | Do not collapse request-import behavior into repository analysis without a verified contract |
| Testbed monolith E2E and regression paths in `forensic-analytics-testbed` | Testbed depends on CLI, engine, REST, Bootstrap, Boot app, adapters, persistence, application and domain; tests instantiate default use cases directly | Active test caller and blocked for removal | `testbed` as non-production integration environment plus service-local integration tests for mandatory target services | S03-approved service contracts and Compose/test environment contracts where needed | Networked or in-process service E2E tests that cover the same evidence without shared Java implementation modules | Keep testbed as regression evidence until service tests are stronger than the monolith coverage | Do not share testbed fixtures as service implementation modules |
| Shared `forensic-analytics-application` and `forensic-analytics-domain` dependencies | CLI, REST, Bootstrap, Boot app, Engine, Ingestion Request and Testbed build files still depend on these modules directly or as tests | Active caller and blocked for service extraction | Split into service-owned application and domain models under each mandatory target service boundary | REST/OpenAPI, gRPC/protobuf, messaging or approved file contracts | Per-service architecture tests proving no shared monolith application/domain dependency | Keep shared modules as current monolith baseline until all consumers are migrated or explicitly deprecated | Do not create shared Java DTO, domain, utility, fixture or internal error-model modules for services |

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

The current local-path `analyze` behavior remains an active legacy command. It
is not caller-free and must remain available unless a later slice explicitly
deprecates it with tests and migration notes. Therefore the CLI module is not
fully retired from monolith dependencies in S02; no path is removed in this
inventory slice.

## S02 Conditional Retirement Result

S02 completes as `NO_REMOVAL_SAFE` for code, build and module retirement.

No `settings.gradle.kts` entry, module, package, class or runtime path is
removed because caller evidence still finds active production or test references
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
that is not weakened by deleting the current regression evidence.

## S14 Three Amigos Repair Decision

S14 was repaired from a direct deletion slice into a retirement-readiness gate.
The direct deletion requirement is not executable on the current repository
state because the required caller-free evidence is false.

Decision: `NO_REMOVAL_SAFE` for module and source-tree retirement in the
current workflow state.

Three Amigos findings:

- Requirement: S14 may decide retirement readiness, but it must not delete
  active legacy modules. The active target is to record blockers and follow-up
  slices when callers remain.
- Architecture: the repository is still in a strangler phase. Central
  `forensic-analytics-*` modules remain legacy in-process and rollback paths,
  not completed microservices and not caller-free removal candidates.
- Backend: CLI, REST, Bootstrap, Boot, Engine, Ingestion Request, Persistence,
  Application and Domain paths still provide verified behavior or ports used by
  current tests and runtime wiring.
- Test: `forensic-analytics-testbed` and `services:testbed` intentionally keep
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

rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|persistence|logging|observability|rest|bootstrap|boot|engine)\\b" services/*/src/main forensic-analytics-*/src/main -g "*.java" | wc -l
# 633

rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|persistence|logging|observability|rest|bootstrap|boot|engine)\\b" services/*/src/test forensic-analytics-*/src/test -g "*.java" | wc -l
# 594
```

`services:testbed` currently has nine test dependencies on retained legacy
modules. This is intentional parity evidence from S13, not permission to use
the testbed as a production dependency.

S14 therefore has this executable rule:

1. If scans find active callers, complete S14 as `NO_REMOVAL_SAFE`.
2. Do not remove `settings.gradle.kts` entries, source trees, runtime paths or
   tests in S14.
3. Create follow-up retirement slices for each active legacy path.
4. Retire only a path that later proves caller-free evidence, replacement
   parity, rollback or explicit deprecation and the required `QUALITY.md` gate.

## Follow-Up Retirement Slices

These slices are provisional follow-up work, not executable S14 work:

| Follow-up | Purpose | Required proof before deletion |
|---|---|---|
| `S14A` | Migrate or explicitly deprecate local CLI `analyze` and `ingest-request` behavior. | Public API CLI parity or deprecation tests, operator migration notes and root quality gate. |
| `S14B` | Replace in-process REST, Bootstrap and Boot runtime callers with service-owned contracts and startup evidence. | Service start/health tests, REST/gRPC contract parity, rollback notes and full local quality gate. |
| `S14C` | Resolve Engine and Ingestion Request ownership through target service APIs or explicit deprecation. | Owner API contract, compatibility/deprecation tests and no remaining production caller imports. |
| `S14D` | Replace monolith-coupled Testbed coverage with networked or service-local E2E coverage. | E2E tests that cover the same evidence without shared Java implementation modules. |
| `S14E` | Remove only verified caller-free modules or paths. | Empty caller scans for the named path, replacement parity, rollback/deprecation notes and full local quality gate. |
