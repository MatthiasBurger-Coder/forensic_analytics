# Architecture Decision Records

This directory contains architecture decisions for the Forensics Platform.

The decisions are derived from the EPIC baseline and refined during implementation.

## Records

- [ADR-0001: Plugins trigger server-side analysis, not the platform](ADR-0001-plugins-are-producers.md)
- [ADR-0002: Use a canonical analysis model](ADR-0002-canonical-analysis-model.md)
- [ADR-0003: Runtime events are sensitive by default](ADR-0003-runtime-events-are-sensitive.md)
- [ADR-0004: Graph DB and Vector DB are projections](ADR-0004-graph-and-vector-db-as-projections.md)
- [ADR-0005: Adapter logging uses a JDK observability boundary](ADR-0005-adapter-logging-observability-boundary.md)
- [ADR-0006: Spring Boot owns the outer server boundary](ADR-0006-spring-boot-server-boundary.md)
- [ADR-0007: REST API strategy under Spring Boot](ADR-0007-rest-api-spring-strategy.md)
- [ADR-0008: Cross-cutting logging module with Spring method interception](ADR-0008-cross-cutting-logging-module.md)
- [ADR-0009: Do not share Java implementation modules between services](ADR-0009-no-shared-common-modules.md)
- [ADR-0010: Use contract-first REST and gRPC communication](ADR-0010-contract-first-rest-and-grpc.md)
- [ADR-0011: Run Three Amigos requirement gate before workflow authoring](ADR-0011-three-amigos-before-workflow.md)
- [ADR-0012: Require quality gates before commit and push](ADR-0012-quality-gates-before-commit.md)
- [ADR-0013: Assign data ownership per service](ADR-0013-data-ownership-per-service.md)
- [ADR-0014: Use an explicit agent handoff protocol](ADR-0014-agent-handoff-protocol.md)
- [ADR-0015: Use skill registry and conflict auditing](ADR-0015-skill-registry-conflict-auditing.md)
- [ADR-0016: Create workflow branches before workflow artifacts](ADR-0016-branch-first-workflow-creation.md)
- [ADR-0017: Use the target microservices service landscape](ADR-0017-target-microservices-service-landscape.md)
- [ADR-0018: Author initial logical service contracts before implementation](ADR-0018-initial-logical-contracts.md)
- [ADR-0019: Allow Spring Boot at service bootstrap boundaries](ADR-0019-spring-boot-service-bootstrap-boundary.md)
