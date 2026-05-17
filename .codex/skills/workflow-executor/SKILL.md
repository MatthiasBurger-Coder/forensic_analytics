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
- checked project workflow file `docs/workflow/workflow.md`
- checked or updated project arc42 documentation under `docs/arc42/**`
- project-specific workflow, routing, role, or skill files under `.agents/`

## Workflow

1. Locate the active workflow only at `docs/workflow/workflow.md`.
2. Stop if the workflow is missing, unchecked or lacks an arc42 check status.
3. Load the checked or updated arc42 documentation.
4. Read the full workflow before implementation.
5. Identify slices, dependencies, write scopes, and verification commands.
6. Classify slices into backend, frontend, Docker/runtime and documentation strands.
7. Route each slice to the smallest suitable set of subagents or role reviews.
8. Execute one slice at a time.
9. Run required targeted checks and quality gates after each slice.
10. Inspect `git diff` and `git diff --check`.
11. Stage only files changed by the current slice.
12. Run `git diff --cached --check`.
13. Create a slice-scoped checkpoint commit.
14. Push the current workflow branch to `origin`.
15. Record the commit SHA and push result in the execution report.
16. Continue only after the checkpoint push succeeded.

Checkpoint pushes are normal branch pushes to the current workflow branch. They
are not `push auto`, do not merge pull requests, do not delete or clean up
branches, do not push to `main`, and must not force-push.

## Stop Conditions

Stop when `docs/workflow/workflow.md`, checked arc42 documentation, a slice, symbol, module, API, build task, schema, command, architecture rule, quality gate, service boundary, scope boundary, staged slice diff, checkpoint commit or checkpoint push cannot be verified exactly.
