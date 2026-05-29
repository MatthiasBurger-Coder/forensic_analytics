# Three Amigos Decision Record

## Requirement Summary

Move repository checkout workspace metadata from H2 to PostgreSQL using
Liquibase while keeping repository checkout bytes on the existing
repository-source workspace volume.

## Decision

`READY_FOR_WORKFLOW`

Confidence: 92 percent.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The request is traceable to repository-source workspace metadata. It extends the current FA-MVP-0001 H2 MVP state and does not create the broader platform workspace domain. |
| Senior System Architect | PostgreSQL is acceptable only as service-owned repository-source storage. ADR and arc42 updates must precede code. Cross-service database access remains forbidden. |
| Senior Java Backend Developer | Existing ports allow a small adapter replacement. H2-specific `MERGE` and schema initialization must be replaced by PostgreSQL `ON CONFLICT` plus Liquibase. |
| Senior React Frontend Developer | No UI implementation impact is expected because public workspace routes and DTOs remain stable. Any response-shape change reopens contract and frontend review. |
| Senior Tester | Existing repository-source tests define persistence behavior. PostgreSQL coverage must be deterministic and default quality gates must not require a live external DB unless explicitly documented. |

## Requirement Classification

- Functional requirement: PostgreSQL workspace metadata persistence.
- Architecture constraint: repository-source remains owner and single writer.
- Persistence requirement: Liquibase-managed schema.
- Deployment requirement: Docker-local `forensic-postgres` integration.
- Security requirement: no credential, private path or raw Git output leakage.
- Quality requirement: repository-source regression tests and full local gate.
- Assumption: existing H2 state does not require automatic migration.

## Dependency and Deadlock Validation

The workflow is linear. No safe parallel groups exist because later slices need
the prior decision, dependency, schema and adapter results.

## Open Questions

No blocking questions remain for workflow creation.

Non-blocking execution-time question:

- If existing local H2 metadata must be preserved, S07 must stop and convert
  that requirement into an explicit one-off migration slice with verified input
  files and acceptance criteria.

## Final Gate Result

The requirement is ready for workflow execution under the documented
assumptions. Implementation must not begin before `workflow execute` routes
each slice through its owner review.
