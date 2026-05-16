# Contract Governance

## Status

Governance for future service communication. This document does not introduce
new REST routes, gRPC methods, events, schemas or generated code.

## Contract-First Rule

Every future cross-service call must be designed through an explicit contract
before implementation starts.

Allowed contract forms:

- REST/OpenAPI
- gRPC/protobuf
- approved message or event contracts

## Required Contract Record

Each service communication slice must document:

- protocol
- producer
- consumers
- contract file or planned contract slice
- versioning policy
- request model
- response model
- event model when applicable
- error or status model
- idempotency and retry expectations
- timeout, deadline or cancellation expectations
- compatibility impact
- generated-code boundary
- contract tests

## Java Boundary

Shared OpenAPI, protobuf or message contract files may be centrally documented.
Generated transport classes and Java DTOs must stay in adapter boundaries and
must not leak into domain or application packages.

Shared Java implementation, domain, DTO, service, repository, utility or
internal error-model modules are forbidden between independently deployable
services.

## Stop Conditions

Stop service communication work when:

- REST path, method, status code or error model is unclear
- RPC method, message semantics, field numbers or reserved fields are unclear
- event names, fields, ordering or delivery semantics are unclear
- consumers are unknown for a breaking change
- compatibility impact cannot be verified
- generated transport classes would leak inward
- continuing would require guessing request, response, event, error, timeout,
  retry, idempotency or compatibility behavior
