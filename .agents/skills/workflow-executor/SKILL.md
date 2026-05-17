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
3. Active workflow under `docs/workflow`.
4. `.agents/orchestrator/routing-rules.md`.
5. `.agents/orchestrator/swarm-orchestrator.md`.
6. Relevant `.agents/roles` files for the slice.
7. Relevant `.agents/skills` files for the slice.

## Active Workflow Discovery

Locate the active workflow in this order:

1. `docs/workflow/workflow.md`, when present.
2. The active workflow described by `docs/workflow/README.md`, when present.
3. The most recent `docs/workflow/*.md`.

Stop if multiple workflows appear active and the execution target cannot be verified.

## Start Conditions

`workflow execute` may start only when both are present and checked:

1. complete checked `docs/workflow/workflow.md`
2. checked or updated `docs/arc42/**` documentation

Stop when either artifact is missing or contradicts `AGENTS.md`, `QUALITY.md`, ADRs or verified repository state.

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

## Required Default Roles

Use at least these roles when relevant to the slice:

- Agent Workflow Orchestrator or Senior Swarm Orchestrator
- Senior System Architect
- Senior Java Backend Developer
- Senior Tester
- Senior DevOps Engineer
- Microservice Senior Expert

Route additional specialist concerns through `.agents/orchestrator/routing-rules.md`.

## Execution Strands

Keep these strands separate while executing workflow slices:

- Backend Strand
- Frontend Strand
- Docker / Runtime Strand
- Documentation Strand

Backend work routes through Senior Java Backend Developer, Microservice Senior Expert, `architecture-hexagonal`, `testing-junit6`, `spring-core` when Spring wiring is affected, and Senior DevOps with `devops-docker` when container readiness is affected.

Frontend work routes through Senior React Frontend Developer, Senior UX Designer, and Senior DevOps with `devops-docker` when container readiness is affected.

## Execution Protocol

For each slice:

1. Understand scope, prerequisites, dependencies, and allowed write scope.
2. Route the slice to the suitable subagent or role for implementation or review.
3. Apply only the changes authorized by the slice.
4. Run targeted tests first.
5. Run the required quality checks from `QUALITY.md` or the workflow.
6. Inspect `git diff` and `git diff --check`.
7. Document the result in the workflow quality log or the workflow-designated location.
8. When the slice quality gate passed, stage only files changed by the current slice.
9. Run `git diff --cached --check`.
10. Create the slice-scoped checkpoint commit.
11. Push the current workflow branch to `origin`.
12. Record the commit SHA and push result in the execution report.
13. Continue with the next slice only when the current slice is clean, the checkpoint push succeeded, or the workflow explicitly permits carrying a documented blocker without a commit.

Slice checkpoint push is not `push auto`. It must not create or merge a PR, run branch cleanup, force-push or push to `main`.

Use one write-capable implementation worker at a time unless the active workflow explicitly defines disjoint write scopes and the orchestrator confirms that parallel edits are safe.

## Stop Conditions

Stop and report if:

- architecture is unclear
- a class, module, API, Gradle task, schema, or command assumption is uncertain
- tests fail and cannot be fixed safely inside the slice
- the workflow conflicts with `AGENTS.md` or `QUALITY.md`
- multiple active workflows conflict
- checked `docs/workflow/workflow.md` is missing
- checked or updated arc42 documentation is missing
- the workflow branch is missing, inactive, or cannot be verified as a local ref
- a change would introduce shared Java code modules between microservices
- subagent or role execution is required but unavailable
- commit or push is requested but not explicitly allowed by the workflow
- checkpoint push would include files outside the current slice
- checkpoint push would push to `main`, create or merge a PR, run `push auto`, run branch cleanup or force-push
