# Build Engineering Governance System Workplan

## Goal

Create a reusable engineering governance system for Codex-based development.

The system standardizes:

- requirements
- EPIC management
- arc42 synchronization
- workplan generation
- slice orchestration
- architecture governance
- quality governance
- resilience governance
- agent coordination

The work introduces two reusable project roles:

- Senior Workplan Architect
- Senior Requirement Engineer

## Target Outcome

After this workplan is executed, the repository contains:

```text
.agents/
+-- roles/
|   +-- senior-workplan-architect/
|   +-- senior-requirement-engineer/
|
+-- skills/
    +-- workplan-authoring/
    +-- requirement-engineering/
    +-- arc42-architecture-governance/
    +-- engineering-governance/
```

The governance system keeps these artifacts synchronized:

- EPIC
- arc42
- workplans
- requirements
- slices
- architecture

## Implementation Status

This workplan has been executed for the governance infrastructure slice. The repository now contains:

- reusable governance skills under `.agents/skills`
- Senior Workplan Architect and Senior Requirement Engineer role directories under `.agents/roles`
- matching Codex agent definitions under `.codex/agents`
- governance routing updates under `.agents/orchestrator`
- governance documentation under `docs/governance`
- skill-audit inventory updates for the new governance artifacts

No runtime business functionality, parser execution, Joern execution, graph runtime, replay runtime or LLM runtime was implemented.

## Core Governance Rules

### Rule 01 - Workplan Regeneration

When a new workplan is created, `docs/workplan` must be deleted completely first.

The new workplan must then fully regenerate `docs/workplan`. This prevents stale slices, obsolete workflows, conflicting plans, dead planning artifacts and historical leftovers from being treated as current instructions.

### Rule 02 - Requirement Drift Detection

The Requirement Engineer must continuously verify whether:

- architecture changed
- runtime behavior changed
- responsibilities moved
- service boundaries changed
- new resilience requirements appeared
- scalability assumptions changed
- UX requirements changed
- observability requirements changed

### Rule 03 - Synchronization Obligation

If architecture or requirements changed, these artifacts must be reviewed:

- EPIC
- arc42
- ADR references
- `QUALITY.md`
- `docs/workplan`
- related skills
- related roles

## Workplan Files

1. [00-verified-baseline.md](00-verified-baseline.md) - read-only repository findings and missing synchronization points.
2. [01-governance-target.md](01-governance-target.md) - target governance model and non-goals.
3. [02-workplan-lifecycle.md](02-workplan-lifecycle.md) - workplan generation, slice and stop rules.
4. [03-requirement-engineering-lifecycle.md](03-requirement-engineering-lifecycle.md) - EPIC and requirement governance.
5. [04-arc42-synchronization.md](04-arc42-synchronization.md) - arc42 and ADR synchronization rules.
6. [05-engineering-governance-umbrella.md](05-engineering-governance-umbrella.md) - cross-artifact governance checkpoints.
7. [06-role-and-skill-design.md](06-role-and-skill-design.md) - planned skills and roles.
8. [07-implementation-slices.md](07-implementation-slices.md) - ordered slices, dependencies and done criteria.
9. [08-validation-checklists.md](08-validation-checklists.md) - governance validation checklists.
10. [09-documentation-and-examples.md](09-documentation-and-examples.md) - documentation flow and examples.
11. [10-verification-and-quality-gates.md](10-verification-and-quality-gates.md) - local verification plan.
12. [11-commit-and-push-plan.md](11-commit-and-push-plan.md) - commit and push workflow for execution.

## Explicit Non-Goals

Do not implement runtime business functionality in this workplan.

The following are out of scope:

- runtime business logic
- parser execution
- Joern execution
- BTM execution
- graph runtime
- replay runtime
- LLM runtime
- Kubernetes deployment
- authentication
- authorization
- real-time streaming

This workplan creates governance infrastructure only.

## Expected Result

Future Codex work becomes structured, traceable, architecture-safe, requirement-aware, resilience-aware and governed.
