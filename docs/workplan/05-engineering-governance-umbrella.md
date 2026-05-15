# Engineering Governance Umbrella

## Purpose

The engineering-governance skill coordinates workplan, requirement, architecture, resilience and quality governance.

## Synchronization Model

Keep these artifacts aligned:

```text
EPIC
  <-> requirements
  <-> arc42
  <-> ADR references
  <-> docs/workplan
  <-> skills
  <-> roles
  <-> QUALITY.md
```

`AGENTS.md` and `QUALITY.md` remain authoritative for repository rules and quality commands. Governance skills must reference them instead of duplicating competing rules.

## Governance Checkpoints

Run governance checks:

- before creating a new workplan
- before changing architecture documentation
- before moving responsibilities across service boundaries
- before adding resilience behavior
- before changing quality-gate expectations
- before aligning existing skills or roles
- before commit readiness review

## Architecture Consistency

Governance rules must require:

- no silent architecture drift
- no undocumented service boundary changes
- no undocumented resilience changes
- no undocumented deployment changes
- no stale EPIC assumptions
- no stale workplan slices
- no hidden compatibility governance

## Quality Synchronization

If verification expectations change, review:

- `QUALITY.md`
- Gradle tasks
- CI workflow files when present
- workplan verification sections
- related quality skills
- commit-preparation guidance

Do not invent quality commands or task names.

## Resilience Synchronization

When a new resilience requirement appears, review:

- EPIC
- arc42 crosscutting concepts
- arc42 runtime view
- arc42 deployment view
- affected ADR references
- related resilience skill guidance
- workplan slice verification
- quality scenarios where practical

## Documentation Consistency

Documentation must identify whether behavior is planned, implemented, deprecated or unresolved.

Do not present generated plans, future architecture or LLM-generated hypotheses as verified implementation facts.
