# ADR-0023: Use H2 only for repository-source MVP persistence

## Status

Accepted for tests only. Superseded for runtime by ADR-0024.

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
owner APIs and sanitized public DTOs.

ADR-0024 selects PostgreSQL as the service-owned runtime and production
metadata store for repository-source workspace, branch, preparation and
idempotency records.

## Decision

Historically, the first MVP slice used an H2 file database as a service-local
persistence adapter for `repository-source-service` until the PostgreSQL cutover.
This runtime usage ended with ADR-0024 and workflow version `2026-05-31`.

After ADR-0024 and the workflow baseline documented in
`docs/workflow/workflow.md` with version/date `2026-05-31`, H2 is no longer a runtime or
Docker fallback. H2 tests must not validate runtime fallback behavior, Docker startup behavior,
production readiness behavior or cross-service persistence behavior.

The H2 adapter may persist:

- repository checkout workspaces;
- repository checkout branches;
- repository preparation records;
- repository-source idempotency records.

The H2 files are private repository-source storage. They must not be mounted,
read or queried by `query-report-api-service`, the UI, worker services, CLI
clients or later analysis services.

This decision does not select H2 as canonical analytics persistence. Runtime
operation must use PostgreSQL and must report missing or unreachable PostgreSQL
through startup failure or storage readiness `DOWN`. Storage readiness `DOWN` must not be masked by an in-memory, H2 or file-based
fallback.

## Consequences

- `repository-source-service` retains H2 adapter tests as deterministic fixture
  coverage.
- Docker-local deployments no longer mount repository-source H2 data as active
  runtime storage.
- Public APIs may expose opaque workspace IDs, branch IDs, source snapshot IDs,
  relative source roots and sanitized diagnostics only.
- Cross-service database access, shared H2 tables and public H2 path exposure
  remain forbidden.
- Existing local H2 files are historical MVP state. If preservation is
  required, workflow execution must stop and create an explicit one-off
  migration slice with verified input files, acceptance criteria, rollback
  strategy and quality gates.

## Related Documents

- `docs/workflow/workflow.md`
- `docs/workflow/execution-report.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/data-ownership.md`
- `docs/arc42/08-crosscutting-concepts/architecture-source-maps/service-boundaries.md`
- `docs/arc42/09-architecture-decisions.md`
- `docs/arc42/11-risks-and-technical-debt.md`
