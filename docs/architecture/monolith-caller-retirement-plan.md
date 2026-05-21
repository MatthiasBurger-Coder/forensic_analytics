# Monolith Caller Retirement Plan

## Status

Slice 05 caller inventory for workflow `e2e-wildfly-cli-deploy-20260521-v1`.

No `forensic-analytics-*` module, package, class or runtime path is removed in
this slice. The current modular-monolith paths remain registered until a later
slice proves caller removal, replacement parity, rollback or explicit
deprecation evidence.

## Verification Commands

Caller evidence was gathered with:

```bash
rg -n "RunRepositoryAnalysisUseCase|RunRepositoryAnalysisCommand|DefaultRepositoryAnalysisIngestionUseCase|RepositoryAnalysisIngestionUseCase" forensic-analytics-cli forensic-analytics-rest forensic-analytics-bootstrap forensic-analytics-boot-app forensic-analytics-engine forensic-analytics-ingestion-request forensic-analytics-testbed services docs -S
rg -n "forensic-analytics-application|forensic-analytics-domain" forensic-analytics-cli forensic-analytics-rest forensic-analytics-bootstrap forensic-analytics-boot-app forensic-analytics-engine forensic-analytics-ingestion-request forensic-analytics-testbed services docs settings.gradle.kts build.gradle.kts -S
```

The searches found active production or test callers. No path in this inventory
is caller-free.

## Caller Inventory

| Legacy path | Verified caller evidence | Current status | Target owner | Required contract | Parity test before removal | Rollback or deprecation strategy | Forbidden changes |
|---|---|---|---|---|---|---|---|
| CLI local `analyze` path in `forensic-analytics-cli` | `ForensicAnalyticsCli` loads `RunRepositoryAnalysisUseCase` through `ServiceLoader`; `AnalyzeCommand` maps to `RunRepositoryAnalysisCommand`; CLI build depends on `forensic-analytics-application` and `forensic-analytics-domain` | Active caller and migration candidate | `forensic-gateway-service` as public facade | `contracts/cli/gateway-cli-contract.md`; `contracts/openapi/gateway-api.yaml` | CLI Gateway-mode contract tests plus existing local `analyze` regression tests | Keep local `analyze` available while any Gateway command or mode is opt-in | Do not silently route local paths to Gateway; do not depend on Gateway Java implementation classes or generated DTO modules; do not expose workspace or checkout paths |
| REST repository analysis adapter in `forensic-analytics-rest` | `RepositoryAnalysisHttpHandler` and `RestApiServerFactory` use `RepositoryAnalysisIngestionUseCase`; REST build depends on application and domain modules | Active caller and migration candidate | `forensic-gateway-service` facade plus Analysis Store owner API | `contracts/openapi/gateway-api.yaml`; Analysis Store job owner contracts | Gateway OpenAPI contract tests and REST endpoint parity tests | Keep in-process REST adapter behind Boot/Bootstrap until Gateway parity and client migration are verified | Do not turn REST adapter into worker orchestration; do not claim Gateway runtime parity from OpenAPI alone |
| Bootstrap combined runtime in `forensic-analytics-bootstrap` | `ForensicAnalyticsBackendComponents` constructs `DefaultRepositoryAnalysisIngestionUseCase` and wires gRPC, REST, persistence and repository-source adapters | Active caller and blocked for removal | Service-owned bootstraps for Gateway, Ingestion, Repository Analysis, Analysis Store and workers | Gateway, ingestion gRPC, repository-analysis, analysis-job and worker contracts | Service-local start tests, health checks and repository-to-BTM integration evidence | Keep Bootstrap as rollback path until service runtime and deployment evidence are complete | Do not remove while Boot or CLI tests still rely on in-process backend components |
| Spring Boot monolith runtime in `forensic-analytics-boot-app` | Boot configuration constructs `DefaultRepositoryAnalysisIngestionUseCase`; REST and gRPC configurations receive `RepositoryAnalysisIngestionUseCase` | Active caller and blocked for removal | Service-local Spring Boot applications | Service-specific REST/gRPC contracts and health endpoints | Service boot tests, health tests, container build tests and local Compose parity | Keep Boot app until all accepted runtime paths are covered by independently startable services | Do not describe the Boot app as a microservice landscape; do not add Spring dependencies to application or domain |
| Repository analysis engine wrapper in `forensic-analytics-engine` | `RepositoryAnalysisEngine` delegates to `RunRepositoryAnalysisUseCase`; engine tests still instantiate that path | Active caller and blocked pending replacement decision | Analysis Store orchestration owner API or a future service-local client facade | Analysis job owner contract plus Gateway or worker-facing contract chosen by the replacement slice | Engine replacement or deprecation tests proving equivalent command/result behavior | Keep the wrapper until the target owner and replacement API are explicit | Do not infer an owner API from naming symmetry; do not remove tests before replacement evidence exists |
| Engine request import helper in `forensic-analytics-ingestion-request` | Module build depends on application, domain and observability; CLI currently depends on this module for `ingest-request` behavior | Active dependency and blocked pending CLI/Gateway decision | Gateway or Ingestion service, depending on the approved submission path | Existing engine-request shape plus Gateway/Ingestion submission contract if routed remotely | CLI `ingest-request` parity tests and request redaction tests | Keep the helper as current local import path until the public submission path is approved | Do not collapse request-import behavior into repository analysis without a verified contract |
| Testbed monolith E2E and regression paths in `forensic-analytics-testbed` | Testbed depends on CLI, engine, REST, Bootstrap, Boot app, adapters, persistence, application and domain; tests instantiate default use cases directly | Active test caller and blocked for removal | Service-local integration tests and repository-to-BTM E2E suite | Gateway, Analysis Store, Repository Analysis, Java AST, Joern and BTM contracts | Networked or in-process service E2E tests that cover the same evidence without shared Java implementation modules | Keep testbed as regression evidence until service tests are stronger than the monolith coverage | Do not share testbed fixtures as service implementation modules |
| Shared `forensic-analytics-application` and `forensic-analytics-domain` dependencies | CLI, REST, Bootstrap, Boot app, Engine, Ingestion Request and Testbed build files still depend on these modules directly or as tests | Active caller and blocked for service extraction | Split service-owned application and domain models per service boundary | REST/OpenAPI, gRPC/protobuf or approved event contracts | Per-service architecture tests proving no shared monolith application/domain dependency | Keep shared modules as current monolith baseline until all consumers are migrated or explicitly deprecated | Do not create shared Java DTO, domain, utility, fixture or internal error-model modules for services |

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

## S06 Candidate

The only near-term candidate is the CLI boundary, and only as an explicit
Gateway mode or Gateway command. The current local-path `analyze` behavior is
not caller-free and must remain available unless a later slice explicitly
deprecates it with tests and migration notes.
