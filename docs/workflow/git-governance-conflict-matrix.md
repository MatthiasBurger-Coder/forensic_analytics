# Git Governance Conflict Matrix

| Area | Current Finding | Workflow Decision | Stop Condition |
| --- | --- | --- | --- |
| Default Codex prefix | Existing branch strategy uses `codex/` for generic agent-created work. | `workflow create` receives a specific exception: default `feature/workflow-...` with three special prefixes. | A rule applies `codex/` to `workflow create` after this workflow is executed. |
| Scope classification | Architecture review flagged governance work as possibly `architecture/`. | This workflow records `feature/` because the user supplied Git Branch Strategy as the default feature example. | A future workflow is clearly architecture-only but is forced to `feature/`. |
| Existing related branch | `feature/workflow-branch-isolation-20260516` already changes branch-first workflow governance files. | Execution must inspect and reconcile it before editing overlapping files. | The branch cannot be inspected or ownership is unclear. |
| Remote default | `origin/HEAD` is not configured. | Use explicit `origin/main`. | Default branch cannot be determined from remote metadata. |
| Branch collision handling | Local and remote branch checks are required before creation. | Use `git branch --list` and `git ls-remote --heads origin`. | Collision ownership cannot be classified safely. |
| Quality authority | `QUALITY.md` defines required Gradle gates. | Workflow docs and execution must use `QUALITY.md` commands. | A required gate is replaced with an undocumented command. |
| Line endings | Repository has tracked files with CRLF or mixed EOL risk. | Use WSL and verify `git diff --check`; stop on broad line-ending-only noise. | Status shows broad unrelated EOL-only changes. |
