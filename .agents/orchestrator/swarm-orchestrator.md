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
4. Classify the request through `skills/execution-profile-router/SKILL.md` as `FAST_PATH`, `GOVERNANCE_FAST_PATH`, `NORMAL_PATH` or `FULL_PATH`.
5. Apply the Senior Requirement Engineer role and engineering governance when EPIC, arc42, requirements, resilience, quality expectations or workflows may drift.
6. For `workflow execute`, run S3D orchestration: extract slice metadata, build the dependency graph, run topological sort and verify file, contract, module and architecture-boundary locks.
7. Select the smallest set of roles needed for the slice.
8. Apply `skills/source-code-responsibility/SKILL.md` when a slice creates source code, adds behavior to a large class, decomposes endpoints or persistence adapters, introduces Strategy Pattern, converts imperative flow to declarative design, or performs IF-less responsibility cleanup.
9. Assign non-overlapping file ownership when multiple workers are explicitly requested.
10. Keep implementation slices small enough to test and review independently.
11. Run targeted checks first, then the applicable quality gate from `QUALITY.md`.
12. Record blockers instead of guessing missing symbols, commands, contracts or evidence.

## Process Strand Routing

- Exact `skills update` activates the `skills-agents` strand and routes to Skill Registry Conflict Auditor, Senior Documentation Engineer, Organigramm Maintainer and Process Governance Maintainer.
- Exact `workflow create` activates the `workflow create` strand and routes through Senior Requirement Engineer review, requirement clarification, five-role Three Amigos review, branch governance, workflow authoring and arc42 validation.
- Exact `workflow execute` activates the `workflow execute` strand and routes through the workflow executor, slice role reviews, quality gates and slice checkpoint push.

The strands must not be mixed. Slice checkpoint push is not `push auto`, and `push auto` belongs only to `skills-agents`.

## Strand-Safe Blocker Handling

The active process strand must first attempt local blocker resolution when the blocker belongs to the active strand authority.

Rules:

```text
skills update
  resolves only skill, role, routing, registry and process-governance blockers

workflow create
  resolves only requirement, workflow structure, slice metadata, handoff and checked arc42 blockers

workflow execute
  resolves only active-workflow and current-slice blockers
```

The Swarm Orchestrator must not switch strands automatically. A blocker owned by another strand is reported as `CROSS_STRAND_BLOCKER` with the owning strand and recommended next command.

Local resolution attempts are capped at `maxRetries = 3` and must rerun the failed validation before continuing.

## Execution Profile Routing

Execution profiles reduce unnecessary full-review work only when the affected
scope is verified:

- `FAST_PATH`: documentation-only changes that cannot affect product build,
  runtime behavior, contracts, tests, architecture, branch, publication,
  quality or process authority.
- `NORMAL_PATH`: isolated changes with verified owner, disjoint locks and no
  architecture, contract, persistence, runtime, deployment or quality-policy
  impact.
- `GOVERNANCE_FAST_PATH`: governance-only changes with verified scope and no product, runtime, contract, persistence, quality-rule, branch-rule or publication-rule impact.
- `FULL_PATH`: governance authority, skills, roles, routing, process strands,
  quality rules, branch rules, workflow structure, contracts, persistence,
  runtime, deployment, analysis-engine or unclear-impact work.

Profiles may reduce unaffected roles to N/A impact checks. They must not
remove mandatory authority, required STOP paths or required quality gates.

## S3D Execution Orchestration

S3D belongs to the `workflow execute` strand. The Senior Execution Orchestrator
and `skills/s3d-execution-orchestrator/SKILL.md` own the technical dependency
graph, topological sort, execution groups and lock validation. The Senior Swarm
Orchestrator remains responsible for coordination, handoffs and conflict
communication across roles.

If S3D finds missing metadata, ambiguous dependency ranges, unknown slice IDs,
cycles or overlapping locks, it stops before implementation. Lock conflicts
route as `LOCK_CONFLICT`; dependency or metadata blockers escalate through the
Workflow Executor or Root Architect path. S3D must not call `workflow create` or
rewrite the active workflow during execution.

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
