# Branch Readiness Checklist

Run these checks before creating or switching to a `workflow create` branch.

## Repository Context

```bash
git rev-parse --show-toplevel
git status --short
git branch --show-current
git remote show origin
```

Stop if the current branch is detached, unclear or contains unrelated local
changes.

## Scope Classification

Record:

```text
Detected workflow branch prefix: <feature|fix|docs|architecture>
Reason: <short reason>
Proposed branch name: <prefix>/workflow-<short-topic>-<yyyyMMdd>
```

Use `feature/` unless the workflow clearly matches `fix/`, `docs/` or
`architecture/`.

## Branch Collision Checks

```bash
git branch --list <branch-name>
git ls-remote --heads origin <branch-name>
```

If the branch exists:

1. Reuse it only when it clearly belongs to the same task.
2. Choose a readable unique fallback when it belongs to another task.
3. Stop when ownership is unclear.

## Branch Creation

Use the verified integration branch explicitly:

```bash
git fetch origin main
git checkout -b <branch-name> origin/main
git branch --show-current
git status --short
```

If the repository uses a different verified integration branch, replace
`origin/main` with that branch. Do not guess.

## Workflow Artifact Gate

Create or regenerate `docs/workflow/**` only after the active branch matches the
verified workflow branch.
