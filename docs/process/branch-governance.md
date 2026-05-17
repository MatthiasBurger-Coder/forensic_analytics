# Branch Governance

Branch isolation protects workflow artifacts, skill governance changes and
publication readiness from accidental changes on shared branches.

## Required Checks

Before mutating files for any governed process strand:

1. Verify the repository root.
2. Inspect `git status --short`.
3. Inspect the active branch.
4. Stop when the branch is detached, unclear or shared.
5. Stop when unrelated or unclear local changes exist.
6. Check local and remote branch-name collisions.
7. Create or verify a dedicated branch.
8. Verify the local branch ref and active branch.

On Windows hosts, repository commands must run through WSL from the WSL-mounted
worktree path.

## Branch Names

`workflow create` uses the branch families documented in `AGENTS.md` and
`.agents/skills/git-branch-strategy/SKILL.md`:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
fix/workflow-<short-topic>-<yyyyMMdd>
docs/workflow-<short-topic>-<yyyyMMdd>
architecture/workflow-<short-topic>-<yyyyMMdd>
```

Agent, skill and process-governance workflow creation uses
`architecture/workflow-<short-topic>-<yyyyMMdd>` unless a future documented rule
accepts a more specific prefix. The `agent/` prefix is not currently a
documented repository branch family.

Normal non-workflow implementation work may use focused `codex/` branches when
the user did not request a different documented strategy.

## Shared Branch Block

Do not create or modify workflow artifacts, skill/agent governance artifacts or
implementation files on:

```text
main
master
develop
```

Subagents must verify the active branch before modifying files. Subagents must
not switch branches unless the active workflow explicitly authorizes that
operation.
