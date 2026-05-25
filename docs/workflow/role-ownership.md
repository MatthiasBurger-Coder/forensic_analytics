# Role Ownership

## Mandatory Roles

| Role | Workflow Responsibility |
|---|---|
| Senior Requirement Engineer | Maintains the accepted branch-selection assumption, EPIC alignment, scope and stop conditions. |
| Senior System Architect | Owns service-boundary, evidence-semantics and contract-stop review. |
| Senior Java Backend Developer | Verifies backend/API sufficiency; implementation is N/A unless S01 stops for contract-first recut. |
| Senior React Frontend Developer | Owns Workspaces list branch selector state, rendering and frontend adapter usage. |
| Senior Tester | Owns regression coverage, targeted verification, full gate planning and diff checks. |

## Conditional Roles

| Role | Trigger |
|---|---|
| Senior UX Designer | Required for selector accessibility, keyboard behavior and responsive layout in S02. |
| Senior Documentation Engineer | Required for S04 workflow and arc42 documentation closure. |
| Contract governance reviewer | Required only if S01 finds a new REST/gRPC contract is necessary. |
| Security reviewer | Required only if implementation would touch branch discovery, Git output, local paths, credentials or cleanup behavior. |
| Senior DevOps Engineer | Required only if build, CI, Docker, runtime configuration or deployment behavior changes. |

## Callable Subagents Used During Workflow Create

| Subagent Role | Result |
|---|---|
| Senior Requirement Engineer | Completed read-only requirement review with `REQUIRES_REFINEMENT` dissent unless the UI-only branch-record assumption is accepted. |
| Senior System Architect | Completed read-only architecture review. |
| Senior Java Backend Developer | Completed read-only backend/API review confirming existing contracts are sufficient only for UI-only selection from `branches[]`. |
| Senior React Frontend Developer | Completed read-only frontend review. |
| Senior Tester | Completed read-only quality and test review. |

No subagent modified files during workflow creation. Backend findings confirm
that OpenAPI, gRPC, `services/query-report-api-service/**` and
`services/repository-source-service/**` must remain untouched unless a new
contract-first workflow is created for remote branch discovery.

## Execution Ownership Rules

- Every slice owner verifies the active branch before editing.
- Subagents must not switch branches.
- Implementation must not occur on `main`, `master`, `develop` or another
  shared branch.
- A selected branch is UI state until repository-source reports verified
  branch evidence.
- Backend or contract changes are forbidden unless the workflow is recut after
  S01 stop.
