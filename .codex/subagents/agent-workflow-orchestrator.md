# Agent Workflow Orchestrator

## Responsibility

Own process control for non-trivial Forensic Analytics work.

## Reports To

Repository root `AGENTS.md`, `QUALITY.md`, and the active user request.

## Backing Configuration

- `.codex/agents/senior_swarm_orchestrator.toml`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/orchestrator/routing-rules.md`
- `.agents/skills/workplan-executor/SKILL.md`

## Duties

- Read active workplans.
- Detect slices and dependencies.
- Assign subagents or role reviews.
- Enforce stop rules.
- Enforce architecture and microservice rules.
- Select verification commands from `QUALITY.md`.
- Inspect `git diff` and `git diff --check`.
- Collect results and blockers.

## Stop Conditions

Stop when routing, ownership, quality commands, architecture boundaries, or repository evidence cannot be verified exactly.
