# Current Coupling Map

## Status

FA-MSA-001 Slice 02 caller and coupling inventory review for workflow
`fa-msa-001-microservice-decomposition-20260521-v1`.

This document records historical coupling that had to be considered before
service extraction and final source-tree retirement. ADR-0022 and S05 supersede
the old "current modular monolith" state: the former source trees are deleted,
and the active Java build is service-only. Historical coupling remains useful
as migration evidence, but it is not current implementation evidence.

S02 verified the inventory on branch
`architecture/workflow-microservice-decomposition-20260521` from starting
commit `128879cda567c4bfcb00dad644a5d9b254ddcf05`.

## Verification Commands

The S02 inventory used explicit file lists instead of a bare `rg -g` search so
that missing matches cannot be confused with a false-empty stdin search:

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --short --branch
git rev-parse HEAD
git ls-files "forensic-analytics-*/build.gradle.kts" "services/*/build.gradle.kts" settings.gradle.kts | sort
git ls-files "*build.gradle.kts" | xargs rg -n "project\\(\\\":forensic-analytics-"
git ls-files "services/*/build.gradle.kts" | xargs -r rg -n "project\\(" || true
rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|ingestion\\.request|ingestion\\.grpc)\\b" services -S -g "*.java" || true
rg -n "RunRepositoryAnalysisUseCase|RunRepositoryAnalysisCommand|DefaultRepositoryAnalysisIngestionUseCase|RepositoryAnalysisIngestionUseCase" forensic-analytics-cli forensic-analytics-rest forensic-analytics-bootstrap forensic-analytics-boot-app forensic-analytics-engine forensic-analytics-ingestion-request forensic-analytics-testbed -S -g "*.java"
git diff --check
```

## Historical Dependency Shape

At the time of S02, the monolith dependency direction was broadly hexagonal:

```text
adapters / runtimes / CLI / REST / gRPC / persistence
        -> application
        -> domain
```

Verified project dependencies included:

| Module | Current Project Dependencies |
|---|---|
| `forensic-analytics-application` | `forensic-analytics-domain` |
| `forensic-analytics-engine` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-adapter-repository-source` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-adapter-javaparser` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-adapter-joern-docker` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-ingestion-grpc` | `api` dependency on `forensic-analytics-application`; implementation dependencies on `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-ingestion-request` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-rest` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-persistence` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-observability` |
| `forensic-analytics-cli` | `forensic-analytics-application`, `forensic-analytics-domain`, `forensic-analytics-ingestion-request`, `forensic-analytics-observability`, `forensic-analytics-persistence` |
| `forensic-analytics-bootstrap` | repository-source adapter, application, gRPC ingestion, observability, persistence, REST |
| `forensic-analytics-boot-app` | Joern adapter, repository-source adapter, application, gRPC ingestion, logging, observability, persistence, REST |
| `forensic-analytics-logging` | `forensic-analytics-observability` |
| `forensic-analytics-testbed` | test dependencies on most backend modules |

## Transitional Service Build Evidence

S02 found no direct `project(...)` dependencies inside
`services/*/build.gradle.kts`. It also found no `project(":services:...")`
dependencies in the tracked Gradle build files. The existing service slices
therefore do not directly include another service as a Gradle project.

Several transitional services generate local transport code from
`contracts/grpc` by configuring their own protobuf source sets. That is current
contract-consumption evidence, not permission to introduce shared Java DTO,
domain, utility, repository, fixture or internal error-model modules. S03 must
still review every service contract and generated-code boundary before product
migration uses it.

S02 also found no production imports from `services/**` into the legacy
monolith packages covered by the scan. Remaining matches in service roots are
generated contract packages, service-local packages or architecture-test
forbidden-package strings.

## Historical Primary Coupling Points

### Boot App Runtime

`forensic-analytics-boot-app` was the strongest predecessor runtime coupling
point. It wired repository-source adapters, application services, gRPC
ingestion, logging, observability, persistence and REST into one Spring Boot
application before service extraction.

This is platform-level runtime composition, not a gateway or service ecosystem.
Future service slices must replace direct project-module coupling with
service-owned models and external contracts.

### Bootstrap Runtime

`forensic-analytics-bootstrap` was a predecessor combined runtime path. It
started gRPC and REST in one process and built backend components directly from
in-memory repositories, application use cases and repository-source adapters.

This path is useful historical pre-retirement evidence, but it is also a
migration coupling that must be retired or isolated only after replacement
evidence exists.

### gRPC Ingestion Adapter

`forensic-analytics-ingestion-grpc` was not contract-only. It contained:

- `forensic_ingestion.proto`;
- generated Protobuf/gRPC classes;
- `ForensicIngestionGrpcService`;
- request validators;
- mappers from Protobuf DTOs to application commands;
- a legacy application project API dependency.

Future contract slices must separate external `.proto` contracts from Java
implementation dependencies. Generated transport types must not become shared
domain or DTO modules between services.

### REST Adapter

`forensic-analytics-rest` was an in-process adapter. It used the JDK HTTP server,
Gson and application/domain types. It is not a gateway service and does not
currently prove service-to-service API boundaries.

### Persistence Adapter

`forensic-analytics-persistence` implemented application ports with in-memory
stores and repositories. It shared application and domain models in-process.
An independent `analysis-store-service` project now exists for job lifecycle
and artifact metadata service behavior. A service-owned durable database is not
verified yet.

### Observability And Logging

`forensic-analytics-observability` and `forensic-analytics-logging` were shared
Java modules in the historical monolith. After S05, no shared Java
observability/logging module remains in the active build. Future independently
deployable services must still not depend on shared Java implementation modules
for runtime behavior.

Service slices need service-owned logging and observability choices or external
operational contracts without shared runtime code.

### Testbed Coupling

`forensic-analytics-testbed` directly test-depended on most backend modules and
contained in-process mini end-to-end and architecture tests. Those tests were
historical monolith evidence. Current testbed evidence lives under
`services:testbed` and still does not by itself prove networked service
boundaries.

Contract and architecture tests exist for implemented service slices, but
networked integration coverage is not complete for the whole target landscape.

## Current Hexagonal Boundary Evidence

Architecture tests currently guard several monolith boundaries:

- application contract neutrality;
- gRPC boundary separation from persistence;
- REST boundary separation from gRPC and persistence;
- logging and Spring dependency confinement;
- observability isolation;
- Spring Boot dependency placement.

The historical production import review did not find outward framework,
adapter, persistence, REST, gRPC or Spring imports from
`forensic-analytics-domain` or `forensic-analytics-application`.

These checks should be preserved during migration, but they do not prove
microservice autonomy.

## Service Extraction Blockers

The following architectural gaps still block broad microservice-readiness
claims:

- not every target service has independently verified build/start/container
  and runtime parity evidence;
- durable service-private persistence ownership is not fully implemented;
- historical contracts and compatibility vocabulary still require explicit
  governance when behavior changes;
- external `contracts/` exists, but not every planned service interaction has
  complete contract-test coverage;
- OpenAPI and event contract files exist, but not every planned interaction is
  implemented or tested;
- no service-private persistence ownership exists;
- transitional service slices have some health endpoints, Docker health checks
  and Compose health conditions, but FA-MSA-001 target landscape readiness is
  not verified for the mandatory target service names;
- graph-replay and report-generation remain deferred or partial evidence, and
  the build-artifact worker is planned without a service root;
- no Docker Swarm or Kubernetes manifests exist;
- no service-specific CI workflow exists.

Any later migration slice must explicitly document which coupling it changes,
which service boundary owns the replacement behavior, which external contract
is used, and which verification proves the replacement.

## No Microservice Claims

The active service projects are technical and architectural modules inside one
repository build. They must not be described as production-ready implemented
microservices until later slices produce runtime evidence for independent
build, start, test, configuration, healthcheck, container and deployment
behavior.

## S02 Inventory Result

S02 kept the remaining `forensic-analytics-*` runtime paths as historical
legacy in-process or rollback evidence. That caller verification is superseded
by the active final-retirement workflow, which requires S04 documentation
cleanup before S05 deletion and S06/S07 closure.

The accepted FA-MSA-001 target is service-owned behavior behind explicit
contracts, not the legacy modules and not the transitional predecessor service
names. Later removal requires caller-free evidence, replacement parity or
explicit deprecation, rollback or operator migration notes and the required
quality gate.

## S14 Retirement-Readiness Update

S14 repeats the caller-free question after S05 through S13 and still resolves
to `NO_REMOVAL_SAFE`. The new `services:testbed` root preserves the legacy
regression surface in a service-root location, which is useful parity evidence
without depending on retained monolith modules in the verified service-only
Gradle model.

Workflow-create refinement after the S14 execution stop is superseded by the
active final-retirement workflow. S04 clears pre-deletion documentation
blockers, S05 is the first deletion-capable slice, S06 closes architecture and
ADR evidence, and S07 owns final release readiness.

Superseded S14 evidence was non-empty at the time:

- all 16 listed legacy modules were still registered in `settings.gradle.kts`;
- `services:testbed` still had test-scoped legacy module dependencies;
- non-zero production imports into retained `application`, `domain`,
  `persistence`, `logging`, `observability`, `rest`, `bootstrap`, `boot` or
  `engine` packages;
- non-zero test imports into those retained packages;
- focused S14 service production scans found no legacy imports in
  `services/**/src/main`, while service test scans still found legacy imports
  in `services/testbed/src/test` at that historical point.

Those findings blocked direct module retirement at S14. Current
final-retirement verification supersedes the counts: `./gradlew projects`
lists only `services:*`, active non-legacy build files have no legacy project
references, active non-legacy Java sources have no legacy monolith imports, and
S05 removed the retired source trees.

## FA-MSA-001-LMR S01 Revalidation

FA-MSA-001-LMR S01 revalidated the caller and dependency inventory on branch
`architecture/workflow-legacy-module-retirement-20260522` after the dedicated
legacy-module-retirement workflow was created.

The S01 inventory used these reproducible checks:

```bash
git ls-files "*build.gradle.kts" | xargs rg -n "project\\(\\\":forensic-analytics-"
rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|ingestion\\.request|ingestion\\.grpc)\\b" services forensic-analytics-* -S -g "**/src/main/**/*.java"
rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|ingestion\\.request|ingestion\\.grpc)\\b" services forensic-analytics-* -S -g "**/src/test/**/*.java"
git diff --check
```

Superseded S01 evidence was non-empty at the time:

- direct legacy Gradle project dependency references across legacy module build
  files and `services/testbed`;
- production Java imports into retained legacy packages;
- test Java imports into retained legacy packages;
- `forensic-analytics-testbed` and `services:testbed` each test-depended on
  retained legacy modules.

S01 therefore recorded `NO_DELETION_SAFE`. No listed legacy module was removed
by S01. The active final-retirement workflow supersedes that with S04
documentation-stopper cleanup, S05 deletion, S06 architecture closure and S07
release readiness after caller-free and service-only build checks.
