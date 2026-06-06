# ADR-0006: Spring Boot owns the outer server boundary

## Status

Accepted

Post-S05 closure note: ADR-0022 retires the
`forensic-analytics-boot-app` and `forensic-analytics-bootstrap` source trees
as active implementation source. This ADR remains historical context for the
predecessor Boot boundary; active Spring Boot permission for service-local
bootstrap packages is governed by ADR-0019.

## Context

At original acceptance time, Forensic Analytics was moving from manually assembled server bootstrap code toward a Spring Boot based server application. The repository then separated domain, application, inbound adapters, outbound adapters, observability, persistence, REST, gRPC ingestion and bootstrap code into Gradle modules.

The migration had to preserve the existing hexagonal dependency direction. Domain and application code had to remain independent from Spring, transport APIs, persistence implementations, logging providers and external tool adapters. Operational logging was governed by ADR-0005 through the framework-neutral predecessor `forensic-analytics-observability` module.

## Decision

Introduce Spring Boot only as an outer server and bootstrap technology.

The original accepted target module was `forensic-analytics-boot-app`. It owned:

- the Spring Boot application entrypoint
- Spring bean wiring for existing use cases and adapters
- typed server configuration
- profile-specific startup configuration
- server lifecycle integration for verified inbound adapters

The pre-S05 `forensic-analytics-*` module names were canonical for the original migration. Broad module renames were not part of this decision.

The predecessor `forensic-analytics-domain` and `forensic-analytics-application` modules were not allowed to depend on Spring APIs or receive Spring annotations. Active service domain and application packages remain framework-free under ADR-0019.

ADR-0005 remains in force. This decision does not introduce AspectJ, SLF4J, MDC, Logback, Log4j2, Micrometer, OpenTelemetry or annotation-driven method logging. The Spring Framework runtime may carry `spring-aop` transitively as part of the Boot app's Spring context classpath, but project code must not import Spring AOP APIs or enable AOP behavior without a later ADR. Spring Boot starter dependencies that bring concrete logging providers or logging bridges are not accepted by this ADR unless the dependency is excluded and verified, or a later ADR explicitly accepts Boot-scoped logging. Any future Spring-specific observability bridge requires a separate ADR.

The predecessor `forensic-analytics-bootstrap` module remained available in the original decision until Spring Boot startup had verified parity for the selected server behavior. ADR-0022 supersedes that retained-source assumption after S05 deletion. gRPC and REST contracts are not changed by this decision.

## Consequences

- Spring Boot dependencies were allowed only in `forensic-analytics-boot-app` until a later ADR named another module and updated the architecture tests; ADR-0019 now governs service-local bootstrap packages.
- Strict dependency verification must be updated before Spring Boot code is compiled.
- Architecture tests must continue to prove that domain and application stay framework-free.
- The Boot app must not require a database, Joern container, graph database, vector database or live LLM provider for a minimal startup test.
- Manual bootstrap behavior can be retired only after a dedicated parity decision and tests.
