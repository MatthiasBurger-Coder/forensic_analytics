# ADR-0007: REST API strategy under Spring Boot

## Status

Accepted

Post-S05 closure note: ADR-0022 retires the `forensic-analytics-rest` and
`forensic-analytics-boot-app` source trees as active implementation source.
This ADR remains historical context for the predecessor REST strategy. Current
public API contract ownership is service-local where explicitly verified.

## Context

At original acceptance time, the repository contained `forensic-analytics-rest`, a JDK `HttpServer` adapter that exposed the UI-facing REST API under `/api`. That adapter delegated to application use cases, used dedicated browser DTOs and did not reuse gRPC transport classes.

The Spring Boot migration needs explicit REST behavior before the Boot app is treated as a server entrypoint. Adding Spring MVC, WebFlux or Actuator would introduce broader dependencies and endpoint semantics that are not required for the current migration.

Runtime traces, stack traces, source content, LLM prompts and generated hypotheses are sensitive by default under ADR-0003. New REST endpoints that expose those values require authorization, redaction and audit decisions before implementation.

## Decision

The predecessor Boot app initially wrapped the existing JDK REST adapter instead of introducing Spring MVC or WebFlux.

In the original decision, `forensic-analytics-boot-app` could start `forensic-analytics-rest` through a Spring `SmartLifecycle` when `forensics.analytics.rest.enabled=true`. After ADR-0022/S05, that source-tree ownership is historical; current public API ownership is service-local where explicitly verified.

In the original Boot/REST decision, REST was disabled by default in the `docker` and `prod` profiles until authorization, evidence sensitivity, redaction and deployment exposure boundaries were verified. Local predecessor starts could explicitly enable REST and bind it to a configured host and port. After ADR-0022/S05, no active Boot/REST profile remains; current public API exposure must be service-local and separately verified.

This decision does not add new REST endpoints, Actuator endpoints, Spring MVC controllers or WebFlux handlers. It also does not change the existing REST DTO contract.

## Consequences

- The predecessor Boot app could verify enabled and disabled REST lifecycle behavior without changing browser API contracts.
- No Spring web starter or concrete logging provider is introduced for this migration slice.
- REST exposure in container or production profiles requires explicit operator configuration and a later security decision.
- Health checks must not rely on Actuator until a separate ADR accepts the dependency and endpoint behavior.
