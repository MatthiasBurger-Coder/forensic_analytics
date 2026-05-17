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
| `governance-flowchart-v2-20260517` | 10 | D8 and Q11 separation | Senior Tester | `.agents/prompts/slice-execute.md`; `.agents/prompts/workflow-execute.md`; `.agents/skills/git-commit-preparation/SKILL.md`; `.agents/skills/quality-gate-orchestrator/SKILL.md`; `.agents/skills/quality-gate-orchestrator/quality-gates.md`; `.agents/skills/quality-gate-orchestrator/workflow.md`; `.agents/skills/release-branch-governance/SKILL.md`; `.agents/skills/release-branch-governance/commit-rules.md`; `.agents/skills/release-branch-governance/push-rules.md`; `.agents/skills/release-branch-governance/workflow.md`; `.agents/skills/workflow-executor/SKILL.md`; `docs/agents/agent-governance.md`; `docs/agents/organigramm.md`; `docs/process/branch-governance.md`; `docs/process/workflow-execute.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md` | documentation gate; D8/Q11 rule search; regulatory exception search; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT` | pending until `CP_COMMIT` | revert Slice 10 checkpoint commit after `CP_COMMIT` | no | no |
| `governance-flowchart-v2-20260517` | 11 | Guard name sharpening | Senior Documentation Engineer | `AGENTS.md`; `docs/agents/agent-governance.md`; `docs/agents/organigramm.md`; `docs/agents/skill-registry.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/08-crosscutting-concepts.md`; `docs/arc42/10-quality-requirements.md`; `docs/arc42/11-risks-and-technical-debt.md`; `docs/process/push-auto.md`; `docs/skill-audit/skill-inventory.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md`; `docs/workflow/workflow.md` | documentation gate; guard-name search; active old-name search; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT` | pending until `CP_COMMIT` | revert Slice 11 checkpoint commit after `CP_COMMIT` | yes | no |
| `governance-flowchart-v2-20260517` | 12 | Documentation governance separation | Senior Documentation Engineer | `.agents/skills/documentation-sync/SKILL.md`; `docs/agents/agent-governance.md`; `docs/agents/organigramm.md`; `docs/agents/skill-registry.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/08-crosscutting-concepts.md`; `docs/arc42/12-glossary.md`; `docs/governance/README.md`; `docs/process/README.md`; `docs/process/skills-update.md`; `docs/process/workflow-create.md`; `docs/process/workflow-execute.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md` | documentation gate; DOCROOT/S1_DOC/S2_DOC/S3_DOC search; fourth-strand wording check; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT` | pending until `CP_COMMIT` | revert Slice 12 checkpoint commit after `CP_COMMIT` | yes | no |
| `governance-flowchart-v2-20260517` | 13 | Two-level flowchart structure | Senior Documentation Engineer / Senior System Architect checklist | `docs/agents/README.md`; `docs/agents/agent-governance.md`; `docs/governance/README.md`; `docs/governance/workflow/README.md`; `docs/governance/workflow/level-1-overview.md`; `docs/governance/workflow/level-2-subgraphs.md`; `docs/process/README.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md`; `docs/workflow/workflow.md` | documentation gate; level-1/level-2 label search; diagram review-rule search; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT`; arc42 runtime alignment deferred to Slice 15 | pending until `CP_COMMIT` | revert Slice 13 checkpoint commit after `CP_COMMIT` | no | no |
| `governance-flowchart-v2-20260517` | 14 | Agent and skill linkage | Skill Registry Conflict Auditor / Senior Documentation Engineer | `.agents/orchestrator/routing-rules.md`; `docs/agents/skill-registry.md`; `docs/skill-audit/README.md`; `docs/skill-audit/governance-flowchart-v2-linkage.md`; `docs/skill-audit/manual-review-required.md`; `docs/skill-audit/skill-inventory.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md` | documentation gate; capability-linkage search; missing-role and missing-skill gap search; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT` | pending until `CP_COMMIT` | revert Slice 14 checkpoint commit after `CP_COMMIT` | no | no |
| `governance-flowchart-v2-20260517` | 15 | arc42 ADR and governance documentation | ADR Steward / arc42 Architecture Governance / Senior System Architect | `docs/adr/ADR-0021-governance-flowchart-v2.md`; `docs/adr/README.md`; `docs/arc42/README.md`; `docs/arc42/02-architecture-constraints.md`; `docs/arc42/05-building-block-view.md`; `docs/arc42/06-runtime-view.md`; `docs/arc42/08-crosscutting-concepts.md`; `docs/arc42/09-architecture-decisions.md`; `docs/arc42/10-quality-requirements.md`; `docs/arc42/11-risks-and-technical-debt.md`; `docs/arc42/12-glossary.md`; `docs/governance/workflow/README.md`; `docs/process/workflow-execute.md`; `docs/workflow/execution-summary.md`; `docs/workflow/governance-inventory.md`; `docs/workflow/workflow.history.md`; `docs/workflow/workflow.md` | documentation gate; ADR numbering check; ADR/arc42 label search; verified-owner wording search; forbidden product-path scope check; staged diff review | passed before `CP_COMMIT` | pending until `CP_COMMIT` | revert Slice 15 checkpoint commit after `CP_COMMIT` | yes | yes |
