# ADR-0010: Use contract-first REST and gRPC communication

## Status

Accepted

## Context

The platform contains REST and gRPC adapter boundaries and will continue to use service communication for ingestion, analysis and UI-facing workflows. Service autonomy requires contracts to be explicit before implementation.

## Decision

REST and gRPC communication changes must be contract-first.

Before implementation, a workflow must identify:

- producer
- consumers
- request and response semantics
- error model
- versioning and compatibility impact
- tests
- security expectations

Breaking changes require explicit ADR or compatibility review.

## Consequences

- DTO sharing through Java common modules is not allowed.
- Protobuf field semantics and numbering must be reviewed.
- REST error models must be documented and testable.
- Unknown consumers or unknown compatibility impact block execution.
