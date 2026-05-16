---
name: workflow-authoring
description: Use for creating or regenerating the current project workflows with verified baselines, executable slices, dependency ordering, subagent ownership, architecture constraints, resilience requirements, quality gates, stop conditions, and full docs/workflow lifecycle control.
---

# Skill: Workflow Authoring

## Purpose

Create executable, repository-specific workflows that preserve `AGENTS.md`, `QUALITY.md`, architecture governance, requirement traceability and evidence integrity.

This skill governs workflow creation. It does not implement runtime business functionality.

## Required Inputs

Read before authoring or regenerating a workflow:

1. User request.
2. Root `AGENTS.md`.
3. Root `QUALITY.md`.
4. Existing `docs/workflow` if present.
5. Relevant EPIC files under `docs/epics`.
6. Relevant `docs/arc42` and `docs/adr` files.
7. Relevant `.agents/skills` and `.agents/roles` files.
8. Build or CI files only when quality-gate behavior is affected.

## Workflow Regeneration Rule

Before creating a new workflow:

1. Verify the repository root and the absolute target path.
2. Delete `docs/workflow` completely, unless the user explicitly asks to preserve an existing workflow.
3. Recreate `docs/workflow`.
4. Regenerate the full workflow structure.

Do not partially overwrite old slices. Do not keep stale workflow files unless the user explicitly asks to archive them outside the active workflow.

## Workflow Structure

Every workflow should include:

- verified baseline
- target outcome
- explicit non-goals
- architecture constraints
- resilience requirements
- ordered slices
- slice dependency graph
- parallelization opportunities
- role or subagent ownership map
- quality-gate expectations from `QUALITY.md`
- documentation synchronization points
- stop conditions
- uncertainty escalation rules
- commit and push plan when requested

## Slice Rules

Use stable two-digit slice numbers:

```text
Slice 01
Slice 02
Slice 03
```

For each slice define:

- purpose
- prerequisites
- affected files
- owner role
- allowed write scope
- dependencies
- parallelization status
- done criteria
- verification commands
- stop conditions

Parallelize only when write scopes are disjoint, shared contracts are stable and verification can be run independently.

## Subagent Assignment

Assign roles by verified responsibility:

- workflow creation and dependency ordering: Senior Workflow Architect
- requirement and EPIC drift: Senior Requirement Engineer
- architecture boundaries and arc42: Senior System Architect or arc42 governance
- documentation consistency: Senior Documentation Engineer
- quality verification: Senior Tester or quality-gate skills
- branch, commit and push readiness: git commit preparation skills

Use subagents only when the user explicitly asks for delegated or parallel agent work.

## Quality Gates

Read `QUALITY.md` before documenting quality commands.

Do not invent Gradle task names, CI jobs or quality scripts. If a command cannot be verified, stop and report.

## Stop Conditions

Stop and report if:

- `docs/workflow` cannot be safely deleted and regenerated
- architecture conflicts are unclear
- EPIC contradicts implementation and the source of truth is unclear
- multiple active workflows conflict
- service ownership is ambiguous
- resilience expectations are unclear
- quality-gate authority is unclear
- planned file paths cannot be verified
- continuing would require guessing governance decisions

## Expected Outputs

- fully regenerated `docs/workflow`
- ordered slice plan
- dependency graph or dependency summary
- role ownership map
- verification plan
- documented assumptions and unresolved conflicts
