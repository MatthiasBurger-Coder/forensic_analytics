# Governance Target

## Purpose

The governance system is a reusable Codex guidance layer for planning and executing Forensic Analytics work. It does not replace `AGENTS.md` or `QUALITY.md`; it applies them to workplan generation, requirement management and architecture synchronization.

## Target Artifacts

Create these skills:

```text
.agents/skills/workplan-authoring/SKILL.md
.agents/skills/requirement-engineering/SKILL.md
.agents/skills/arc42-architecture-governance/SKILL.md
.agents/skills/engineering-governance/SKILL.md
```

Create these roles:

```text
.agents/roles/senior-workplan-architect/SKILL.md
.agents/roles/senior-requirement-engineer/SKILL.md
```

The role directories are intentional because the requested target outcome names them as reusable project roles with `SKILL.md` files. Existing flat role Markdown files remain unchanged unless a later alignment slice explicitly verifies a safe reference update.

## Governance Flow

The intended flow is:

```text
EPIC and requirements
  -> requirement-engineering
  -> arc42-architecture-governance
  -> workplan-authoring
  -> engineering-governance checkpoints
  -> implementation slices
  -> verification and documentation synchronization
```

## Artifact Responsibilities

`workplan-authoring` owns:

- workplan structure
- slice structure
- dependency graph
- subagent assignment rules
- workplan regeneration rules
- stop conditions
- verification planning

`requirement-engineering` owns:

- EPIC lifecycle
- requirement drift detection
- requirement classification
- traceability
- assumptions and constraints
- architecture impact analysis

`arc42-architecture-governance` owns:

- arc42 update triggers
- architecture decision propagation
- service boundary documentation
- runtime, deployment and resilience documentation
- ADR reference review

`engineering-governance` owns:

- EPIC to arc42 to workplan synchronization
- quality synchronization
- resilience synchronization
- architecture consistency checks
- documentation consistency checks
- governance checkpoint checklists

## Architecture Constraints

The governance system is documentation and agent guidance. It must not introduce runtime dependencies, Gradle plugins, framework code, parser execution, graph execution, replay execution or LLM provider integration.

The governance system must preserve the root architecture direction:

```text
adapters / infrastructure / plugins / UI / CLI
        -> application
        -> domain
```

## Non-Goals

Do not change production code.

Do not add new runtime modules.

Do not change Gradle configuration unless execution discovers that documentation-only validation is impossible without a documented build update.

Do not create compatibility aliases, fallback role names or hidden governance shortcuts.
