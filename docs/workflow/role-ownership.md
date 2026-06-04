# Role Ownership

| Slice | Primary Owner | Secondary Reviewers | Scope |
|---|---|---|---|
| S01 | Senior Requirement Engineer | Senior System Architect, Senior Documentation Engineer, Senior Tester | Requirement and ADR-aligned documentation |
| S02 | Senior Java Backend Developer | Senior System Architect, Senior Tester | Repository-source persistence boundary verification |
| S03 | Senior React Frontend Developer | Senior Java Backend Developer, Senior UX Designer, Senior Tester | Public API and UI DTO boundary verification |
| S04 | Senior DevOps Engineer | Senior System Architect, Senior Security Sandbox Engineer, Senior Tester | Docker-local PostgreSQL and volume boundary verification |
| S05 | Senior Tester | Senior Documentation Engineer, Senior System Architect | Quality closure, arc42 check and handoff |

## Ownership Rules

- `repository-source-service` owns repository checkout workspace metadata,
  branch metadata, repository preparation records, idempotency records,
  PostgreSQL schema, workspace directories and storage readiness.
- `query-report-api-service` owns public REST validation, public DTO mapping,
  error redaction and owner API calls.
- `forensic-ui` owns operator workflow rendering and public REST client usage.
- H2 adapter ownership remains inside repository-source deterministic tests and
  direct fixtures only.

## Non-Owner Rules

- No non-owner service may read repository-source PostgreSQL tables.
- No non-owner service may read repository-source H2 files.
- No non-owner service may read repository-source private checkout directories.
- Public DTOs must stay sanitized and path-free.

## Subagent Note

Callable subagents were not used in workflow creation because the user did not
explicitly ask for delegated or parallel agent work. Role files were applied as
local review checklists.
