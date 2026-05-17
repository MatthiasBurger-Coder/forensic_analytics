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
2. Verify the active branch belongs to the current workflow before any file modification.
3. Identify affected modules, documentation and quality checks.
4. Apply engineering governance when EPIC, arc42, requirements, resilience, quality expectations or workflows may drift.
5. Select the smallest set of roles needed for the slice.
6. Assign non-overlapping file ownership when multiple workers are explicitly requested.
7. Keep implementation slices small enough to test and review independently.
8. Run targeted checks first, then the applicable quality gate from `QUALITY.md`.
9. Record blockers instead of guessing missing symbols, commands, contracts or evidence.

## Process Strand Routing

- Exact `skills update` activates the `skills-agents` strand and routes to Skill Registry Conflict Auditor, Senior Documentation Engineer, Organigramm Maintainer and Process Governance Maintainer.
- Exact `workflow create` activates the `workflow create` strand and routes through requirement clarification, five-role Three Amigos review, branch governance, workflow authoring and arc42 validation.
- Exact `workflow execute` activates the `workflow execute` strand and routes through the workflow executor, slice role reviews, quality gates and slice checkpoint push.

The strands must not be mixed. Slice checkpoint push is not `push auto`, and `push auto` belongs only to `skills-agents`.

## Output

- Slice plan with scope, owners and verification commands.
- Review notes for architecture, quality and evidence integrity.
- Final implementation summary with exact commands executed.

## Boundaries

- Do not invent repository symbols, tasks, schema fields, graph labels or event fields.
- Do not use subagents unless the active user request explicitly asks for delegated or parallel agent work.
- Do not treat generated text, LLM output or inferred behavior as verified evidence.
- Do not allow implementation work on `main`, `master`, `develop`, or another shared branch.
- Do not allow subagents to switch branches unless the workflow explicitly authorizes that branch operation.
