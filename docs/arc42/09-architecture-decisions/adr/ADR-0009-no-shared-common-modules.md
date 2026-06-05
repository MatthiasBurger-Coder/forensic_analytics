# ADR-0009: Do not share Java implementation modules between services

## Status

Accepted

## Context

Forensic Analytics is defining a governed skill and workflow landscape for future service-split work. Root `AGENTS.md` already requires microservice autonomy and forbids shared Java code modules between independently deployable services.

## Decision

Future microservice work must not introduce shared Java implementation modules, shared domain model modules, shared event implementation classes or direct service-to-service class dependencies.

Allowed integration mechanisms are explicit external contracts:

- REST/OpenAPI
- gRPC/protobuf
- message contracts such as RabbitMQ events

Contract documentation may be central, but it must not become a shared Java implementation dependency.

## Consequences

- Contract-first review is required before service communication changes.
- Each service owns its internal domain model and adapters.
- Code reuse pressure must be resolved through explicit contracts, not shared implementation modules.
- Workflow slices that violate this rule are blocked.
