# Skill Landscape Expansion Prompt

Use this project prompt to expand the governed skill landscape.

## Governance Chain

```text
Requirement
  -> Three Amigos Requirement Gatekeeper
  -> Skill Registry & Conflict Auditor
  -> Workflow Authoring
  -> Workflow Executor
  -> Agent Handoff Protocol
  -> Quality Gate Orchestrator
  -> Release & Branch Governance
```

## Required Checks

- Root `AGENTS.md` remains authoritative.
- Root `QUALITY.md` remains authoritative for gates.
- Project-specific rules stay in `.agents` or project docs, not portable `.codex` files.
- New skills define Mission, Responsibilities, Authority, Forbidden, Inputs, Outputs, Collaboration Rules and STOP Rules.
- Workflow slices have one owner and explicit review roles.
- Handoffs include source, target, inputs, outputs, assumptions, risks, blockers, validation status and next action.
- Commit and push require clean required gates.

## Output

Produce or update:

- skill files
- prompt files
- conflict matrices
- deadlock rules
- ADRs when decisions are durable
- validation report
