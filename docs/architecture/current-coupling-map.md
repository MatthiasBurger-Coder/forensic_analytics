# Current Coupling Map

## Status

Slice 00 baseline for the microservices ecosystem conversion workflow.

This document records the current coupling that must be considered before
service extraction. The current coupling is acceptable for the existing
modular monolith, but it blocks any claim that current modules are already
microservices.

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
There is no verified independent analysis-store service or service-owned
durable database yet.

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

Contract and integration tests for services do not exist yet.

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
- no external `contracts/` area exists;
- no OpenAPI or event contract files were verified;
- no service-private persistence ownership exists;
- no per-service health checks exist;
- no service-local Dockerfiles, READMEs, tests or configuration exist;
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
