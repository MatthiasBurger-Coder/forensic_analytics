# ADR-0006: Spring Boot owns the outer server boundary

## Status

Accepted

## Context

Forensic Analytics is moving from manually assembled server bootstrap code toward a Spring Boot based server application. The repository already separates domain, application, inbound adapters, outbound adapters, observability, persistence, REST, gRPC ingestion and bootstrap code into Gradle modules.

The migration must preserve the existing hexagonal dependency direction. Domain and application code must remain independent from Spring, transport APIs, persistence implementations, logging providers and external tool adapters. Operational logging is already governed by ADR-0005 through the framework-neutral `forensic-analytics-observability` module.

## Decision

Introduce Spring Boot only as an outer server and bootstrap technology.

The accepted target module is `forensic-analytics-boot-app`. It may own:

- the Spring Boot application entrypoint
- Spring bean wiring for existing use cases and adapters
- typed server configuration
- profile-specific startup configuration
- server lifecycle integration for verified inbound adapters

The current `forensic-analytics-*` module names remain canonical for this migration. Broad module renames are not part of this decision.

`forensic-analytics-domain` and `forensic-analytics-application` must not depend on Spring APIs or receive Spring annotations. Existing adapters and infrastructure may be wired by the Boot app from the outside.

ADR-0005 remains in force. This decision does not introduce AspectJ, SLF4J, MDC, Logback, Log4j2, Micrometer, OpenTelemetry or annotation-driven method logging. The Spring Framework runtime may carry `spring-aop` transitively as part of the Boot app's Spring context classpath, but project code must not import Spring AOP APIs or enable AOP behavior without a later ADR. Spring Boot starter dependencies that bring concrete logging providers or logging bridges are not accepted by this ADR unless the dependency is excluded and verified, or a later ADR explicitly accepts Boot-scoped logging. Any future Spring-specific observability bridge requires a separate ADR.

The existing `forensic-analytics-bootstrap` module remains available until Spring Boot startup has verified parity for the selected server behavior. gRPC and REST contracts are not changed by this decision.

## Consequences

- Spring Boot dependencies are allowed only in `forensic-analytics-boot-app` until a later ADR names another module and updates the architecture tests.
- Strict dependency verification must be updated before Spring Boot code is compiled.
- Architecture tests must continue to prove that domain and application stay framework-free.
- The Boot app must not require a database, Joern container, graph database, vector database or live LLM provider for a minimal startup test.
- Manual bootstrap behavior can be retired only after a dedicated parity decision and tests.
