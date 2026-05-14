---
name: commit-preparation
description: Use when preparing, reviewing, validating, repairing, committing, pushing, creating a PR, or running `push auto` for Forensic Analytics changes. This skill is the commit-readiness workflow and enforces AGENTS.md, QUALITY.md, git diff inspection, task-scope validation, quality-gate verification, blocker routing, commit-message-preparation, PR creation on explicit `push`, and PR merge plus branch cleanup on explicit `push auto`.
---

# Commit Preparation Skill

## Goal

Prepare, review, repair, commit, push, and open a pull request for Forensic Analytics changes without weakening repository rules, mixing unrelated changes, or claiming unexecuted verification.

This skill does not replace repository rules. This skill applies repository rules.

`AGENTS.md` remains the source of truth for agent behavior, architecture rules, safety rules, and documentation ownership.

`QUALITY.md` remains the source of truth for quality gates, verification commands, coverage, dependency verification, and failure policy.

## Role Split

Use this skill with these Codex roles:

- `commit-message-preparation`: reusable skill for drafting and validating the proposed commit message from diff and verification evidence.
- `commit_reviewer`: read-only reviewer that classifies changes, checks scope, verifies evidence, and returns `READY`, `NOT READY`, or `BLOCKED`.
- `commit_operator`: mutating operator that may stage explicit files, commit, push, and create or complete a GitHub pull request only when the contract allows it. On exact `push auto`, it may also merge the verified pull request, delete the merged pull request's remote head branch, and invoke the clean workflow.

The reviewer must not modify files. The operator must not bypass the reviewer result, required verification, or the `push` command requirement.

## Required References

Read these files before deciding commit readiness:

```text
AGENTS.md
QUALITY.md
.agents/skills/commit-message-preparation/SKILL.md
.agents/skills/commit-preparation/workflow.commit-preparation.md
.agents/skills/clean/SKILL.md when the user enters `push auto`
active workflow.md or task-specific workflow if present
```

If any required reference cannot be read, stop and report the missing file.

## Command Execution Environment

Follow the command execution environment defined by `AGENTS.md` and `QUALITY.md`:

- On Windows hosts, run repository commands through WSL from the repository's WSL-mounted worktree path.
- On Linux hosts, run repository commands through native shell access.
- Use the Linux-style commands documented in this skill and in `QUALITY.md`, including `./gradlew`.
- If WSL Git reports broad unexpected line-ending-only changes, correct the local Git EOL configuration or stop and report before staging, committing, pushing, or reviewing commit readiness.
- If WSL is unavailable on a Windows host, or if the worktree cannot be reached from WSL, stop and report instead of silently using Windows-native commands.

## When to Use This Skill

Use this skill when:

- preparing a commit,
- reviewing staged or unstaged changes,
- validating commit readiness,
- writing a commit message,
- checking whether changed files belong to a task,
- documenting verification evidence before commit.

When only drafting or validating the commit message, use `.agents/skills/commit-message-preparation/SKILL.md`.

## Commit Readiness Workflow

### Phase 0: Preconditions

Verify the intended worktree, branch, task scope, repository rules, and local change ownership.

Stop when the branch, worktree, task, or ownership of local changes is unclear.

### Phase 1: Read Repository Rules

Read `AGENTS.md`, `QUALITY.md`, this skill, the commit-message-preparation skill, and any active task workflow.

Do not rely on remembered commands, package names, Gradle tasks, quality rules, or architecture boundaries.

### Phase 2: Inspect Git State

Inspect:

```bash
git status --short --branch
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

Inspect staged and unstaged changes separately.

### Phase 3: Classify Changed Files

Classify every changed file by purpose, ownership, and task relevance.

Unexpected, unrelated, sensitive, generated, or unclassified files block commit readiness.

### Phase 4: Scope Review

Verify why each file changed, which task requirement caused the change, and whether behavior, API, schema, evidence, graph, replay, LLM, report, test, dependency, documentation, or workflow semantics are affected.

### Phase 5: Verification

Use `QUALITY.md` for exact commands.

Run the narrowest meaningful verification first when applicable, then the required quality gate when practical.

Do not claim a command passed unless it actually passed.

### Phase 6: Blocker Routing and Repair

When commit readiness is `NOT READY` or `BLOCKED`, route only clear, in-scope fixes to the appropriate worker or reviewer:

- implementation defects: `implementation_worker`
- tests, coverage, dependency verification, or build failures: `quality_reviewer` or `test_archunit_reviewer`
- documentation inconsistencies: `documentation_reviewer`
- architecture boundary risks: `architecture_reviewer`
- security, credentials, tokens, or sensitive data: `security_reviewer`
- static source-analysis risks: `source_analysis_reviewer`
- ingestion or handoff risks: `ingestion_handoff_reviewer`
- Joern semantic artifact risks: `joern_semantics_reviewer`
- persistence or deterministic artifact risks: `analytics_persistence_reviewer`
- replay, graph, reporting, or LLM evidence-package risks: `replay_graph_llm_reviewer`
- commit-message defects: `commit-message-preparation`

After repair, rerun the commit-readiness review.

Do not repair speculative, out-of-scope, or unclear blockers. Report those instead.

### Phase 7: Staging

Stage only exact task-related files after the final diff has been reviewed.

Do not stage unrelated files, generated build output, `.gradle/`, `build/`, IDE metadata, temporary logs, local databases, local trace dumps, credentials, tokens, or generated reports unless explicitly required.

### Phase 8: Commit Message

Use `.agents/skills/commit-message-preparation/SKILL.md`.

The final message must come from the staged diff, task scope, reviewer findings, and executed verification evidence.

### Phase 9: Commit, Push, Pull Request, And Auto Merge

Create a commit only when readiness is `READY`, required verification passed or has an acceptable documented skip reason, all staged files are in scope, and the user or active workflow permits committing.

Push and create or complete a pull request only when the user enters exactly `push`, enters exactly `push auto`, or explicitly requests that action.

The pull request must target `main`.

Merge the pull request, delete its remote head branch, and run the clean workflow only when the user enters exactly `push auto`.

## Mandatory Behavior

You must:

- inspect repository rules before commit review,
- inspect staged and unstaged changes separately,
- classify every changed file,
- verify scope against the current task,
- identify generated, sensitive, local, or unrelated files,
- use `QUALITY.md` for verification commands,
- report skipped verification with a clear reason,
- use the commit-message-preparation skill for the proposed message,
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

## Push Auto Command Behavior

When the user enters exactly `push auto`, treat it as explicit permission to run the normal `push` workflow and then automatically finish the GitHub pull request lifecycle.

Execute this order:

1. Rerun commit readiness and verification.
2. Create the commit from the reviewed staged diff.
3. Push the current non-`main` branch to `origin`.
4. Create or reuse a GitHub pull request from the current branch against `main`.
5. Verify that the pull request head branch matches the current branch and the base branch is `main`.
6. Verify that GitHub reports the pull request as mergeable and that required checks are successful, or that no required checks are configured.
7. Merge the pull request through GitHub.
8. Re-fetch the pull request and verify `merged: true` before any branch deletion.
9. Delete only the merged pull request's remote head branch.
10. Run `.agents/skills/clean/SKILL.md` to switch to `main`, fast-forward it, and delete the local merged branch.

Do not treat `push auto` as permission to force-push, push directly to `main`, retarget the pull request, bypass failed or pending checks, merge an unrelated pull request, delete `main`, or delete a branch before the pull request is verified as merged.

If GitHub mergeability, required-check status, merge result, remote branch deletion, or clean execution cannot be verified, stop and report the exact blocker.

## Required Commands

Run or inspect output from:

```bash
git status --short --branch
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

For `push auto`, also verify the relevant GitHub pull request state and run the command required by the clean skill after the merge.

Use the minimum and full quality commands documented in `QUALITY.md` when verification is required and practical.

## Commit Message Contract

Use `.agents/skills/commit-message-preparation/SKILL.md` to draft or validate the proposed commit message.

Do not invent verification, affected components, risks, limitations, type, or scope.

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

Recommended remediation:
- <role or skill to address each blocker, or "None">

Proposed commit message:
<full commit message>
```

## Stop Conditions

Stop if:

- `AGENTS.md` cannot be read,
- `QUALITY.md` cannot be read,
- `.agents/skills/commit-message-preparation/SKILL.md` cannot be read when drafting the commit message,
- `.agents/skills/commit-preparation/workflow.commit-preparation.md` cannot be read when using the reusable workflow,
- `.agents/skills/clean/SKILL.md` cannot be read when the user enters `push auto`,
- branch or worktree is unclear,
- staged and unstaged changes conflict,
- unexpected files exist,
- files cannot be classified,
- quality gates failed,
- quality gates were skipped without justification,
- sensitive data appears in the diff,
- generated artifacts appear unexpectedly,
- the commit message would require guessing.
- `push auto` is requested but pull request mergeability, required checks, merge result, branch deletion target, or clean execution cannot be verified.

## Forbidden Actions

Do not:

- modify files while acting as a commit reviewer,
- stage files without explicit commit-preparation authority,
- create commits unless the user or active workflow permits committing,
- push or create pull requests unless the user enters `push`, enters `push auto`, or explicitly requests that action,
- merge pull requests unless the user enters exactly `push auto`,
- delete remote branches unless the user enters exactly `push auto` and the pull request was verified as merged,
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
