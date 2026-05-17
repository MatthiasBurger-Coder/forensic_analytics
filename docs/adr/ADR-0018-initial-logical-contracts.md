# ADR-0018: Author initial logical service contracts before implementation

## Status

Accepted

## Date

2026-05-16

## Context

The microservices ecosystem workflow requires contract-first communication
before service implementation slices. Some target service interactions do not
yet have implemented endpoints, RPCs, events or worker protocols.

The repository normally forbids guessing missing contracts. During Slice 03,
the user explicitly approved creating plausible initial communication contracts
for not-yet-defined interactions, with the constraint that communication must
be logical and the contracts must not claim implementation evidence.

## Decision

Author initial logical contracts for planned service communication in Slice 03.

These contracts must:

- distinguish current verified implementation evidence from planned design;
- remain interface descriptions only;
- avoid shared Java implementation, DTO, domain, mapper, exception, fixture or
  utility modules;
- preserve existing gRPC v1 ingestion field numbers and deprecated field
  compatibility;
- define REST, gRPC and event error/status, retry, idempotency and
  generated-code-boundary rules;
- require contract tests before runtime readiness is claimed.

## Consequences

Future service slices may implement against these contracts without inventing
communication semantics again.

Planned contracts may evolve through the compatibility rules before production
traffic depends on them. Once an operation, RPC or event is implemented and
tested, later changes must follow the versioning and compatibility rules in
`docs/architecture/contract-versioning.md`.

The initial contracts are not proof that Gateway, worker, report, replay,
health or event runtime behavior exists.
