# Three Amigos Decision Record

## Requirement Summary

Move repository checkout workspace metadata from H2 to PostgreSQL using
Liquibase while keeping repository checkout bytes on the existing
repository-source workspace volume.

Clarification accepted on 2026-05-31:

- H2 may remain for tests and deterministic fixtures.
- PostgreSQL is the runtime and production persistence path.
- Missing or unreachable PostgreSQL must be reported by startup failure or
  storage health/readiness `DOWN`; it must not silently fall back to H2.
- Database configuration must be available through the operator Settings UI.

## Decision

`READY_FOR_WORKFLOW`

Confidence: 90 percent.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The request is traceable to repository-source workspace metadata. It extends the current FA-MVP-0001 H2 MVP state and does not create the broader platform workspace domain. |
| Senior System Architect | PostgreSQL is acceptable only as service-owned repository-source storage. ADR and arc42 updates must precede code. Cross-service database access remains forbidden. |
| Senior Java Backend Developer | Existing ports allow a small adapter replacement. H2-specific `MERGE` and schema initialization must be replaced by PostgreSQL `ON CONFLICT` plus Liquibase. Runtime bootstrap must not keep H2 as a fallback outside tests. |
| Senior React Frontend Developer | UI implementation is now in scope through the existing `forensic-ui` Settings route. Settings must use public API adapters and must not connect to PostgreSQL directly. |
| Senior Tester | Existing repository-source tests define persistence behavior. PostgreSQL coverage must be deterministic and default quality gates must not require a live external DB unless explicitly documented. Settings slices require query-report API and frontend tests. |

## Requirement Classification

- Functional requirement: PostgreSQL workspace metadata persistence.
- Architecture constraint: repository-source remains owner and single writer.
- Persistence requirement: Liquibase-managed schema.
- Deployment requirement: Docker-local `forensic-postgres` integration.
- Security requirement: no credential, private path or raw Git output leakage.
- UX requirement: operator Settings screen for database configuration.
- Contract requirement: public Settings API and repository-source handoff must
  be contract-first.
- Resilience requirement: missing PostgreSQL is visible as startup failure or
  health/readiness `DOWN`, never hidden by fallback.
- Quality requirement: repository-source, query-report API, frontend tests and
  full local gate.
- Assumption: existing H2 state does not require automatic migration.

## Dependency and Deadlock Validation

The workflow remains linear. No safe parallel groups exist because later slices
need the prior decision, dependency, schema, adapter, runtime cutover, Settings
contract and UI results.

## Open Questions

No blocking questions remain for workflow creation.

Non-blocking execution-time questions:

- If existing local H2 metadata must be preserved, S07 must stop and convert
  that requirement into an explicit one-off migration slice with verified input
  files and acceptance criteria.
- If runtime database Settings require persistence of raw credentials, S08 must
  stop until a verified secrets-storage boundary exists.
- If changed database settings must apply without restart, S08 must document
  tested reconnect semantics before S09 exposes the behavior in the UI.

S08 execution-time resolution on 2026-05-31:

- Settings operations require an operator token before accepting
  credential-bearing requests.
- Password input is write-only and may be used only for validation through the
  repository-source owner API.
- S08 does not persist changed database credentials and does not hot-apply
  repository-source runtime settings.
- Settings validation and status responses must report `RESTART_REQUIRED` and
  `hotApplySupported: false` for changed database configuration.
- Unreachable PostgreSQL must be reported as `UNREACHABLE`, not as successful
  readiness.

## Final Gate Result

The requirement is ready for workflow execution under the documented
assumptions. Implementation must not begin before `workflow execute` routes
each slice through its owner review.
