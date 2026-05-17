# Branch Rules

## Rules

- Larger workflow execution should happen on a dedicated branch.
- New workflow creation must ensure a dedicated workflow branch exists, is checked out and is verified before any workflow planning artifact is created or modified.
- `workflow create` produces checked `docs/workflow/workflow.md` plus checked or updated `docs/arc42/**`; supporting sidecars do not replace those required outputs.
- Default Codex branch prefix is `codex/` unless the user requests another prefix.
- For `workflow create`, use `feature/workflow-<short-topic>-<yyyyMMdd>` by default, with `fix/`, `docs/` or `architecture/` only when the workflow scope clearly matches that category.
- Branch purpose must match workflow scope.
- Do not create or modify workflow planning artifacts on `main`, `master`, `develop` or another shared branch.
- Do not switch branches if doing so would endanger uncommitted user work.
- Before staging or committing on Windows-hosted WSL worktrees, check for broad line-ending-only noise.

## STOP Rules

Stop when:

- branch context is unclear;
- the current branch is detached or unclear;
- local or remote branch-name collisions cannot be resolved with a clear unique suffix;
- the workflow branch cannot be created, checked out or verified as active;
- branch switch would lose or hide uncommitted work;
- line-ending-only changes pollute unrelated files;
- workflow scope and branch purpose conflict.
