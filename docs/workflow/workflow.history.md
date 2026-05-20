# Workflow History

## microservices-btm-pipeline-20260517-v4

| Field | Value |
|---|---|
| Created | 2026-05-20 |
| Branch | `feature/workflow-microservices-btm-pipeline-20260517` |
| Strand | workflow-governance refinement through `workflow create` after blocked `workflow execute` Slice 12 precheck |
| Status | Active. V4 inserts Slice 12 for source-fact byte retrieval, Repository Analysis to Java AST handoff closure and deterministic local E2E fixture readiness before end-to-end orchestration can resume. |
| Source request | Create the missing prerequisite workflow slice after the Slice 12 review identified source-fact byte retrieval and deterministic fixture blockers. |

V4 preserves completed v3 Slice 00 through Slice 11 checkpoints. Downstream
slices are renumbered by one: v3 Slice 12 becomes v4 Slice 13, and the final
quality gate becomes v4 Slice 19.

The v4 prerequisite keeps Java AST as the owner of source-fact artifact bytes
until an explicit handoff or object-store contract transfers custody. It
requires `ArtifactByteAccess.retrieval_contract` to name a real owner API,
closes the Repository Analysis to Java AST handoff signal and defines a
deterministic local fixture path that uses fakes, in-process gRPC or local
fixtures instead of external Git network access, Docker, Jenkins, Artifactory
or credentials.

## microservices-btm-pipeline-20260517-v3

| Field | Value |
|---|---|
| Created | 2026-05-19 |
| Branch | `feature/workflow-microservices-btm-pipeline-20260517` |
| Strand | workflow-governance refinement inside `workflow execute` stop handling |
| Status | Active. V3 inserts Slice 11 for the Repository-to-BTM orchestration contract and artifact-readiness bridge before the end-to-end orchestration slice can resume. |
| Source request | Continue safely by inserting the prerequisite workflow-refinement slice identified after the blocked Slice 11 review. |

V3 preserves completed v2 Slice 00 through Slice 10 checkpoints. Downstream
slices are renumbered by one: v2 Slice 11 becomes v3 Slice 12, and the final
quality gate becomes v3 Slice 18.

The v3 prerequisite closes the Gateway public API security model, the
orchestration owner API, Java AST artifact byte-access preservation, explicit
Joern incomplete handling for unavailable package descriptors and deterministic
local readiness gates before the repository-to-BTM end-to-end implementation
continues.

## microservices-btm-pipeline-20260517-v2

| Field | Value |
|---|---|
| Created | 2026-05-19 |
| Branch | `feature/workflow-microservices-btm-pipeline-20260517` |
| Strand | workflow-governance update inside `workflow execute` recovery |
| Status | Active. V2 inserts Slice 07 for repository snapshot, complete build-output package, optional Artifactory/Jenkins producers, `build-artifact-worker-service` fallback contract, `ArtifactByteAccess` preservation and Joern-owned materialization before the Joern handoff can resume. |
| Source request | Add the missing workspace/build-artifact slice and continue the workflow after the Slice 07 blocker. |

V2 preserves completed v1 Slice 00 through Slice 06 checkpoints. Downstream
slices are renumbered by one: v1 Slice 07 becomes v2 Slice 08, and the final
quality gate becomes v2 Slice 17.

Jenkins and Artifactory remain optional external producers. Local quality gates
must use deterministic tests and fakes unless a later slice explicitly opts into
external integration checks.

## microservices-btm-pipeline-20260517-v1

| Field | Value |
|---|---|
| Created | 2026-05-17 |
| Branch | `feature/workflow-microservices-btm-pipeline-20260517` |
| Strand | `workflow create` |
| Status | Workflow-create package committed; `workflow execute` active with Slice 00, Slice 01, Slice 02, Slice 03, Slice 04, Slice 05 and Slice 06 checkpoints pushed; Slice 07 read-only precondition review is blocked by the missing Repository Analysis to Joern transfer/materialization contract and missing artifact byte-access preservation. |
| Source request | Complete the microservice migration so service collaboration happens through service contracts only; distribute existing implementation into service-owned boundaries; create BTM rules; create workspaces from external Git repositories; accept Git repository requests over HTTP; return completed BTM files over gRPC. |

This workflow replaced the previous active `docs/workflow/**` package during
the branch-first workflow regeneration. Historical workflow files were not
kept in the active workflow directory because workflow creation regenerates the
full package unless preservation is explicitly requested.

Execution CP_RECORD entries must be written to
`docs/workflow/execution-report.md` after `workflow execute` starts. Slices 00,
01, 02, 03, 04, 05 and 06 have completed checkpoint publication for this
workflow version and are recorded there. Slice 07 is recorded as blocked during
read-only precondition review.
