---
name: workflow-executor
description: Reusable entrypoint for executing repository workflows when the user writes workflow execute; routes slices through subagents or role reviews, quality gates, diff inspection, stop conditions, and commit restrictions.
---

# Workflow Executor

Use this skill only when the user writes `workflow execute` or explicitly asks to execute the active repository workflow.

## Authoritative Sources

Read, when present:

- root `AGENTS.md`
- root `QUALITY.md`
- `.codex/workflow/workflow-execution-rules.md`
- project workflow files such as `docs/workflow/workflow.md`, `docs/workflow/README.md`, or the newest relevant `docs/workflow/*.md`
- project-specific workflow, routing, role, or skill files under `.agents/`

## Workflow

1. Locate the active workflow using project instructions when they exist.
2. Read the full workflow before implementation.
3. Identify slices, dependencies, write scopes, and verification commands.
4. Route each slice to the smallest suitable set of subagents or role reviews.
5. Execute one slice at a time.
6. Run required targeted checks and quality gates after each slice.
7. Inspect `git diff` and `git diff --check`.
8. Continue only when the slice is clean or the workflow explicitly permits carrying a documented blocker.

## Stop Conditions

Stop when a slice, symbol, module, API, build task, schema, command, architecture rule, quality gate, or service boundary cannot be verified exactly.
