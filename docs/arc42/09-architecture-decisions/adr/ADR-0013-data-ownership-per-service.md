# ADR-0013: Assign data ownership per service

## Status

Accepted

## Context

Future service-split work will involve relational stores, graph projections, event streams, vector indexes, file/object artifacts and runtime trace data. Forensic evidence must remain traceable and reproducible.

## Decision

Every persistent data type must have one owner service or module and one owning write path.

Other services may read through:

- owner APIs
- published events
- documented projections
- defined query interfaces

Direct cross-service database access and shared tables as hidden coupling are forbidden.

## Consequences

- Persistence decisions require an ownership report.
- Graph, event, vector, file/object and runtime trace stores require a documented reason.
- Evidence provenance, correlation identifiers and completeness markers must be preserved.
- Unclear ownership blocks workflow execution.
