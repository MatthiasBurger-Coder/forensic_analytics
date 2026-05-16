# Microservice Architecture Workplan

## Goal

Prepare a future Forensic Analytics service split without performing the migration in this slice.

The target architecture must enforce real microservice autonomy:

- every service is independently buildable, runnable, testable, containerized and deployable
- every service owns its own Spring Boot application
- every service owns its own Dockerfile
- every service can run in Docker, Docker Swarm and Kubernetes
- services do not share Java code modules, domain models, event classes or test fixtures
- communication happens only through REST/OpenAPI, gRPC/protobuf or RabbitMQ/message contracts

## Verified Baseline

- Repository path verified from WSL: `/mnt/d/Projects/forensic_analytics`.
- Root quality authority is `QUALITY.md`.
- Root agent authority is `AGENTS.md`.
- Repository skills live under `.agents/skills/<skill-name>/SKILL.md`.
- Project-scoped custom subagents live under `.codex/agents/*.toml`.
- Role descriptions live under `.agents/roles/`.
- Existing active workplan material under `docs/workplan/spring-boot-migration/` remains unchanged.
- Current Gradle modules are still the verified implementation baseline. This workplan does not authorize moving code into `services/`.

## Target Structure

```text
forensic_analytics
├── services
│   ├── forensic-server
│   ├── java-ast-scanner-worker
│   ├── joern-scanner-worker
│   ├── btm-generator-worker
│   └── report-generator-worker
├── contracts
│   ├── rest
│   ├── grpc
│   └── messaging
├── deployment
│   ├── docker-compose
│   ├── docker-swarm
│   └── kubernetes
└── docs
    └── workplan
```

## Architecture Rules

Services must not depend on each other through Java classes or Gradle project modules.

Forbidden sharing:

- shared Java libraries
- shared domain models
- shared event classes
- shared test fixtures
- direct service-to-service class imports
- worker-to-server project dependencies

Allowed integration:

- REST APIs described by OpenAPI documents
- gRPC APIs described by `.proto` files
- RabbitMQ messages described by message contracts
- deployment contracts for Docker, Docker Swarm and Kubernetes

Contracts may be centrally documented under `contracts/`, but generated Java code or contract helpers must not become a shared service implementation dependency.

## Non-Goals

- Do not migrate existing modules in this slice.
- Do not create service directories before each service boundary is verified.
- Do not introduce Docker, Swarm or Kubernetes manifests before service ownership is clear.
- Do not split domain or application modules speculatively.
- Do not create shared Java contract libraries.
- Do not weaken hexagonal architecture or forensic evidence rules.

## Slice 01 - Service Boundary Inventory

Purpose:

- Map the verified existing modules to future service ownership candidates.
- Identify which code would belong to `forensic-server`, scanner workers, BTM generation and report generation.

Prerequisites:

- Root `AGENTS.md`.
- Root `QUALITY.md`.
- Current `settings.gradle.kts`.
- Current `docs/README.md` module inventory.

Affected files:

- documentation only

Owner role:

- Microservice Senior Expert
- Senior System Architect

Allowed write scope:

- future workplan notes only

Dependencies:

- none

Parallelization status:

- blocking for all service extraction

Done criteria:

- every future service has a documented responsibility boundary
- no service ownership is guessed
- unresolved ownership is listed explicitly

Verification commands:

```bash
git status --short
```

Stop conditions:

- module ownership cannot be verified
- current documentation and Gradle module layout conflict in a way that changes service boundaries

## Slice 02 - Contract Boundary Plan

Purpose:

- Define which interactions use REST/OpenAPI, gRPC/protobuf or RabbitMQ/message contracts.
- Keep contracts separate from service implementation code.

Prerequisites:

- Slice 01 complete.
- Existing gRPC, REST and messaging requirements verified from source and documentation.

Affected files:

- `contracts/rest/**`
- `contracts/grpc/**`
- `contracts/messaging/**`
- related documentation

Owner role:

- Microservice Senior Expert
- Senior gRPC/Proto Specialist
- Senior Documentation Engineer

Allowed write scope:

- contract documentation and schema files only

Dependencies:

- Slice 01

Parallelization status:

- partly parallel after service ownership is verified

Done criteria:

- every cross-service interaction has one approved external contract mechanism
- contract files do not introduce shared Java implementation modules
- unknown contract fields are documented as unresolved instead of inferred

Verification commands:

```bash
git status --short contracts docs
```

Stop conditions:

- an interaction requires an unverified event field, RPC method or REST endpoint
- contract ownership cannot be assigned to a service boundary

## Slice 03 - Service Scaffold Plan

Purpose:

- Plan minimal service scaffolds for each future service without migrating business logic prematurely.

Target service roots:

```text
services/forensic-server
services/java-ast-scanner-worker
services/joern-scanner-worker
services/btm-generator-worker
services/report-generator-worker
```

Prerequisites:

- Slice 01 complete.
- Slice 02 complete for any service that needs external communication.

Affected files:

- `services/**`
- `settings.gradle.kts` only after exact service modules are approved

Owner role:

- Microservice Senior Expert
- Senior Java Backend
- Senior DevOps
- Senior Tester

Allowed write scope:

- one service scaffold per implementation slice

Dependencies:

- Slice 01
- Slice 02 when contracts are required

Parallelization status:

- parallel only when service roots and Gradle write scopes are disjoint

Done criteria:

- each scaffold has its own Spring Boot application
- each scaffold has service-local tests
- each scaffold has its own Dockerfile and README
- no scaffold depends on another service as a Java module

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- a service scaffold requires shared Java domain or event code
- a Spring Boot application boundary would depend on another service implementation

## Slice 04 - Deployment Plan

Purpose:

- Prepare independent deployment material for local Docker, Docker Swarm and Kubernetes.

Prerequisites:

- A service scaffold exists for the service being deployed.
- Service ports and health checks are verified.

Affected files:

- `deployment/docker-compose/**`
- `deployment/docker-swarm/**`
- `deployment/kubernetes/**`
- service-owned Dockerfiles

Owner role:

- Microservice Senior Expert
- Senior DevOps
- Senior Security Sandbox Engineer

Allowed write scope:

- deployment descriptors for one verified service at a time

Dependencies:

- Slice 03 for the affected service

Parallelization status:

- parallel per service after ports and image names are stable

Done criteria:

- each service can be built as an independent image
- each service has Docker, Swarm and Kubernetes deployment material
- health checks are service-owned and do not require shared code
- secrets and sensitive runtime data are not embedded in descriptors

Verification commands:

```bash
git status --short services deployment
```

Stop conditions:

- service ports, health endpoints, image names or environment variables are not verified
- deployment would require an unapproved shared runtime dependency

## Quality Gate

Use `QUALITY.md` as the authority.

Minimum verification for documentation-only changes:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate when implementation or build logic changes:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Do not claim a command passed unless it was executed.

## Stop Conditions

Stop instead of guessing when:

- a service boundary cannot be verified
- a Gradle module dependency would create shared service code
- a contract field, RPC method, REST endpoint or message property is missing
- service-local deployment ownership is unclear
- Docker, Swarm or Kubernetes descriptors would require unverified ports or health checks
- current implementation and documentation disagree about service ownership

## Current Slice Status

This file only prepares future service-split work. No service migration, contract implementation, deployment descriptor or shared module change is performed by this slice.
