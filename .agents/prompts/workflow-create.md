# Workflow Create Prompt

Use when the user writes `workflow create`.

Read-only verification, requirement intake, routing-rule inspection and role selection may occur before branch creation. Mutating workflow creation must not.

`workflow create` is a planning and documentation strand. It must not implement
backend, frontend, Docker/runtime or analytics product code. It is complete only
when both checked outputs exist:

1. `docs/workflow/workflow.md`
2. checked or updated `docs/arc42/**` documentation

## Required Branch-First Flow

1. Load root `AGENTS.md`.
2. Load root `QUALITY.md`.
3. Load `.agents/skills/workflow-authoring/SKILL.md`.
4. Load `.agents/orchestrator/routing-rules.md`.
5. Load `.agents/skills/git-branch-strategy/SKILL.md`.
6. Load `.agents/skills/workflow-conflict-resolution/SKILL.md`.
7. Load `.agents/skills/release-branch-governance/branch-rules.md`.
8. Verify the Git repository context:

```bash
git rev-parse --show-toplevel
```

9. Check the working tree:

```bash
git status --short
```

10. Stop if the current branch is detached, unclear, or if unrelated or unclear uncommitted changes exist.
11. Generate a dedicated workflow branch name unless the current branch already exactly matches this workflow:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
fix/workflow-<short-topic>-<yyyyMMdd>
docs/workflow-<short-topic>-<yyyyMMdd>
architecture/workflow-<short-topic>-<yyyyMMdd>
```

12. Check local and remote branch-name collisions, choosing the next clear unique suffix when needed.
13. Create and checkout the workflow branch when no matching workflow branch is active:

```bash
git checkout -b <workflow-branch>
```

14. Verify that the branch ref exists and that it is active:

```bash
git show-ref --verify --quiet refs/heads/<workflow-branch>
git branch --show-current
```

15. Continue only when the local branch ref exists and the active branch exactly
    matches the workflow branch.
16. Create or sharpen `docs/workflow/workflow.md` only after successful branch verification.
17. Check and update arc42 documentation when the workflow affects architecture.
18. Validate both checked outputs before release for `workflow execute`.
19. Build slices, role ownership, quality gates and stop conditions through the workflow-authoring skill.

Before workflow authoring continues, record a Three Amigos decision with these
roles:

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

For microservice migration workflows, the decision must include scope,
non-scope, acceptance criteria, service boundary, contract impact, data
ownership impact, test impact, risk level and stop conditions.

## Required docs/workflow/workflow.md Sections

`docs/workflow/workflow.md` must include:

- Executive Summary
- Target Picture
- Scope
- Non-Goals
- Architecture Boundaries
- Backend Assessment
- Frontend Assessment
- Test Strategy
- Slice Structure
- Subagent Assignment
- Quality Gates
- Definition of Done
- Handoff to `workflow execute`
- arc42 Check Status

The arc42 check status must record inspected sections, updated sections or
`no update required`, reviewer or role, date, branch and unresolved drift.

## Subagent Rules

- Subagents must verify that the active branch belongs to the current workflow before modifying files.
- Subagents must not switch branches unless the workflow explicitly authorizes that branch operation.
- Subagents must stop before implementation work on `main`, `master`, `develop`, or any shared branch.
- Parallel subagents must work inside the same workflow branch unless the workflow explicitly defines separate worktrees.

## Stop Conditions

Stop when:

- no Git repository is detected;
- the current branch cannot be determined;
- the current branch is detached or unclear;
- unrelated or unclear uncommitted changes exist;
- local or remote branch-name collisions cannot be resolved with a clear unique suffix;
- the workflow branch cannot be created;
- the workflow branch cannot be checked out;
- the workflow branch ref cannot be verified after creation or checkout;
- the active branch after checkout does not match the expected workflow branch;
- workflow rules conflict and cannot be resolved from repository sources;
- creating or modifying workflow planning artifacts would happen on `main`, `master`, `develop`, or another shared branch;
- backend, frontend, Docker/runtime or analytics implementation would be required;
- `docs/workflow/workflow.md` cannot be completed;
- arc42 cannot be checked or updated from verified evidence.

Use this stop report:

```text
STOP: workflow create cannot continue safely.
Reason: <concrete reason>
No workflow files were created before resolving the branch isolation issue.
```
