---
name: workplan-executor
description: Use when the user writes workplan execute or asks to execute the active Forensic Analytics workplan through the configured subagent workflow with slice sequencing, specialist reviews, quality gates, git diff review, and commit restrictions.
---

# Workplan Executor Skill

## Purpose

Execute repository workplans through the configured subagent-based workflow while preserving `AGENTS.md`, `QUALITY.md`, architecture boundaries, evidence integrity, and commit restrictions.

## Trigger

Use this skill whenever the user writes:

```text
workplan execute
```

or otherwise asks Codex to execute the active workplan through the configured subagent workflow.

## Required Inputs

Read these files before implementation:

1. Root `AGENTS.md`.
2. Root `QUALITY.md`.
3. Active workplan under `docs/workplan`.
4. `.agents/orchestrator/routing-rules.md`.
5. `.agents/orchestrator/swarm-orchestrator.md`.
6. Relevant `.agents/roles` files for the slice.
7. Relevant `.agents/skills` files for the slice.

## Active Workplan Discovery

Locate the active workplan in this order:

1. `docs/workplan/workflow.md`, when present.
2. The active workplan described by `docs/workplan/README.md`, when present.
3. The most recent `docs/workplan/*.md`.

Stop if multiple workplans appear active and the execution target cannot be verified.

## Core Rule

Never implement a workplan slice directly before the relevant subagent or role has reviewed the slice.

The `workplan execute` command authorizes the configured subagent workflow for that workplan only. Keep unrelated tasks under the normal repository subagent authorization rules.

## Required Default Roles

Use at least these roles when relevant to the slice:

- Agent Workflow Orchestrator or Senior Swarm Orchestrator
- Senior System Architect
- Senior Java Backend Developer
- Senior Tester
- Senior DevOps Engineer
- Microservice Senior Expert

Route additional specialist concerns through `.agents/orchestrator/routing-rules.md`.

## Execution Protocol

For each slice:

1. Understand scope, prerequisites, dependencies, and allowed write scope.
2. Route the slice to the suitable subagent or role for implementation or review.
3. Apply only the changes authorized by the slice.
4. Run targeted tests first.
5. Run the required quality checks from `QUALITY.md` or the workplan.
6. Inspect `git diff` and `git diff --check`.
7. Document the result in the workplan quality log or the workplan-designated location.
8. Continue with the next slice only when the current slice is clean or the workplan explicitly permits carrying a documented blocker.

Use one write-capable implementation worker at a time unless the active workplan explicitly defines disjoint write scopes and the orchestrator confirms that parallel edits are safe.

## Stop Conditions

Stop and report if:

- architecture is unclear
- a class, module, API, Gradle task, schema, or command assumption is uncertain
- tests fail and cannot be fixed safely inside the slice
- the workplan conflicts with `AGENTS.md` or `QUALITY.md`
- multiple active workplans conflict
- a change would introduce shared Java code modules between microservices
- subagent or role execution is required but unavailable
- commit or push is requested but not explicitly allowed by the workplan
