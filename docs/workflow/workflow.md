# Workflow: Three-Strand Agent Workflow Governance

## Executive Summary

This workflow reconstructs the deleted branch `architecture/workflow-align-agent-workflow-strands-20260517` from `main` and restores repository governance for three process strands:

- `skills-agents`
- `workflow create`
- `workflow execute`

It adds the explicit `skills update` command, restores `workflow create` requirement clarification, restores `workflow execute` slice checkpoint semantics and restricts `push auto` to `skills-agents`.

## Target Picture

Repository governance is explicit and non-overlapping:

- `skills update` updates skills, agents, roles, prompts, Codex definitions, routing rules, organigramm, skill registry and process documentation.
- `workflow create` sharpens requirements, validates architecture impact, creates or updates `docs/workflow/workflow.md`, checks or updates arc42 and releases the workflow for execution.
- `workflow execute` executes a checked workflow slice by slice, runs required quality gates and checkpoint-pushes each successful slice to the workflow branch.

## Scope

Allowed changes:

- `AGENTS.md`
- `.agents/**`
- `.codex/**`
- `docs/agents/**`
- `docs/process/**`
- `docs/workflow/**`
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/adr/**`
- `docs/governance/**`
- `docs/skill-audit/**`

## Non-Goals

This workflow does not change product implementation.

Forbidden changes:

- `src/**`
- `services/**`
- `contracts/**`
- `docker/**`
- `build.gradle*`
- `settings.gradle*`
- `gradle/**`
- `proto/**`
- `forensic-ui/**`

## Architecture Boundaries

The workflow changes agent and documentation governance only. It does not alter runtime architecture, service boundaries, data ownership, persistence behavior, API contracts, frontend behavior, Docker runtime or analytics behavior.

Planned behavior is not implemented behavior.

## Backend Assessment

No backend implementation changes are in scope. Backend roles are referenced only as governance participants for `workflow create` and `workflow execute`.

## Frontend Assessment

No frontend implementation changes are in scope. Frontend roles are referenced only as governance participants for `workflow create` and `workflow execute`.

## Test Strategy

This branch is documentation and agent-governance only. Required verification:

- `git diff --check`
- `git diff --cached --check` before each checkpoint commit
- final changed-file scope check against forbidden paths
- final traceability search for required governance terms

The Gradle test command may be run when practical:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

If not run, the final report must state why it was skipped.

## Slice Structure

| Slice | Purpose | Checkpoint commit |
|---|---|---|
| 01 | Define process strands and `skills update` in `AGENTS.md` | `docs(agents): define process strands and skills update command` |
| 02 | Restore `docs/process` skills update and push auto governance | `docs(process): restore skills update and push auto governance` |
| 03 | Restore workflow-create requirement clarification gate | `docs(workflow): restore requirement clarification gate` |
| 04 | Restore workflow-execute slice checkpoint execution | `docs(workflow): restore slice checkpoint execution` |
| 05 | Restore checkpoint push governance in Git rules | `docs(git): restore checkpoint push governance` |
| 06 | Restore organigramm and skill registry | `docs(agents): restore organigramm and skill registry` |
| 07 | Restore arc42 and ADR governance records | `docs(arc42): restore governance architecture records` |
| 08 | Restore active workflow checkpoint semantics | `docs(workflow): restore active workflow checkpoint semantics` |
| 09 | Restore Codex command prompts and operators | `agent(codex): restore command prompts and operators` |
| 10 | Validate reconstructed governance branch | Optional, only if validation updates documentation |

## Subagent Assignment

- Agent Workflow Orchestrator / Senior Swarm Orchestrator: slice routing and strand separation
- Senior System Architect: governance authority and architecture consistency
- Senior Documentation Engineer: documentation consistency
- Skill Registry Conflict Auditor: skills-agents ownership and duplicate review
- Senior Workflow Architect: workflow create and workflow execute semantics
- Git Commit Reviewer / Git Commit Operator: checkpoint and publication-mode governance

## Quality Gates

Per slice:

```bash
git diff --check
git diff --cached --check
git push origin HEAD:architecture/workflow-align-agent-workflow-strands-20260517
```

Final:

```bash
git status --short --branch
git diff main...HEAD --name-status
git diff --check main...HEAD
rg -n "skills update|skills-agents|workflow create|workflow execute|Requirement Clarification|Blocking Questions|READY_FOR_WORKFLOW|PROCEED_WITH_ACCEPTED_ASSUMPTIONS|REQUIRES_REFINEMENT|Senior Java Backend Developer|Senior React Frontend Developer|Senior Tester|Slice checkpoint|checkpoint push|push auto|arc42" AGENTS.md .agents .codex docs
git diff --name-only main...HEAD | rg "^(src/|services/|contracts/|docker/|gradle/|proto/|forensic-ui/|build.gradle|settings.gradle)"
```

## Definition of Done

- Branch `architecture/workflow-align-agent-workflow-strands-20260517` exists and is active.
- `skills update`, `workflow create` and `workflow execute` are documented as separate process strands.
- `push auto` is restricted to `skills-agents`.
- Slice checkpoint push is documented as separate from `push auto`.
- `workflow create` includes Requirement Clarification Loop and five mandatory Three Amigos roles.
- `workflow create` ends with checked workflow.md and checked or updated arc42.
- `workflow execute` starts only with checked workflow.md and checked or updated arc42.
- No product implementation files changed.

## Handoff to workflow execute

Workflow creation itself does not commit or push product implementation changes. During workflow execute, each successfully completed slice must create a slice-scoped checkpoint commit and push the current workflow branch to origin after the slice quality gate passes.

## arc42 Check Status

arc42 was checked and updated for governance consequences only. No runtime system boundary, deployment topology, product service contract or implementation behavior changed.
