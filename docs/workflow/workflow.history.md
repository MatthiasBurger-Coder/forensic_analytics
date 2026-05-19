# Workflow History

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
