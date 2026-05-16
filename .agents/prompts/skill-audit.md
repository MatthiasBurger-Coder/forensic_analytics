# Skill Audit Prompt

Use for auditing skill compatibility before workflow authoring or execution.

## Required Inputs

- `AGENTS.md`
- `QUALITY.md`
- active `docs/workflow/**`
- `.agents/skills/**`
- `.agents/roles/**`
- `.agents/orchestrator/**`
- `.codex/skills/**`
- `.codex/agents/**`
- `docs/adr/**`
- `docs/skill-audit/**`

## Required Output

- skill inventory
- missing skills
- overlapping responsibilities
- conflict classification
- required specialist reviews
- blockers

## Decision

Return `CONTINUE` only when no blocking skill or governance conflict remains.
