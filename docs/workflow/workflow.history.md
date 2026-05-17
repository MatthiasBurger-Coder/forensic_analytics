# Workflow History

This file records workflow versions and slice checkpoint evidence for
Governance Flowchart V2.

## Active Version

| Field | Value |
|---|---|
| workflowVersion | `governance-flowchart-v2-20260517` |
| workflowTitle | Governance Flowchart V2 |
| sourceWorkflow | `docs/workflow/workflow.md` |
| workflowCreateCommit | `8c0a123` |
| executionBranch | `architecture/workflow-governance-flowchart-v2-20260517` |
| status | in execution |

Workflow versions remain stable for all slices in one checked workflow
execution. A scope, dependency or governance-rule change must be recorded as a
new workflow version instead of being hidden inside a later slice.

## CP_RECORD Fields

Every slice checkpoint record uses these fields:

```text
workflowVersion
sliceId
sliceTitle
responsibleAgent
changedFiles
qualityGateCommands
qualityGateResult
commitHash
rollbackReference
arc42Updated
adrUpdated
```

The `commitHash` field is filled after `CP_COMMIT` succeeds. Before the commit
exists, records may use `pending`; the post-commit checkpoint report must then
record the actual hash and push result.

## Slice Records

| workflowVersion | sliceId | sliceTitle | responsibleAgent | changedFiles | qualityGateCommands | qualityGateResult | commitHash | rollbackReference | arc42Updated | adrUpdated |
|---|---|---|---|---|---|---|---|---|---|---|
| `governance-flowchart-v2-20260517` | 00 | Repository and governance inventory | Senior Workflow Architect | `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md` | `git status --short`; `git diff --check`; slice diff review | passed | `74dafa7` | revert slice commit `74dafa7` | no | no |
| `governance-flowchart-v2-20260517` | 01 | Branch governance confirmation | Senior Git Workspace Specialist | `docs/workflow/execution-summary.md` | branch/ref checks; `git status --short --branch`; `git diff --check`; staged diff review | passed | `95c3a64` | revert slice commit `95c3a64` | no | no |
| `governance-flowchart-v2-20260517` | 02 | Feedback loop limits | Senior System Architect | `AGENTS.md`; `.agents/prompts/**`; `.agents/skills/three-amigos-requirement-gatekeeper/workflow.md`; `.agents/skills/workflow-authoring/SKILL.md`; `docs/agents/**`; `docs/arc42/**`; `docs/process/**`; `docs/workflow/execution-summary.md` | documentation gate; label search; scope check; staged diff review | passed | `cff6d87` | revert slice commit `cff6d87` | yes | no |
| `governance-flowchart-v2-20260517` | 03 | S3 safety preflight | Workflow Executor / Senior System Architect | `.agents/skills/workflow-executor/SKILL.md`; `.codex/skills/workflow-executor/SKILL.md`; `docs/agents/**`; `docs/arc42/**`; `docs/process/workflow-execute.md`; `docs/workflow/execution-summary.md` | documentation gate; S3 label search; forbidden product-path scope check; staged diff review | passed | `0eebf55` | revert slice commit `0eebf55` | yes | no |
| `governance-flowchart-v2-20260517` | 04 | S3_CLASSIFY default path | Senior Swarm Orchestrator | `.agents/skills/workflow-executor/SKILL.md`; `.codex/skills/workflow-executor/SKILL.md`; `docs/agents/**`; `docs/arc42/**`; `docs/process/workflow-execute.md`; `docs/workflow/execution-summary.md` | documentation gate; classification label search; forbidden product-path scope check; staged diff review | passed | `4f44b2f` | revert slice commit `4f44b2f` | yes | no |
| `governance-flowchart-v2-20260517` | 05 | Typed Error Router | Senior Tester / Senior System Architect | `.agents/orchestrator/routing-rules.md`; `.agents/skills/quality-gate-orchestrator/**`; `.agents/skills/workflow-executor/SKILL.md`; `docs/agents/**`; `docs/arc42/**`; `docs/process/workflow-execute.md`; `docs/workflow/**` | documentation gate; typed-error label search; retry-rule search; forbidden product-path scope check; staged diff review | passed | `09b8734` | revert slice commit `09b8734` | yes | no |
| `governance-flowchart-v2-20260517` | 06 | S3D execution orchestration | Senior Swarm Orchestrator | `.agents/orchestrator/**`; `.agents/prompts/workflow-execute.md`; `.agents/roles/senior-swarm-orchestrator.md`; `.agents/skills/agent-swarm-coordination-specialist/SKILL.md`; `.agents/skills/workflow-executor/SKILL.md`; `docs/agents/**`; `docs/process/workflow-execute.md`; `docs/workflow/**` | documentation gate; S3D metadata search; lock-rule search; forbidden product-path scope check; staged diff review | passed | `07f0b6b` | revert slice commit `07f0b6b` | no | no |
| `governance-flowchart-v2-20260517` | 07 | Publication modes cleanup | Senior DevOps Engineer | `docs/agents/**`; `docs/process/**`; `docs/workflow/**` | documentation gate; publication-terminal search; self-reference search; forbidden product-path scope check; staged diff review | passed | `1c10bc9` | revert slice commit `1c10bc9` | no | no |
| `governance-flowchart-v2-20260517` | 08 | Commit checkpoint rollback | Senior DevOps Engineer | `docs/agents/**`; `docs/process/**`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md` | documentation gate; CP label search; rollback safety wording search; forbidden product-path scope check; staged diff review | passed | `081c7a7` | revert slice commit `081c7a7` | no | no |
| `governance-flowchart-v2-20260517` | 09 | Commit traceability and workflow versioning | Senior Documentation Engineer | `.agents/prompts/slice-execute.md`; `.agents/prompts/workflow-execute.md`; `.agents/skills/git-commit-message-preparation/SKILL.md`; `.agents/skills/git-commit-preparation/SKILL.md`; `.agents/skills/release-branch-governance/commit-rules.md`; `.agents/skills/workflow-executor/SKILL.md`; `docs/agents/skill-registry.md`; `docs/process/branch-governance.md`; `docs/process/workflow-execute.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md` | documentation gate; traceability field search; no-multi-slice rule search; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT` | pending until `CP_COMMIT` | revert Slice 09 checkpoint commit after `CP_COMMIT` | no | no |
