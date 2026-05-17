# Skill Audit Prompt

Use for auditing skill compatibility before workflow authoring or execution.

## Required Inputs

- `AGENTS.md`
- `QUALITY.md`
- checked `docs/workflow/workflow.md` when workflow work is relevant
- `.agents/skills/**`
- `.agents/roles/**`
- `.agents/orchestrator/**`
- `.codex/skills/**`
- `.codex/agents/**`
- `docs/adr/**`
- `docs/agents/**`
- `docs/process/**`
- `docs/skill-audit/**`

## Required Output

- skill inventory
- missing skills
- overlapping responsibilities
- strand assignment gaps
- conflict classification
- required specialist reviews
- blockers

## Decision

Return `CONTINUE` only when no blocking skill or governance conflict remains.
