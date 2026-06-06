# arc42 Documentation Layout

## Purpose

This document records the documentation layout rule used by the ADR Baseline
Consolidation workflow.

It does not create product behavior, runtime behavior, deployment behavior or
new architecture decisions. It keeps architecture documentation placement
traceable after ADR-0025.

## Layout Rule

Authoritative architecture, ADR, requirement, conflict-analysis and final-report
outputs created by the ADR Baseline Consolidation workflow belong under
`docs/arc42/`.

Workflow-control artifacts remain under:

```text
docs/workflow/
docs/workflows/adr-baseline-consolidation-20260604/
```

## Current arc42 ADR Outputs

- `docs/arc42/09-architecture-decisions/adr/README.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0001-plugins-are-producers.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0025-consolidated-architecture-baseline-without-migration.md`
- `docs/arc42/09-architecture-decisions/inventory/adr-inventory-20260604.md`
- `docs/arc42/09-architecture-decisions/conflicts/adr-conflict-analysis-20260604.md`
- `docs/arc42/01-introduction-and-goals/requirements/requirement-alignment-20260604.md`

The numbered ADR range is represented by `ADR-0001` through `ADR-0025` in the
arc42 ADR chapter.

## Retired Duplicate ADR Source

The former numbered ADR files under `docs/adr/` were removed on 2026-06-06
after byte-identical authoritative copies were verified under
`docs/arc42/09-architecture-decisions/adr/`. `docs/adr/README.md` remains as a
compatibility pointer and must not duplicate ADR content.

## Historical Source Inputs

Existing documents under `docs/architecture/` may remain repository history,
compatibility input or source evidence until a separate approved slice changes
that location.

They must not be silently treated as newer authoritative outputs when a
workflow output exists under `docs/arc42/`.

The broader documentation-root classification is recorded in
`docs/arc42/08-crosscutting-concepts/documentation-governance/documentation-root-classification-20260605.md`.

## Supporting Architecture Companion Outputs

Documentation companion files that summarize architecture-relevant contract,
deployment or quality guidance may live in their owning arc42 sections. The
current moved companion outputs are:

- `docs/arc42/08-crosscutting-concepts/service-contracts/contract-test-plan.md`
- `docs/arc42/07-deployment-view/forensic-analytics-docker-compose.md`
- `docs/arc42/10-quality-requirements/testing/wildfly-hardening.md`

This rule does not move contract artifacts from `contracts/` or deployment
artifacts from `deployment/` into arc42.

## Constraints

- Do not renumber ADRs.
- Do not convert `AD-*` rows from `docs/arc42/09-architecture-decisions.md`
  into numbered ADR files without an explicit ADR decision.
- Do not duplicate architecture content in pointer stubs.
- Do not describe target services as ready for runtime operation unless
  repository evidence and quality commands prove that readiness.
- Do not invent runtime, deployment or data-transfer flows to make
  documentation layout look complete.
