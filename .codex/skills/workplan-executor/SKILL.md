---
name: workplan-executor
description: Codex-team entrypoint for executing Forensic Analytics workplans when the user writes workplan execute; routes slices through subagents or role reviews, quality gates, diff inspection, and commit restrictions.
---

# Workplan Executor

Use this skill only when the user writes `workplan execute` or explicitly asks to execute the active Forensic Analytics workplan.

## Authoritative Sources

- Root `AGENTS.md`
- Root `QUALITY.md`
- `.agents/skills/workplan-executor/SKILL.md`
- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.codex/workflow/workflow-execution-rules.md`

## Workflow

1. Locate the active workplan in the order defined by root `AGENTS.md`.
2. Read the full workplan before implementation.
3. Identify slices, dependencies, write scopes, and verification commands.
4. Route each slice to the smallest suitable set of subagents or role reviews.
5. Execute one slice at a time.
6. Run required targeted checks and quality gates after each slice.
7. Inspect `git diff` and `git diff --check`.
8. Continue only when the slice is clean or the workplan explicitly permits carrying a documented blocker.

## Stop Conditions

Stop when a slice, symbol, module, API, Gradle task, schema, command, architecture rule, quality gate, or service boundary cannot be verified exactly.
