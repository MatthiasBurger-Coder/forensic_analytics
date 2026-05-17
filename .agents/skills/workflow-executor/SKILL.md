---
name: workflow-executor
description: Use when the user writes workflow execute or asks to execute the active project workflow through the configured subagent workflow with slice sequencing, specialist reviews, quality gates, git diff review, and commit restrictions.
---

# Workflow Executor Skill

## Purpose

Execute repository workflows through the configured subagent-based workflow while preserving `AGENTS.md`, `QUALITY.md`, architecture boundaries, evidence integrity, and commit restrictions.

## Trigger

Use this skill whenever the user writes:

```text
workflow execute
```

or otherwise asks Codex to execute the active workflow through the configured subagent workflow.

## Required Inputs

Read these files before implementation:

1. Root `AGENTS.md`.
2. Root `QUALITY.md`.
3. Checked `docs/workflow/workflow.md`.
4. Checked or updated `docs/arc42/**` documentation.
5. `.agents/orchestrator/routing-rules.md`.
6. `.agents/orchestrator/swarm-orchestrator.md`.
7. Relevant `.agents/roles` files for the slice.
8. Relevant `.agents/skills` files for the slice.

## Active Workflow Discovery

Locate the active workflow only at:

```text
docs/workflow/workflow.md
```

Stop if `docs/workflow/workflow.md` is missing, unchecked, or does not record a checked arc42 review or update. Do not fall back to `docs/workflow/README.md` or the newest `docs/workflow/*.md`.

## Branch Verification

Before implementation, verify the workflow branch from the active workflow with:

```bash
git branch --show-current
git show-ref --verify --quiet refs/heads/<workflow-branch>
git status --short --branch
```

Continue only when the active branch matches the workflow branch and the local
branch ref exists. If the branch is missing or inactive, stop before file
changes. Create or restore the branch only when the user explicitly approves
that remediation, then rerun the active-branch and local-ref checks before
continuing.

Do not rely on workflow notes or execution summaries as proof that the branch
exists.

## Core Rule

Never implement a workflow slice directly before the relevant subagent or role has reviewed the slice.

The `workflow execute` command authorizes the configured subagent workflow for that workflow only. Keep unrelated tasks under the normal repository subagent authorization rules.

`workflow execute` may not expand scope. If the requested work is outside the checked `docs/workflow/workflow.md`, stop and return to `workflow create`.

## Required Default Roles

Use at least these roles when relevant to the slice:

- Agent Workflow Orchestrator or Senior Swarm Orchestrator
- Senior System Architect
- Senior Java Backend Developer
- Senior Tester
- Senior DevOps Engineer
- Microservice Senior Expert

Route additional specialist concerns through `.agents/orchestrator/routing-rules.md`.

Backend slices require Senior Java Backend Developer, Microservice Senior Expert
when service boundaries are affected, `architecture-hexagonal`, `spring-core`
when Spring wiring is affected, `testing-junit6`, and Senior DevOps with
`devops-docker` when container readiness is affected.

Frontend slices require Senior React Frontend Developer, Senior UX Designer, and
Senior DevOps with `devops-docker` when container readiness is affected.

Documentation slices must update the execution report, arc42 consistency notes,
testing documentation and deviations from `docs/workflow/workflow.md`.

## Execution Protocol

For each slice:

1. Understand scope, prerequisites, dependencies, and allowed write scope.
2. Classify the slice as backend, frontend, Docker/runtime, documentation or a documented combination with separated write scopes.
3. Route the slice to the suitable subagent or role for implementation or review.
4. Apply only the changes authorized by the slice.
5. Run targeted tests first.
6. Run the required quality checks from `QUALITY.md` or the workflow.
7. Inspect `git diff` and `git diff --check`.
8. Document the result in the workflow quality log or the workflow-designated location.
9. Continue with the next slice only when the current slice is clean or the workflow explicitly permits carrying a documented blocker.

Use one write-capable implementation worker at a time unless the active workflow explicitly defines disjoint write scopes and the orchestrator confirms that parallel edits are safe.

## Stop Conditions

Stop and report if:

- architecture is unclear
- `docs/workflow/workflow.md` is missing, unchecked or lacks arc42 check status
- checked arc42 documentation is missing
- a class, module, API, Gradle task, schema, or command assumption is uncertain
- tests fail and cannot be fixed safely inside the slice
- the workflow conflicts with `AGENTS.md` or `QUALITY.md`
- multiple active workflows conflict
- the workflow branch is missing, inactive, or cannot be verified as a local ref
- the requested work expands scope beyond checked `docs/workflow/workflow.md`
- a change would introduce shared Java code modules between microservices
- subagent or role execution is required but unavailable
- commit or push is requested but not explicitly allowed by the workflow
