# Three Amigos Decision Record

## Decision

`READY_FOR_WORKFLOW`

Confidence: 91 percent.

## Requirement Review

Senior Requirement Engineer review:

- EPIC v0.2 supports server-side repository analysis and producer-neutral
  evidence normalization.
- The request is consistent with the target microservice migration direction.
- The separate deployment workflow requirement is treated as a handoff, not as
  deployment implementation inside this workflow.
- The non-blocking open question is the exact WildFly branch or commit for the
  optional external hardening run.

## Architecture Review

Senior System Architect review:

- `FULL_PATH` is required because tests, contracts, CLI behavior, deployment
  governance and monolith retirement may be affected.
- Gateway must remain a facade; Analysis Store and worker services retain
  orchestration and evidence ownership.
- Legacy module removal is blocked until caller inventory and replacement
  parity are verified.
- Docker Swarm and Kubernetes must stay in a separate workflow because no
  stack file or manifests exist today.

## Backend Review

Senior Java Backend review:

- S01, S03, S06 and S07 may touch Java tests or production code.
- Tests must be regression-first and scoped to the affected module before
  broader gates run.
- CLI migration must be contract-first and must not use shared Java DTOs from
  Gateway or generated transport classes in domain/application logic.

## Frontend Review

Senior React Frontend review:

- No direct frontend change is planned.
- If Gateway OpenAPI response or request fields used by `forensic-ui` change,
  frontend adapter tests become required before the slice can pass.

## Tester Review

Senior Tester review:

- Default E2E tests must be deterministic and independent of network, Docker
  and credentials.
- WildFly remains optional external hardening, with explicit skip evidence.
- Caller-free retirement must run at least the repository minimum command, and
  the full local quality gate is required for module removal or build changes.

## Final Gate

No blocking question prevents workflow creation. `workflow execute` may start
with S00 after the workflow package is committed or otherwise explicitly
accepted by the user.
