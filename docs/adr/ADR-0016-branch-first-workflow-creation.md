# ADR-0016: Create Workflow Branches Before Workflow Artifacts

## Status

Accepted

## Date

2026-05-16

## Context

Forensic Analytics uses governed workflows, role routing and subagent handoffs for non-trivial repository work. Multiple workflows may be prepared or executed in parallel, and workflow artifacts can affect agent behavior, quality-gate routing and commit readiness.

Creating or updating workflow artifacts on shared branches such as `main`, `master` or `develop` risks mixing unrelated work, making stale workflow slices appear active, or causing subagents to operate without an isolated branch context.

## Decision

`workflow create` must ensure a dedicated workflow branch exists and is active before mutating workflow artifacts.

Read-only verification, requirement intake, routing-rule inspection and role selection may happen before branch creation. Mutating workflow creation must not.

Workflow planning artifacts, including `docs/workflow/workflow.md`, arc42 workflow-impact sections, slice definitions and write-capable agent assignments, must only be created after the workflow branch is verified.

2026-05-17 amendment: workflow creation now follows the three-strand process
model. `workflow create` is a planning and documentation strand and must end
with two checked outputs:

1. checked `docs/workflow/workflow.md`
2. checked or updated `docs/arc42/**` documentation

Supporting sidecar files under `docs/workflow/**` may exist as historical or
auxiliary material, but they are not completion criteria for new workflow
creation and they do not replace checked `docs/workflow/workflow.md`.

Detached or unclear branch state stops workflow creation. Local and remote branch-name collisions must be checked before branch creation; if a collision exists, the next clear unique suffix must be chosen.

The default branch naming convention is:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
fix/workflow-<short-topic>-<yyyyMMdd>
docs/workflow-<short-topic>-<yyyyMMdd>
architecture/workflow-<short-topic>-<yyyyMMdd>
```

Subagents must verify the active workflow branch before modifying files and must not switch branches unless the workflow explicitly authorizes that branch operation.

## Consequences

- Workflow creation starts from an isolated Git context.
- Shared branches remain free from in-progress workflow artifacts.
- Subagent and role handoffs can require branch evidence before mutating work.
- Existing sidecar workflow material may be preserved until a task explicitly
  archives or migrates it.
- `workflow execute` can require checked `docs/workflow/workflow.md` plus checked
  or updated arc42 documentation as its only start input.
- If the branch cannot be created, checked out or verified, workflow creation stops without creating workflow files.

## Alternatives Considered

- Continue allowing workflow files to be created on the current branch. Rejected because it allows new workflows to start on shared or unrelated branches.
- Rely only on commit-time branch checks. Rejected because workflow artifacts can already influence routing before commit preparation.
- Require branch creation before any read-only routing. Rejected because repository verification and role selection are necessary to choose the correct branch name safely.

## Related Documents

- `AGENTS.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/prompts/workflow-create.md`
- `.agents/skills/git-branch-strategy/SKILL.md`
- `.agents/skills/release-branch-governance/branch-rules.md`
- `docs/governance/README.md`
