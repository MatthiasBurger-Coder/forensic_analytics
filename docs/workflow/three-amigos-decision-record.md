# Three Amigos Decision Record

## Requirement

Convert the active `forensic_analytics` project from the current modular
platform into an independently deployable microservices ecosystem.

The user supplied a detailed `workflow create` draft on 2026-05-16. Repository
documentation must be written in English, so this workflow records the
requirement in English while preserving the requested architecture intent.

## Decision

```text
READY_FOR_WORKFLOW
```

This decision approves workflow authoring only. It does not approve direct
service extraction, production code moves, persistence changes, runtime routing,
deployment publication, commit or push.

## Requirement Analyst Findings

- Business goal: make forensic analysis workloads independently scalable,
  independently deployable and operationally isolated.
- Technical goal: split gateway, ingestion, repository analysis, AST analysis,
  Joern analysis, BTM generation, analysis storage, graph/replay, reporting and
  frontend concerns into service-owned runtime boundaries.
- Scope: workflow artifacts, migration planning, service-boundary decisions,
  contract-first sequencing, service scaffolding, contracts, tests, deployment
  material and documentation when the workflow is executed.
- Non-scope during workflow creation: production Java changes, source moves,
  endpoint implementation, persistence schema changes, Docker or Kubernetes
  manifests, commits and push.
- Acceptance criteria: each planned service has explicit ownership,
  independently verifiable build/start/test/container evidence, no shared Java
  runtime implementation modules, and communication only through REST/OpenAPI,
  gRPC/protobuf or approved event contracts.

## Architecture Validator Findings

- The current repository is a modular Gradle platform, not a microservice
  ecosystem.
- `docs/adr/ADR-0009-no-shared-common-modules.md`,
  `docs/adr/ADR-0010-contract-first-rest-and-grpc.md` and
  `docs/adr/ADR-0013-data-ownership-per-service.md` already support the core
  microservice constraints.
- `docs/arc42/07-deployment-view.md` currently lists future service roots that
  differ from the user-supplied target service landscape. This workflow treats
  the user-supplied service landscape as the new target to be validated and then
  synchronized into arc42 during execution.
- Behavior-changing service extraction remains blocked until the early
  architecture slices define a strangler or rollback strategy for each extracted
  runtime path.
- Current modular-monolith modules must not be renamed as services without
  independent runtime evidence.

## Quality Validator Findings

- `QUALITY.md` overrides the simpler quality commands in the user draft.
- Minimum repository verification is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

- Full local quality gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

- Documentation-only slices must at least run `git diff --check` and inspect the
  diff. Build, production, contract, test or deployment slices must run the
  applicable `QUALITY.md` commands.
- Frontend verification currently belongs to `forensic-ui` unless a later slice
  moves or replaces that root after verification.

## Dependency And Deadlock Review

- Service implementation slices depend on service-boundary and contract-first
  decisions.
- Persistence-owning and graph/replay slices depend on data ownership decisions.
- Frontend decoupling depends on gateway contracts.
- Local compose, Swarm and Kubernetes slices depend on service Dockerfiles and
  healthcheck contracts.
- Contract and integration tests depend on the contract slice and at least one
  independently startable service.
- Final migration cleanup depends on successful service replacement evidence.

## Risk Level

```text
HIGH
```

The workflow is high risk because it plans service extraction, independent
runtime boundaries, contracts, persistence ownership and deployment material.
The risk is acceptable for workflow authoring because execution is split into
gated slices with explicit stop conditions.

## Required Skills And Role Reviews

- Senior Workflow Architect
- Senior Requirement Engineer
- Senior System Architect
- Microservice Senior Expert
- Senior Java Backend Developer
- Senior gRPC/Proto Specialist
- Senior React Frontend Developer
- Senior UX Designer
- Senior DevOps Engineer
- Senior Tester
- Senior Swarm Orchestrator
- Service Decomposition And Bounded Context
- Contract Governance Expert
- Data Ownership And Persistence Steward
- Microservice Migration Safety Gate
- Microservice Runtime Readiness Expert

## Open Execution Preconditions

- Before any behavior-changing migration slice, record a service-specific
  migration safety record with scope, non-scope, contract impact, data ownership,
  tests, rollback or strangler strategy and forbidden changes.
- Before any service-specific Gradle command is documented as executable, verify
  that the service is included in the actual build.
- Before any Docker, Swarm or Kubernetes command is required, verify that the
  corresponding local tooling and manifests exist.
