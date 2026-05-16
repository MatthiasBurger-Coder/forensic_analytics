# ADR-0011: Run Three Amigos requirement gate before workflow authoring

## Status

Accepted

## Context

Workflow execution can modify architecture, quality gates, skills, roles and documentation. Requirements that are incomplete or ambiguous can create deadlocks, unsafe implementation slices and evidence-integrity risks.

## Decision

New or changed requirements must pass the Three Amigos Requirement Gatekeeper before workflow authoring or execution.

The gate validates:

- business goal
- technical goal
- scope and non-goals
- architecture fit
- quality and testability
- data ownership
- API contracts
- dependency and deadlock risks
- required skills and role reviews

The gate returns `READY_FOR_WORKFLOW` or `REQUIRES_REFINEMENT`.

## Consequences

- Workflow authoring must not begin from incomplete requirements.
- Missing ownership, acceptance criteria, API contracts, data ownership or rollback expectations block readiness.
- Provisional slices must be labeled as provisional and are not executable.
