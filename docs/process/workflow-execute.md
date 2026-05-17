# Workflow Execute Command

`workflow execute` activates the workflow execution process strand.

It executes a checked `docs/workflow/workflow.md` slice by slice through the configured subagent workflow, required role reviews, tests, documentation updates, quality gates and slice checkpoint pushes.

## Start Conditions

`workflow execute` may start only when both are present and checked:

1. complete checked `docs/workflow/workflow.md`
2. checked or updated `docs/arc42/**` documentation

Stop when either artifact is missing or contradicts `AGENTS.md`, `QUALITY.md`, ADRs or verified repository state.

## S3 Safety Preflight

`workflow execute` must pass these safety nodes before any slice is routed or
implemented:

```mermaid
flowchart TD
  S3_STATUS["S3_STATUS: Check working tree"] -->|clean| S3_BRANCH["S3_BRANCH: Check execution branch"]
  S3_STATUS -->|dirty working tree| S3_STOP_STATUS["STOP: Dirty working tree - report only"]
  S3_BRANCH -->|valid workflow branch| S3_SCOPE["S3_SCOPE: Check workflow scope"]
  S3_BRANCH -->|wrong branch| S3_STOP_BRANCH["STOP: Wrong branch - report only"]
  S3_SCOPE -->|scope valid| S3_CLASSIFY["S3_CLASSIFY: Classify slice"]
  S3_SCOPE -->|scope conflict| S3_STOP_SCOPE["STOP: Scope conflict - escalate"]
  S3_CLASSIFY -->|backend| S3D["S3D: Execution Orchestrator"]
  S3_CLASSIFY -->|frontend| S3D
  S3_CLASSIFY -->|runtime / devops / contracts| S3D
  S3_CLASSIFY -->|documentation / governance / metadata declared by workflow| S3D
  S3_CLASSIFY -->|none of the above| S3_UNCLASSIFIED["S3_UNCLASSIFIED: Stop and escalate"]
  S3_UNCLASSIFIED --> ROOT_ARCHITECT["Root Architect decision"]
  S3D -->|backend graph and locks valid| BE_Q["BE_Q: Backend slice"]
  S3D -->|frontend graph and locks valid| FE_Q["FE_Q: Frontend slice"]
  S3D -->|runtime graph and locks valid| RT_Q["RT_Q: Runtime slice"]
  S3D -->|documentation graph and locks valid| DOC_Q["DOC_Q: Documentation slice"]
  S3D -->|lock conflict| S3D_LOCK_CONFLICT["LOCK_CONFLICT: Route to Typed Error Router"]
  S3D -->|cycle, missing metadata or unknown dependency| S3D_STOP["STOP: Orchestration blocker - escalate"]
```

S3 STOP paths report the blocker and do not jump back to `workflow create`.
Scope conflicts escalate to the Root Architect for a decision.
Unclassifiable slices must not execute automatically.

Explicitly declared governance, metadata and documentation-only slices may route
through the Documentation Strand only when the active workflow declares that
scope. Otherwise they are unclassified and must escalate.

## S3D Execution Orchestrator

S3D runs after `S3_CLASSIFY` and before write-capable slice execution. It is the
Execution Orchestrator for `workflow execute`, not a fourth process strand.

S3D reads the checked `docs/workflow/workflow.md` and extracts:

- slice ID
- slice goal
- affected files
- affected modules
- affected contracts
- responsible subagents or roles
- dependencies
- quality gates
- documentation duties

S3D then builds a directed dependency graph, runs topological sort, forms
independent parallelization groups and checks file, contract, module and
architecture-boundary locks before any write-capable agent starts.

Parallel execution is allowed only when all active slices have disjoint write
scopes, no shared contract or architecture boundary is edited by more than one
active slice, quality gates can be attributed independently and documentation
ownership is explicit.

S3D must stop and report when metadata is missing, dependency references are
unknown, dependency ranges are not expanded to concrete slice IDs, the graph has
a cycle, or a lock overlaps. Lock overlaps route as `LOCK_CONFLICT` through the
Typed Error Router. S3D must not call `workflow create`, rewrite the active
workflow during execution or expand scope automatically.

## Typed Error Router

Quality-gate and validation failures in `workflow execute` must route through
typed ownership before any retry or fix attempt starts:

```mermaid
flowchart TD
  Q10["Q10: Quality Gate / Validation Failure"] --> R["Typed Error Router"]
  R -->|ARCH_VIOLATION| A["Root Architect / Senior System Architect / Hexagonal Architecture Expert"]
  R -->|BUILD_FAILURE| B["Responsible Backend or Frontend Agent / DevOps / Build Owner"]
  R -->|TEST_FAILURE| T["Senior Tester / Responsible Slice Agent"]
  R -->|DOC_GOVERNANCE_FAILURE| D["Documentation Governance Agent / Requirement Engineer"]
  R -->|LOCK_CONFLICT| L["Execution Orchestrator Specialist / Root Architect"]
  R -->|UNKNOWN_FAILURE| X["Root Architect Escalation"]
  A --> RC{"Retry <= 3?"}
  B --> RC
  T --> RC
  D --> RC
  L --> RC
  X --> ESC["Escalate to Root Architect"]
  RC -->|yes| FIX["Targeted Fix Slice inside S3"]
  RC -->|no| ESC
  FIX --> Q10
```

The router categories are:

| Error type | Target role |
|---|---|
| `ARCH_VIOLATION` | Root Architect, Senior System Architect, Hexagonal Architecture Expert |
| `BUILD_FAILURE` | responsible Backend or Frontend Agent, DevOps, Build Owner |
| `TEST_FAILURE` | Senior Tester, responsible Slice Agent |
| `DOC_GOVERNANCE_FAILURE` | Documentation Governance Agent, Requirement Engineer |
| `LOCK_CONFLICT` | Execution Orchestrator Specialist, Root Architect |
| `UNKNOWN_FAILURE` | Root Architect |

Automatic fix attempts are capped at `maxRetries = 3`. Retry exhaustion,
`UNKNOWN_FAILURE`, missing ownership or unclear classification stops execution
and escalates to the Root Architect. Targeted fix slices remain inside
`workflow execute`; the router must not jump back to `workflow create` or expand
the workflow scope automatically.

## Execution Strands

Workflow execution must keep these implementation and documentation strands separate:

- Backend Strand
- Frontend Strand
- Docker / Runtime Strand
- Documentation Strand

## Backend Strand

Required roles and skills when backend work is in scope:

- Senior Java Backend Developer
- Microservice Senior Expert
- `architecture-hexagonal`
- `spring-core` when Spring wiring is affected
- `testing-junit6`
- Senior DevOps with `devops-docker` when container readiness is affected

Required rules:

- JUnit 6 tests or a checked workflow exception
- hexagonal architecture
- domain, ports and adapters separated
- no framework leaks into domain
- Microservice Senior Expert checks service autonomy
- no shared Java implementation modules between microservices
- communication only through REST/OpenAPI, gRPC/protobuf or approved messaging contracts

## Frontend Strand

Required roles when frontend work is in scope:

- Senior React Frontend Developer
- Senior UX Designer
- Senior DevOps with `devops-docker` when container readiness is affected

## Slice Checkpoint Push

After each successfully completed slice:

1. Run the slice quality gate.
2. Inspect the slice diff.
3. Stage only files changed by this slice.
4. Run `git diff --cached --check`.
5. Create a slice-scoped checkpoint commit.
6. Push the current workflow branch to `origin`.
7. Record the commit SHA and push result in the execution report.
8. Continue with the next slice only after the checkpoint push succeeded.

Not allowed:

- no commit when the slice quality gate failed
- no commit when the diff is unclear
- no commit with files from other slices
- no push to `main`
- no PR merge
- no `push auto`
- no branch cleanup
- no force-push

Commit messages use:

```text
<type>(slice-<nn>): <short description>
```

Examples:

```text
docs(slice-01): refine workflow create clarification loop
agent(slice-02): align three amigos gate roles
test(slice-03): add backend regression coverage
feat(slice-04): implement ingestion service boundary
```

## Recovery Rule

If the machine crashes or the local worktree is lost, restore the last successful state from `origin/<workflow-branch>`. The execution report must show the latest completed slice, commit SHA and pushed branch state.

## Relationship To Publication Modes

Slice checkpoint push is not `push auto`.

Slice checkpoint push does not create or merge a PR.

Slice checkpoint push does not run branch cleanup.
