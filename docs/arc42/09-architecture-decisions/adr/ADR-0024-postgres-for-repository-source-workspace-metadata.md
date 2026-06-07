# ADR-0024: Use PostgreSQL for repository-source workspace metadata

## Status

Accepted

## Date

2026-05-29

## Context

The repository already contains a local PostgreSQL runtime named
`forensic-postgres`. The repository-source workspace MVP currently persists
repository checkout workspace, branch, repository preparation and idempotency
records through the service-local H2 adapter accepted by ADR-0023.

The new workspace persistence requirement moves repository-source workspace
metadata to PostgreSQL and keeps checked-out repository bytes on the existing
repository-source workspace volume.

ADR-0013 still requires one owner and one write path for every persistent data
type. The broader platform relational persistence decision remains open
outside this bounded repository-source metadata scope.

## Decision

Use PostgreSQL as the service-owned metadata store for
`repository-source-service` repository checkout workspace state.

The PostgreSQL schema is owned by `repository-source-service` and may persist
only:

- repository checkout workspaces;
- repository checkout branches;
- repository preparation records;
- repository-source idempotency records.

Liquibase owns schema creation and evolution for this repository-source schema.
Implementation slices must trace every table and column to the verified
repository-source domain model, application ports or existing H2 persistence
fields.

Repository checkout bytes, source package bytes and private workspace
directories stay outside PostgreSQL in service-owned storage. Other services
must access repository-source state through owner APIs, public facade APIs or
explicit contracts only.

## Consequences

- `repository-source-service` remains the only writer for repository checkout
  workspace metadata.
- `forensic-postgres` is not shared cross-service storage. Direct reads of
  repository-source tables by `query-report-api-service`, UI, CLI, workers or
  later analysis services are forbidden.
- Public APIs may expose opaque workspace IDs, branch IDs, source snapshot IDs,
  relative source roots and sanitized diagnostics only.
- PostgreSQL credentials, JDBC URLs, table names, private workspace paths, raw
  Git output and private network details must not leak into public DTOs,
  diagnostics or logs.
- ADR-0023 remains the historical H2 MVP decision until the H2 retirement slice
  removes active H2 runtime fallback or records an explicit migration policy.
- OD-001 remains open for broader canonical Analytics persistence, runtime
  evidence storage, graph/replay projections, reports, LLM packages and
  platform workspace administration.

## Related Documents

- `docs/workflow/workflow.md`
- `docs/workflow/three-amigos-decision-record.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/11-risks-and-technical-debt.md`
