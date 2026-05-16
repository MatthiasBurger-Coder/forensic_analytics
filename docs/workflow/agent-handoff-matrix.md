# Agent Handoff Matrix

This matrix records owner and review expectations for the Git Branch Strategy
workflow.

| Slice | Owner | Inputs | Outputs | Review / Handoff |
| --- | --- | --- | --- | --- |
| 00 - Baseline And Reconciliation Gate | Senior Workflow Architect | Git status, default branch, related governance branch | Updated git-state review and execution notes | Senior Git Workspace Specialist confirms branch safety. |
| 01 - Repository-Governance Rule | Senior Git Workspace Specialist | `AGENTS.md`, branch-governance skills, branch-first predecessor branch | Updated workflow-create branch rule | Senior System Architect checks governance authority. |
| 02 - Workflow-Scope Classification | Senior Workflow Architect | Branch rule, prompts, routing docs | Prefix decision order and required output text | Senior Agent Orchestrator verifies routing impact. |
| 03 - Branch Conflict Check | Senior Git Workspace Specialist | Branch strategy rules, workflow prompt, conflict skill | Local and remote collision behavior | Senior Tester verifies reproducible command evidence. |
| 04 - Subagent Assignment By Scope | Senior Agent Orchestrator | Scope rules, routing docs, role files | Scope-to-role mapping | Senior System Architect checks architecture cases. |
| 05 - Quality Assurance By Scope | Senior Tester | `QUALITY.md`, Gradle build, quality skills | Scope quality expectations | Senior Git Workspace Specialist checks commit/push readiness. |
| 06 - Documentation, Prompts And Skills Sync | Senior Documentation Engineer | All changed governance files | Consistent docs, prompts and execution summary | Senior Agent Orchestrator verifies no stale route remains. |

## Handoff Rules

- Each owner verifies the active branch before editing.
- Each owner records changed files and verification evidence before handoff.
- Reviewers do not approve uncertain branch ownership, missing quality evidence
  or unresolved prompt conflicts.
- Write-capable parallel work is blocked unless file ownership is disjoint.
