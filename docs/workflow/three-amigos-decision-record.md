# Three Amigos Decision Record

Decision: `READY_FOR_WORKFLOW`

Confidence: 92 percent.

## Requirement

Enable the GUI to list remote repository branches returned by `POST /api/workspace-metadata` for the submitted repository URL, and persist the selected branch through repository-source workspace metadata.

## Roles

| Role | Decision |
|---|---|
| Senior Requirement Engineer | Requirement is complete enough for workflow execution. |
| Senior System Architect | Repository-source remains owner; query-report and UI are facades/clients only. |
| Senior Java Backend Developer | Existing backend symbols and contract fields are verifiable. |
| Senior React Frontend Developer | Existing UI metadata model and create-workspace page are verifiable. |
| Senior Tester | Acceptance criteria can be tested with deterministic fake branch fixtures. |

## Acceptance Criteria

- `repositoryBranches` is preserved from repository-source metadata resolution through gRPC and REST.
- The UI renders the returned remote branch list.
- The selected branch is persisted through repository-source-owned workspace branch metadata.
- Tests do not require live GitHub.
- Diagnostics distinguish empty branch response from stale service/gateway/UI data-path failures.
