# ADR-0019: Allow Spring Boot at service bootstrap boundaries

## Status

Accepted

## Date

2026-05-16

## Context

ADR-0006 accepted Spring Boot only for the existing
`forensic-analytics-boot-app` module until a later ADR named another module and
updated architecture tests.

The active microservices ecosystem workflow requires independent Spring Boot
applications for future backend services. Slice 04 creates the first service at
`services/forensic-ingestion-service`.

## Decision

Allow Spring Boot dependencies and annotations in service-local bootstrap
packages under `de.burger.forensics.analytics.services..bootstrap..`.

This permission is limited to:

- Spring Boot application entrypoints;
- service-local configuration;
- service-local lifecycle wiring;
- service-local property mapping;
- service-local health or readiness wiring when explicitly implemented and
  tested.

Service domain and application packages must remain independent from Spring,
gRPC, generated Protobuf classes, persistence clients and runtime
infrastructure.

The permission does not allow shared Java service modules. Each service must
own its bootstrap, configuration, domain, application, adapters, tests and
Dockerfile.

## Consequences

Architecture tests must allow Spring dependencies in service bootstrap packages
while continuing to forbid them in service domain and application code.

Later service slices may use Spring Boot in their own bootstrap packages without
changing ADR-0006 again, as long as they stay inside this boundary.

Spring Boot starters that add concrete logging providers, Micrometer,
OpenTelemetry, Actuator or other infrastructure still require explicit
dependency and architecture review before use.
