# Current Coupling Map

## Status

FA-MSA-001 Slice 02 caller and coupling inventory review for workflow
`fa-msa-001-microservice-decomposition-20260521-v1`.

This document records the current coupling that must be considered before
service extraction. The current coupling is acceptable for the existing
modular monolith, but it blocks any claim that current modules are already
microservices.

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

## Current Dependency Shape

The current dependency direction is broadly hexagonal:

```text
adapters / runtimes / CLI / REST / gRPC / persistence
        -> application
        -> domain
```

Verified project dependencies include:

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

## Primary Coupling Points

### Boot App Runtime

`forensic-analytics-boot-app` is the strongest current runtime coupling point.
It wires repository-source adapters, application services, gRPC ingestion,
logging, observability, persistence and REST into one Spring Boot application.

This is platform-level runtime composition, not a gateway or service ecosystem.
Future service slices must replace direct project-module coupling with
service-owned models and external contracts.

### Bootstrap Runtime

`forensic-analytics-bootstrap` is a second combined runtime path. It starts
gRPC and REST in one process and builds backend components directly from
in-memory repositories, application use cases and repository-source adapters.

This path is useful current-state evidence, but it is also a migration coupling
that must be retired or isolated only after replacement evidence exists.

### gRPC Ingestion Adapter

`forensic-analytics-ingestion-grpc` is not contract-only. It contains:

- `forensic_ingestion.proto`;
- generated Protobuf/gRPC classes;
- `ForensicIngestionGrpcService`;
- request validators;
- mappers from Protobuf DTOs to application commands;
- an `api(project(":forensic-analytics-application"))` dependency.

Future contract slices must separate external `.proto` contracts from Java
implementation dependencies. Generated transport types must not become shared
domain or DTO modules between services.

### REST Adapter

`forensic-analytics-rest` is an in-process adapter. It uses the JDK HTTP server,
Gson and application/domain types. It is not a gateway service and does not
currently prove service-to-service API boundaries.

### Persistence Adapter

`forensic-analytics-persistence` implements application ports with in-memory
stores and repositories. It shares application and domain models in-process.
An independent `analysis-store-service` project now exists for job lifecycle
and artifact metadata service behavior. A service-owned durable database is not
verified yet.

### Observability And Logging

`forensic-analytics-observability` and `forensic-analytics-logging` are shared
Java modules in the current monolith. This is acceptable in the current build,
but future independently deployable services must not depend on shared Java
implementation modules for runtime behavior.

Service slices need service-owned logging and observability choices or external
operational contracts without shared runtime code.

### Testbed Coupling

`forensic-analytics-testbed` directly test-depends on most backend modules and
contains in-process mini end-to-end and architecture tests. These tests verify
the current modular monolith, not networked service boundaries.

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

Production import review did not find outward framework, adapter, persistence,
REST, gRPC or Spring imports from `forensic-analytics-domain` or
`forensic-analytics-application`.

These checks should be preserved during migration, but they do not prove
microservice autonomy.

## Service Extraction Blockers

The following current couplings block microservice claims:

- current modules share Java domain and application code;
- current candidates share in-memory persistence implementations;
- the existing gRPC proto is stored inside a Java implementation module;
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

The current modules are technical and architectural modules inside one
repository build. They must not be renamed or described as implemented
microservices until later slices produce runtime evidence for independent
build, start, test, configuration, healthcheck, container and deployment
behavior.

## S02 Inventory Result

S02 keeps the remaining `forensic-analytics-*` runtime paths registered as
legacy in-process or rollback evidence. Caller verification still finds active
dependencies for CLI, REST, bootstrap, Boot, engine, ingestion-request and
testbed behavior, so removal would break verified current behavior.

The accepted FA-MSA-001 target is service-owned behavior behind explicit
contracts, not the legacy modules and not the transitional predecessor service
names. Later removal requires caller-free evidence, replacement parity or
explicit deprecation, rollback or operator migration notes and the required
quality gate.
