# ADR-0021: Governance Flowchart V2

## Status

Accepted

## Context

ADR-0020 defines the three agent process strands and separates slice
checkpoint push, `push` and `push auto`. Governance Flowchart V2 sharpens that
model after workflow execution exposed additional control risks:

- S3 preflight failures could continue without explicit STOP paths.
- Quality failures could route through generic retry behavior.
- Automatic feedback loops could become unbounded.
- Unclassified slices could proceed without a responsible owner.
- Rollback and publication failures were not explicit enough.
- Large flowcharts were too difficult to review for dead nodes, missing
  branches and wrong backward jumps.

The governance model controls repository changes. It is architecture-relevant
because agents can modify source, tests, build logic, contracts, documentation,
skills, branches and publication state.

## Decision

We accept Governance Flowchart V2 as the active repository governance model.

`workflow execute` must pass explicit S3 safety preflight nodes before any
write-capable slice execution:

- `S3_STATUS`
- `S3_BRANCH`
- `S3_SCOPE`
- `S3_CLASSIFY`

Dirty working trees, wrong branches, scope conflicts and unclassifiable slices
must STOP, report and escalate as documented. `workflow execute` must not
automatically call, regenerate or rewrite `workflow create` artifacts.

Quality and validation failures must route through the Typed Error Router
before retry or targeted fix work. The accepted categories are:

- `ARCH_VIOLATION`
- `BUILD_FAILURE`
- `TEST_FAILURE`
- `DOC_GOVERNANCE_FAILURE`
- `LOCK_CONFLICT`
- `UNKNOWN_FAILURE`

Automatic clarification, correction and targeted-fix loops are capped at
`maxRetries = 3`. Retry exhaustion escalates to the Root Architect decision
path.

S3D is the workflow-execute Execution Orchestrator. It extracts slice metadata,
builds a dependency graph, runs topological sort, forms parallelization groups
and enforces file, contract, module and architecture-boundary locks before
write-capable work starts.

Each `workflow execute` checkpoint commit must represent exactly one slice.
The traceability chain is:

```text
Workflow-Version -> Slice -> Agent -> Files -> Tests -> Commit -> Quality Gate -> Report
```

Commit, checkpoint and rollback governance must include `CP_ROLLBACK`, and
publication outcomes must be explicit, including `PUB_DONE`, `PUB_PR_RESULT`,
`PUB_PUSH_FAILED` and `PUB_REJECTED`.

Large governance diagrams must be maintained as a two-level flowchart package:

- Level 1: global governance overview.
- Level 2: separate reviewable subgraphs for S1, S2, S3, BE, FE, RT, QG, CP,
  PUB and DOC.

## Rationale

S3 STOP paths prevent execution from mutating files when the working tree,
branch or workflow scope is unsafe.

Typed error routing prevents generic retries from assigning the wrong owner to
architecture, build, test, documentation or lock failures.

`maxRetries = 3` prevents infinite correction loops and creates a deterministic
escalation point.

Forbidding automatic jumps from `workflow execute` back to `workflow create`
preserves the boundary between workflow design and workflow execution.

One-slice-one-commit keeps rollback and audit evidence precise. A failed or
reverted slice can be identified without reversing unrelated changes.

Two-level flowcharts keep the model reviewable. They make dead nodes, missing
`no` paths, circular references, missing terminals and wrong backward jumps
visible without forcing all process detail into one large diagram.

## Consequences

- `docs/process/**`, `docs/workflow/**`, `docs/governance/workflow/**`,
  `docs/agents/**`, `docs/skill-audit/**` and `.agents/**` must stay aligned
  with this decision.
- arc42 documents Governance Flowchart V2 as architecture governance, not as
  product runtime behavior.
- At the time of this decision, missing dedicated Root Architect and Flowchart
  Integrity Audit artifacts were documented with bootstrap owners.
- Current status: Root Architect escalation and Flowchart Integrity Audit have
  dedicated governance artifacts with explicit specialist collaboration and
  escalation ownership. This status update does not change the decision
  history or intent.
- Workflow execution may stop more often, but every stop has an owner,
  rationale and recovery path.
- Rollback must remain a decision node and must not be represented as blind
  `git reset --hard`.
