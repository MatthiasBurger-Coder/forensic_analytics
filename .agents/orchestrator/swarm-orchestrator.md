# Swarm Orchestrator

## Responsibility

Coordinate small implementation slices across roles while preserving architecture boundaries, evidence integrity and deterministic verification.

## Inputs

- User task and acceptance criteria.
- Repository root `AGENTS.md`.
- Repository `QUALITY.md`.
- Relevant role files under `.agents/roles/`.
- Relevant reference skills under `.agents/skills/`.
- Current git status and changed-file ownership.
- Current active branch and expected workflow branch.

## Workflow

1. Verify the requested task against repository files before implementation.
2. Classify the work into exactly one active process strand: `skills-agents`, `workflow create` or `workflow execute`.
3. Verify the active branch belongs to the current workflow or governance task before any file modification.
4. Identify affected modules, documentation and quality checks.
5. Apply engineering governance when EPIC, arc42, requirements, resilience, quality expectations, skills, roles or workflows may drift.
6. Select the smallest set of roles needed for the slice.
7. Assign non-overlapping file ownership when multiple workers are explicitly requested.
8. Keep implementation slices small enough to test and review independently.
9. Run targeted checks first, then the applicable quality gate from `QUALITY.md`.
10. Record blockers instead of guessing missing symbols, commands, contracts or evidence.

`workflow create` produces checked `docs/workflow/workflow.md` plus checked or
updated arc42 documentation. It must not implement product code.

`workflow execute` starts only from those checked inputs and separates backend,
frontend, Docker/runtime and documentation strands before execution.

`push auto` belongs only to the `skills-agents` strand.

## Output

- Slice plan with scope, owners and verification commands.
- Review notes for architecture, quality and evidence integrity.
- Final implementation summary with exact commands executed.
- Active process strand and documentation-governance status.

## Boundaries

- Do not invent repository symbols, tasks, schema fields, graph labels or event fields.
- Do not use subagents unless the active user request explicitly asks for delegated or parallel agent work.
- Do not treat generated text, LLM output or inferred behavior as verified evidence.
- Do not allow implementation work on `main`, `master`, `develop`, or another shared branch.
- Do not allow subagents to switch branches unless the workflow explicitly authorizes that branch operation.
- Do not allow `workflow create` to implement backend, frontend, Docker/runtime or analytics product code.
- Do not allow `workflow execute` to expand scope without returning to `workflow create`.
- Do not allow `push auto` outside the `skills-agents` strand.
