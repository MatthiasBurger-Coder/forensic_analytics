# Three Amigos Decision Record

Decision: `READY_FOR_WORKFLOW`

Confidence: 94 percent.

## Requirement

Regenerate FA-MVP-0001 workflow documentation so runtime repository-source
workspace metadata persistence is PostgreSQL under ADR-0024, while H2 remains
allowed only for deterministic adapter tests and direct fixtures under the
revised ADR-0023.

## Source Of Truth

- ADR-0024 is authoritative for runtime metadata persistence.
- ADR-0023 is accepted for tests only and superseded for runtime by ADR-0024.
- `repository-source-service` is the only owner and writer for repository
  checkout workspace, branch, repository preparation and idempotency records.
- Other services consume only owner APIs and sanitized public DTOs.

## Roles

| Role | Decision |
|---|---|
| Senior Requirement Engineer | Requirement drift is resolved; PostgreSQL is authoritative and H2 runtime is out of scope. |
| Senior System Architect | Service ownership and storage boundaries are clear under ADR-0013, ADR-0023 and ADR-0024. |
| Senior Java Backend Developer | Runtime persistence verification must prove PostgreSQL selection and H2 runtime rejection. |
| Senior React Frontend Developer | UI remains a public REST consumer and must not access private repository-source storage. |
| Senior Tester | Acceptance criteria and quality gates are testable with documented Gradle, npm and Docker commands. |

## Accepted Assumptions

- The updated ADR-0023 on the active branch is accepted as the current H2 scope.
- Existing implementation evidence may be verified during workflow execution
  but is not modified during workflow creation.
- Historical H2 file preservation is a separate migration concern.

## Acceptance Criteria

- FA-MVP-0001 no longer requires H2 runtime or Docker persistence.
- Runtime repository-source metadata persistence uses PostgreSQL.
- H2 is allowed only for deterministic adapter tests and direct fixtures.
- Docker-local deployments do not mount H2 files as active runtime storage.
- No service except `repository-source-service` accesses repository-source
  persistence directly.
- Other services consume only owner APIs and sanitized public DTOs.
- PostgreSQL startup/readiness failure is visible and not hidden by fallback
  storage.
- Existing H2 files are historical MVP data and require an explicit migration
  slice if preservation is needed.
- The workflow references ADR-0023, ADR-0024, data ownership, service
  boundaries and relevant README/test expectations.
- Git status remains clean except intended workflow and documentation artifacts.

## Final Decision

`READY_FOR_WORKFLOW`
