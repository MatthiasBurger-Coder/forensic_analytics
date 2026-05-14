---
name: commit-preparation
description: Use when preparing, reviewing, validating, or documenting a Git commit in the Forensic Analytics repository. This skill enforces Commit.md, AGENTS.md, QUALITY.md, git diff inspection, task-scope validation, quality-gate verification, and traceable commit message creation.
---

# Commit Preparation Skill

## Goal

Prepare or review a Git commit for the Forensic Analytics repository without weakening repository rules, mixing unrelated changes, or claiming unexecuted verification.

This skill does not replace repository rules. This skill applies repository rules.

## Role Split

Use this skill with these Codex roles:

- `Commit.md`: repository contract and source of truth for commit readiness.
- `commit_reviewer`: read-only reviewer that classifies changes, checks scope, verifies evidence, and returns `READY`, `NOT READY`, or `BLOCKED`.
- `commit_operator`: mutating operator that may stage explicit files, commit, push, and create or complete a GitHub pull request only when the contract allows it.

The reviewer must not modify files. The operator must not bypass the reviewer result, required verification, or the `push` command requirement.

## Required References

Read these files before deciding commit readiness:

```text
AGENTS.md
QUALITY.md
Commit.md
.agents/skills/commit-preparation/workflow.commit-preparation.md
active workflow.md or task-specific workflow if present
```

If any required reference cannot be read, stop and report the missing file.

## When to Use This Skill

Use this skill when:

- preparing a commit,
- reviewing staged or unstaged changes,
- validating commit readiness,
- writing a commit message,
- checking whether changed files belong to a task,
- documenting verification evidence before commit.

## Mandatory Behavior

You must:

- inspect repository rules before commit review,
- inspect staged and unstaged changes separately,
- classify every changed file,
- verify scope against the current task,
- identify generated, sensitive, local, or unrelated files,
- use `QUALITY.md` for verification commands,
- report skipped verification with a clear reason,
- create commit messages only from the real diff and executed verification.

Never approve a commit when required verification failed, unrelated files are present, sensitive data appears in the diff, or the proposed commit message would require guessing.

## Push Command Behavior

When the user enters exactly `push`, treat it as explicit permission to:

- rerun commit readiness,
- obtain or reproduce a `commit_reviewer` readiness result,
- create the commit from the reviewed staged diff,
- push the current non-`main` branch to `origin`,
- create or complete a GitHub pull request from the current branch against `main`.

Do not treat `push` as permission to force-push, push to `main`, merge a pull request, enable auto-merge, skip verification, or create a pull request against another base branch.

Before acting on `push`, verify that commit readiness is `READY`, required verification passed, no unstaged changes exist, and GitHub access is available. If an open pull request already exists for the current branch against `main`, reuse it instead of creating a duplicate.

The pull request body must include summary, changed files or areas, verification commands and results, impact, risks, limitations, and confirmation that no merge was performed.

## Required Commands

Run or inspect output from:

```bash
git status --short --branch
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

Use the minimum and full quality commands documented in `QUALITY.md` when verification is required and practical. On Windows PowerShell, use `.\gradlew.bat` with the same arguments.

## Commit Message Contract

Allowed commit types:

```text
feat
fix
refactor
test
docs
build
ci
quality
agent
chore
```

Preferred format:

```text
<type>(<scope>): <short imperative summary>

Why:
- ...

What:
- ...

How:
- ...

Verification:
- ...

Impact:
- ...

Limitations:
- ...
```

Do not invent verification, affected components, risks, or limitations.

## Output Format

Return exactly this structure:

```text
Commit readiness: READY | NOT READY | BLOCKED

Changed files:
- <file>: <reason for change>

Scope assessment:
- <in scope / out of scope findings>

Verification:
- <commands executed>
- <pass/fail/not executed with reason>

Risks:
- <behavior, architecture, evidence, test, documentation, or dependency risks>

Required fixes before commit:
- <fixes or "None">

Proposed commit message:
<full commit message>
```

## Stop Conditions

Stop if:

- `AGENTS.md` cannot be read,
- `QUALITY.md` cannot be read,
- `Commit.md` cannot be read,
- `.agents/skills/commit-preparation/workflow.commit-preparation.md` cannot be read when using the reusable workflow,
- branch or worktree is unclear,
- staged and unstaged changes conflict,
- unexpected files exist,
- files cannot be classified,
- quality gates failed,
- quality gates were skipped without justification,
- sensitive data appears in the diff,
- generated artifacts appear unexpectedly,
- the commit message would require guessing.

## Forbidden Actions

Do not:

- modify files while acting as a commit reviewer,
- stage files without explicit commit-preparation authority,
- create commits unless the user or active workflow permits committing,
- push or create pull requests unless the user enters `push` or explicitly requests that action,
- commit unrelated files,
- commit generated build output,
- commit `.gradle/`,
- commit `build/`,
- commit IDE workspace metadata,
- commit temporary logs,
- commit local databases,
- commit local trace dumps,
- commit credentials or tokens,
- claim tests passed without execution evidence,
- approve a commit when required quality gates fail.

## Final Rule

Commit readiness is evidence-based. If scope, verification, file ownership, or commit-message content cannot be proven from repository state and executed commands, return `BLOCKED` or `NOT READY` instead of guessing.
