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
  S3_CLASSIFY -->|backend| BE_Q["BE_Q: Backend slice"]
  S3_CLASSIFY -->|frontend| FE_Q["FE_Q: Frontend slice"]
  S3_CLASSIFY -->|runtime / devops / contracts| RT_Q["RT_Q: Runtime slice"]
  S3_CLASSIFY -->|documentation / governance / metadata declared by workflow| DOC_Q["DOC_Q: Documentation slice"]
  S3_CLASSIFY -->|none of the above| S3_UNCLASSIFIED["S3_UNCLASSIFIED: Stop and escalate"]
  S3_UNCLASSIFIED --> ROOT_ARCHITECT["Root Architect decision"]
```

S3 STOP paths report the blocker and do not jump back to `workflow create`.
Scope conflicts escalate to the Root Architect for a decision.
Unclassifiable slices must not execute automatically.

Explicitly declared governance, metadata and documentation-only slices may route
through the Documentation Strand only when the active workflow declares that
scope. Otherwise they are unclassified and must escalate.

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
