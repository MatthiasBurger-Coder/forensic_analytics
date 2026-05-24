# ADR-0023: Use H2 only for repository-source MVP persistence

## Status

Accepted

## Date

2026-05-24

## Context

FA-MVP-0001 needs durable repository checkout workspace state before the
platform wires JavaParser, Joern, BTM generation, reports, replay or LLM
context generation. The implemented first slice stores repository-source
workspace, branch, repository preparation and idempotency records across local
service restarts.

ADR-0013 requires one owner and one write path for persistent data. For this
MVP, the owner is `repository-source-service`; other services may consume only
owner APIs and sanitized public DTOs. The broader production relational
database decision remains open as OD-001 in arc42.

## Decision

Use an H2 file database as a service-local, Docker-local MVP persistence
adapter for `repository-source-service` only.

The H2 adapter may persist:

- repository checkout workspaces;
- repository checkout branches;
- repository preparation records;
- repository-source idempotency records.

The H2 files are private repository-source storage. They must not be mounted,
read or queried by `query-report-api-service`, the UI, worker services, CLI
clients or later analysis services.

This decision does not select H2 as canonical analytics persistence and does
not close the production relational database decision.

## Consequences

- `repository-source-service` owns the H2 schema, JDBC configuration, schema
  initialization and adapter tests.
- Docker-local deployments mount repository-source H2 data only into
  `repository-source-service`.
- Public APIs may expose opaque workspace IDs, branch IDs, source snapshot IDs,
  relative source roots and sanitized diagnostics only.
- Cross-service database access, shared H2 tables and public H2 path exposure
  remain forbidden.
- A later production persistence migration requires a separate owner decision,
  contract impact review, data migration or rollback strategy and quality
  gates.

## Related Documents

- `docs/workflow/workflow.md`
- `docs/workflow/execution-report.md`
- `docs/architecture/data-ownership.md`
- `docs/architecture/service-boundaries.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/11-risks-and-technical-debt.md`
