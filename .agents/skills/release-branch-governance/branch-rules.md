# Branch Rules

## Rules

- Larger workflow execution should happen on a dedicated branch.
- Default Codex branch prefix is `codex/` unless the user requests another prefix.
- Branch purpose must match workflow scope.
- Do not switch branches if doing so would endanger uncommitted user work.
- Before staging or committing on Windows-hosted WSL worktrees, check for broad line-ending-only noise.

## STOP Rules

Stop when:

- branch context is unclear;
- branch switch would lose or hide uncommitted work;
- line-ending-only changes pollute unrelated files;
- workflow scope and branch purpose conflict.
