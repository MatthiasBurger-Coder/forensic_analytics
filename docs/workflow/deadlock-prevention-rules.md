# Deadlock Prevention Rules

This workflow changes branch and workflow-governance rules. These controls
prevent circular waits, branch collisions and overlapping agent edits.

## Dependency Rules

- Slice 00 must complete before any governance file is edited.
- Slices 01 through 06 are sequential by default because their write scopes
  overlap.
- Read-only reviews may run in parallel after the active branch is verified.
- Write-capable parallel work requires disjoint files and an explicit handoff in
  `agent-handoff-matrix.md`.

## Branch Rules

- Work must stay on `feature/workflow-git-branch-strategy-20260516` unless the
  user explicitly approves a different branch.
- Do not merge, rebase or delete `feature/workflow-branch-isolation-20260516`
  without explicit user approval.
- Before branch creation logic is changed, verify local and remote collision
  commands exactly.
- Do not infer that a missing remote branch means a local branch is safe to
  overwrite.

## Ownership Rules

- Senior Git Workspace Specialist owns branch-state review and collision logic.
- Senior Workflow Architect owns workflow artifact structure and slice order.
- Senior System Architect owns architecture-sensitive scope classification.
- Senior Tester owns quality-gate classification and required command evidence.
- Senior Documentation Engineer owns final discoverability and consistency.

## Stop Conditions

- A dependency cycle appears between branch rules, workflow prompts and release
  governance.
- Two slices need the same file and no handoff is recorded.
- The predecessor branch changes the same governance file and no reconciliation
  decision exists.
- A commit or push is requested before required quality evidence exists.
- A reviewer cannot verify the files or commands it is asked to approve.
