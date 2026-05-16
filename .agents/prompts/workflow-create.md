# Workflow Create Prompt

Use when the user writes `workflow create`.

Read-only verification, requirement intake, routing-rule inspection and role selection may occur before branch creation. Mutating workflow creation must not.

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

14. Verify the active branch:

```bash
git branch --show-current
```

15. Continue only when the active branch exactly matches the workflow branch.
16. Create or regenerate workflow artifacts only after successful branch verification.
17. Build slices, role ownership, quality gates and stop conditions through the workflow-authoring skill.

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
- the active branch after checkout does not match the expected workflow branch;
- workflow rules conflict and cannot be resolved from repository sources;
- creating or modifying workflow artifacts would happen on `main`, `master`, `develop`, or another shared branch.

Use this stop report:

```text
STOP: workflow create cannot continue safely.
Reason: <concrete reason>
No workflow files were created before resolving the branch isolation issue.
```
