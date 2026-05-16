# Spring Boot Migration Workplan

This directory contains the active workplan for migrating Forensic Analytics toward a Spring Boot based server application while preserving the existing hexagonal boundaries and evidence integrity rules.

The workplan was regenerated on 2026-05-15 from the verified repository state at:

```text
/mnt/d/Projects/forensic_analytics
```

The matching Windows path is:

```text
D:/Projects/forensic_analytics
```

## Workplan Files

1. [spring-boot-migration/00-inventory.md](spring-boot-migration/00-inventory.md) - verified repository, dependency, module, logging and quality inventory.
2. [spring-boot-migration/01-target-architecture.md](spring-boot-migration/01-target-architecture.md) - target architecture, non-goals and module mapping.
3. [spring-boot-migration/02-version-and-dependency-plan.md](spring-boot-migration/02-version-and-dependency-plan.md) - Spring Boot, Gradle and dependency verification plan.
4. [spring-boot-migration/03-implementation-slices.md](spring-boot-migration/03-implementation-slices.md) - ordered executable slices with owners, write scopes, done criteria and stop conditions.
5. [spring-boot-migration/04-dependency-graph-and-parallelization.md](spring-boot-migration/04-dependency-graph-and-parallelization.md) - slice dependency graph and parallelization opportunities.
6. [spring-boot-migration/05-quality-gates.md](spring-boot-migration/05-quality-gates.md) - targeted and full verification commands from `QUALITY.md`.
7. [spring-boot-migration/06-documentation-sync.md](spring-boot-migration/06-documentation-sync.md) - documentation, ADR and arc42 synchronization points.
8. [spring-boot-migration/07-stop-conditions-and-uncertainty.md](spring-boot-migration/07-stop-conditions-and-uncertainty.md) - conditions that require stopping instead of guessing.
9. [spring-boot-migration/08-commit-and-push-plan.md](spring-boot-migration/08-commit-and-push-plan.md) - suggested commit boundaries for later implementation.
10. [spring-boot-migration/quality-log.md](spring-boot-migration/quality-log.md) - initial quality log and future execution log.

Additional planning material:

- [microservice-architecture-workplan.md](microservice-architecture-workplan.md) - future microservice split preparation with service-autonomy, contract and deployment guardrails.
- [../architecture/microservice-governance.md](../architecture/microservice-governance.md) - active governance for future service boundaries, no shared Java implementation modules and runtime independence evidence.
- [../governance/contract-governance.md](../governance/contract-governance.md) - active governance for REST/OpenAPI, gRPC/protobuf and event contract-first service communication.

## Planning Status

This workplan is planning material only. It does not authorize speculative implementation.

Each slice must begin with read-only verification of the exact files, symbols, tasks, modules and contracts it will touch. If an expected class, task, schema field, package, dependency alias, Gradle plugin or architecture rule cannot be verified, implementation must stop and report the mismatch.

## Key Direction

Spring Boot is introduced at the outer server/bootstrap boundary.

Core domain and application code remain framework-free:

```text
Spring Boot adapters / boot app / infrastructure
        -> application ports and use cases
        -> domain
```

The current repository already contains `forensic-analytics-domain`, `forensic-analytics-application`, `forensic-analytics-observability`, gRPC ingestion, REST, persistence, source adapters and bootstrap modules. The migration therefore starts by adding a Spring Boot application boundary around verified modules instead of renaming modules first.
