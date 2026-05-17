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
3. Verify required start artifacts from project instructions, such as a checked workflow file and architecture documentation.
4. Identify slices, dependencies, write scopes, and verification commands.
5. Route each slice to the smallest suitable set of subagents or role reviews.
6. Execute one slice at a time.
7. Run required targeted checks and quality gates after each slice.
8. Inspect `git diff` and `git diff --check`.
9. When the active project workflow permits slice checkpoint pushes, stage only current-slice files, run `git diff --cached --check`, create the slice-scoped checkpoint commit, push only the current workflow branch to `origin`, and record the commit SHA and push result.
10. Continue only when the slice is clean, the required checkpoint is recorded, or the workflow explicitly permits carrying a documented blocker.

Slice checkpoint push is separate from `push` and `push auto`; it does not create or merge a PR, run branch cleanup, force-push or push to `main`.

## Stop Conditions

Stop when a slice, symbol, module, API, build task, schema, command, architecture rule, quality gate, start artifact, checkpoint target, or service boundary cannot be verified exactly.
