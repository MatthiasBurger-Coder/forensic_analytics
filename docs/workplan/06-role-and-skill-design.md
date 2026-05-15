# Role And Skill Design

## Skill: workplan-authoring

Create:

```text
.agents/skills/workplan-authoring/SKILL.md
```

The skill must define:

- how to create workplans
- how to structure slices
- how to assign subagents
- how to define dependencies
- how to define quality gates
- how to define non-goals
- how to define architecture constraints
- how to define resilience requirements
- how to define verification steps
- how to delete and fully regenerate `docs/workplan`
- how to handle uncertainty and stop conditions

## Skill: requirement-engineering

Create:

```text
.agents/skills/requirement-engineering/SKILL.md
```

The skill must define:

- EPIC lifecycle
- requirement drift detection
- functional versus non-functional requirements
- architecture impact analysis
- requirement traceability
- requirement classification
- constraint management
- assumption tracking
- continuous comparison between implementation and EPIC assumptions

## Skill: arc42-architecture-governance

Create:

```text
.agents/skills/arc42-architecture-governance/SKILL.md
```

The skill must define:

- when arc42 must be updated
- how architecture decisions propagate
- how runtime changes affect documentation
- how deployment views evolve
- how service boundaries are documented
- how resilience decisions are documented
- how ADR references are reviewed

## Skill: engineering-governance

Create:

```text
.agents/skills/engineering-governance/SKILL.md
```

This umbrella skill must define:

- EPIC to arc42 to workplan synchronization
- governance checkpoints
- quality synchronization
- resilience synchronization
- architecture consistency checks
- documentation consistency checks
- stop and report rules
- validation checklists

## Role: Senior Workplan Architect

Create:

```text
.agents/roles/senior-workplan-architect/SKILL.md
```

Responsibilities:

- create executable workplans
- split work into slices
- assign subagents
- detect planning risks
- define dependencies
- validate implementation order
- coordinate architecture-safe execution

The role must always delete `docs/workplan` before generating a new workplan unless explicitly instructed otherwise.

## Role: Senior Requirement Engineer

Create:

```text
.agents/roles/senior-requirement-engineer/SKILL.md
```

Responsibilities:

- maintain EPIC consistency
- detect requirement drift
- update EPIC when needed
- update arc42 when needed
- check architecture consistency
- track new constraints
- track new scalability requirements
- track new resilience requirements
- track new UX requirements

The role must ask internally:

```text
Does the implementation still match the EPIC?
```

## Frontmatter Requirement

Every new `SKILL.md` must include YAML frontmatter with `name` and `description`, matching `.agents/AGENTS.md`.

## Alignment Rule

Existing skills and roles may be updated only where the integration point is obvious and verified. Uncertain conflicts must be documented instead of rewritten.
