# Three Amigos Requirement Gate

The Three Amigos Requirement Gate is the mandatory intake gate for
`workflow create`.

It validates requirement clarity, architecture fit, implementation impact,
testability and slice feasibility before executable workflow artifacts are
created or changed.

## Required Roles

| Role | Review focus |
|---|---|
| Senior Requirement Engineer | Business goal, technical goal, scope, non-goals, assumptions and acceptance criteria |
| Senior System Architect | Architecture boundaries, service ownership, evidence semantics, API and data ownership impact |
| Senior Java Backend Developer | Backend impact, hexagonal boundaries, ports, adapters and JUnit 6 testability |
| Senior React Frontend Developer | Frontend impact, UX/API adapter boundaries and React build/test impact |
| Senior Tester | Regression strategy, quality gates, deterministic fixtures and stop conditions |

## Decision

The gate returns exactly one decision:

```text
READY_FOR_WORKFLOW
```

or:

```text
REQUIRES_REFINEMENT
```

Use `READY_FOR_WORKFLOW` only when scope, non-goals, affected boundaries,
acceptance criteria, slice dependencies, required roles and quality gates are
known.

Use `REQUIRES_REFINEMENT` when continuing would require guessing requirement
intent, architecture boundaries, data ownership, testability, evidence semantics
or role ownership.

## Output Contract

The gate report must include:

- Normalized requirement summary.
- Scope and non-goals.
- Backend impact.
- Frontend impact.
- Architecture and evidence-integrity validation.
- Quality and testability validation.
- Dependency and deadlock review.
- Required skills, agents or role reviews.
- Draft slice boundaries when ready.
- Open questions and blockers.
- Final decision.

The gate does not implement product code and does not execute workflow slices.
