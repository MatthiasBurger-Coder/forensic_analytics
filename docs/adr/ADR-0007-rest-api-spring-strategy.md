# ADR-0007: REST API strategy under Spring Boot

## Status

Accepted

## Context

The repository already contains `forensic-analytics-rest`, a JDK `HttpServer` adapter that exposes the current UI-facing REST API under `/api`. That adapter delegates to application use cases, uses dedicated browser DTOs and does not reuse gRPC transport classes.

The Spring Boot migration needs explicit REST behavior before the Boot app is treated as a server entrypoint. Adding Spring MVC, WebFlux or Actuator would introduce broader dependencies and endpoint semantics that are not required for the current migration.

Runtime traces, stack traces, source content, LLM prompts and generated hypotheses are sensitive by default under ADR-0003. New REST endpoints that expose those values require authorization, redaction and audit decisions before implementation.

## Decision

The Boot app initially wraps the existing JDK REST adapter instead of introducing Spring MVC or WebFlux.

`forensic-analytics-boot-app` may start `forensic-analytics-rest` through a Spring `SmartLifecycle` when `forensics.analytics.rest.enabled=true`. The REST contract remains owned by `forensic-analytics-rest`; Boot only supplies configuration and lifecycle wiring.

REST is disabled by default in the `docker` and `prod` profiles until authorization, evidence sensitivity, redaction and deployment exposure boundaries are verified. Local starts may explicitly enable REST and bind it to a configured host and port.

This decision does not add new REST endpoints, Actuator endpoints, Spring MVC controllers or WebFlux handlers. It also does not change the existing REST DTO contract.

## Consequences

- The Boot app can verify enabled and disabled REST lifecycle behavior without changing browser API contracts.
- No Spring web starter or concrete logging provider is introduced for this migration slice.
- REST exposure in container or production profiles requires explicit operator configuration and a later security decision.
- Health checks must not rely on Actuator until a separate ADR accepts the dependency and endpoint behavior.
