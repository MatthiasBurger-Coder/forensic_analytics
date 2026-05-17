---
name: workflow-authoring
description: Use for creating or sharpening the current project `workflow create` output with verified baselines, executable slices, dependency ordering, subagent ownership, architecture constraints, resilience requirements, quality gates, stop conditions, checked `docs/workflow/workflow.md`, and checked or updated arc42 documentation.
---

# Skill: Workflow Authoring

## Purpose

Create executable, repository-specific workflows that preserve `AGENTS.md`, `QUALITY.md`, architecture governance, requirement traceability and evidence integrity.

This skill governs the `workflow create` strand. It does not implement runtime business functionality, backend features, frontend features, Docker/runtime code, analytics engine code or product behavior.

`workflow create` has exactly two checked end artifacts:

1. `docs/workflow/workflow.md`
2. checked or updated `docs/arc42/**` documentation

## Mandatory Branch-First Rule

Every workflow creation must start by ensuring a dedicated Git branch for that workflow exists and is active.

For a new workflow this means creating and checking out a workflow branch before mutating workflow planning artifacts. If the current branch already exactly matches the current workflow, verify it before continuing.

Read-only verification, requirement intake, routing-rule inspection, and role selection may occur before branch creation. Mutating workflow creation must not.

This rule applies before any workflow planning artifact is created or modified, including `docs/workflow/workflow.md`, arc42 workflow-impact sections, workflow-specific planning notes, slice definitions or write-capable agent assignments.

Required order:

1. Verify the Git repository context.
2. Check the working tree status.
3. Stop if the current branch is detached, unclear, or if unrelated or unclear uncommitted changes exist.
4. Create a dedicated workflow branch, unless the current branch already matches the current workflow.
5. Check local and remote branch-name collisions, choosing the next clear unique suffix when needed.
6. Checkout the workflow branch, or verify the existing matching workflow branch.
7. Verify that the local branch ref exists.
8. Verify the active branch.
9. Run the Three Amigos Requirement Gate before authoring executable slices.
10. Create or sharpen `docs/workflow/workflow.md` only after successful branch verification.
11. Check and update arc42 documentation when affected.
12. Validate both checked outputs before releasing the workflow for `workflow execute`.

Default branch naming:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
fix/workflow-<short-topic>-<yyyyMMdd>
docs/workflow-<short-topic>-<yyyyMMdd>
architecture/workflow-<short-topic>-<yyyyMMdd>
```

Never create or modify workflow planning artifacts on `main`, `master`, `develop`, or any shared branch. If branch creation, checkout or verification fails, stop and report the reason instead of continuing in the current branch.

Branch verification after creation or checkout must include both:

```bash
git show-ref --verify --quiet refs/heads/<workflow-branch>
git branch --show-current
```

Do not rely on generated workflow notes as proof that a branch exists.

## Required Inputs

Read before authoring or regenerating a workflow:

1. User request.
2. Root `AGENTS.md`.
3. Root `QUALITY.md`.
4. Existing `docs/workflow/workflow.md` if present.
5. Relevant EPIC files under `docs/epics`.
6. Relevant `docs/arc42` and `docs/adr` files.
7. Relevant `.agents/skills` and `.agents/roles` files.
8. Build or CI files only when quality-gate behavior is affected.

## Workflow Output Rule

Before creating or sharpening a workflow:

1. Verify the repository root and the absolute target path.
2. Verify that the dedicated workflow branch exists and is active.
3. Create or update `docs/workflow/workflow.md` as the checked workflow artifact.
4. Check every relevant arc42 section and update `docs/arc42/**` when affected.
5. Record the arc42 check status in `docs/workflow/workflow.md`.

Do not delete historical or sidecar workflow files unless the task explicitly asks to archive or migrate them. Supporting sidecars are not the completion criteria for `workflow create`; executable scope must be consolidated into `docs/workflow/workflow.md`.

## Workflow Structure

Every workflow should include:

- Executive Summary
- Target Picture
- Scope
- Non-Goals
- Architecture Boundaries
- Backend Assessment
- Frontend Assessment
- Test Strategy
- Slice Structure
- Subagent Assignment
- Quality Gates
- Definition of Done
- Handoff to `workflow execute`
- arc42 Check Status

The arc42 Check Status must record inspected sections, updated sections or `no update required`, reviewer or role, date, branch and unresolved drift.

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
- backend impact assessment: Senior Java Backend Developer
- frontend impact assessment: Senior React Frontend Developer
- documentation consistency: Senior Documentation Engineer
- quality verification: Senior Tester or quality-gate skills
- branch, commit and push readiness: git commit preparation skills

Use subagents only when the user explicitly asks for delegated or parallel agent work.

Subagents must verify that the active branch belongs to the current workflow before modifying files. They must not switch branches or implement on `main`, `master`, `develop`, or any shared branch unless the workflow explicitly authorizes the branch operation.

## Quality Gates

Read `QUALITY.md` before documenting quality commands.

Do not invent Gradle task names, CI jobs or quality scripts. If a command cannot be verified, stop and report.

## Stop Conditions

Stop and report if:

- the Git repository context cannot be verified
- the current branch is detached or unclear
- unrelated or unclear uncommitted changes exist before workflow branch creation
- the branch name collides with an existing local or remote branch and no clear unique suffix can be chosen
- the dedicated workflow branch cannot be created, checked out, verified as a local ref, or verified as active
- the active branch is `main`, `master`, `develop`, or another shared branch when workflow files would be created
- `docs/workflow/workflow.md` cannot be completed
- arc42 cannot be checked or updated from verified evidence
- authoring would require backend, frontend, Docker/runtime or analytics implementation
- architecture conflicts are unclear
- EPIC contradicts implementation and the source of truth is unclear
- multiple active workflows conflict
- service ownership is ambiguous
- resilience expectations are unclear
- quality-gate authority is unclear
- planned file paths cannot be verified
- continuing would require guessing governance decisions

## Expected Outputs

- checked `docs/workflow/workflow.md`
- checked or updated `docs/arc42/**`
- ordered slice plan inside `docs/workflow/workflow.md`
- dependency graph or dependency summary inside `docs/workflow/workflow.md`
- role ownership map inside `docs/workflow/workflow.md`
- verification plan inside `docs/workflow/workflow.md`
- documented assumptions and unresolved conflicts
