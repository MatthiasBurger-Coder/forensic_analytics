# Workflow: Commit Preparation and Commit Review

## Purpose

Describe how Codex prepares, reviews, verifies, and decides commit readiness in the Forensic Analytics repository.

Use the commit-preparation skill.
Use the commit_reviewer worker/subagent for read-only review.
Use the commit_operator worker/subagent for commit, push, and GitHub pull-request execution.
Do not commit if the commit reviewer returns NOT READY or BLOCKED.
Do not commit if required quality gates fail.
Do not push or create a pull request unless the user enters `push` or explicitly requests that action.

## Inputs

- Current task or issue context.
- Current Git worktree.
- `AGENTS.md`.
- `QUALITY.md`.
- `Commit.md`.
- `.agents/skills/commit-preparation/SKILL.md`.
- `.agents/skills/commit-preparation/workflow.commit-preparation.md`.
- `.codex/agents/commit_reviewer.toml`.
- `.codex/agents/commit_operator.toml`.
- Relevant workflow or task-specific prompt when present.

## Preconditions

- Work only in the current worktree.
- Verify the current branch.
- Do not commit while detached.
- Do not modify unrelated files.
- Do not stage or commit generated output, local runtime data, credentials, or IDE metadata.
- The reviewer is not allowed to modify files.

## Phase 1: Read Rules

Read, in order:

```text
1. AGENTS.md
2. QUALITY.md
3. Commit.md
4. .agents/skills/commit-preparation/SKILL.md
5. .agents/skills/commit-preparation/workflow.commit-preparation.md
6. .codex/agents/commit_reviewer.toml
7. .codex/agents/commit_operator.toml
```

Also read the active `workflow.md` or task-specific workflow if present.

## Phase 2: Inspect Git State

Run:

```bash
git status --short --branch
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

Classify staged and unstaged changes separately.

## Phase 3: Run Commit Reviewer

Ask the `commit_reviewer` worker/subagent to perform a read-only commit-readiness review.

The reviewer must:

- read `AGENTS.md`, `QUALITY.md`, `Commit.md`, and `.agents/skills/commit-preparation/SKILL.md`,
- read `.agents/skills/commit-preparation/workflow.commit-preparation.md` when using the reusable workflow,
- inspect status and diffs,
- classify every changed file,
- check task scope,
- check verification evidence,
- return the output contract defined in `Commit.md`.

The reviewer must not modify files, stage files, create commits, or push branches.

## Phase 3a: Run Commit Operator Only For Mutations

Use `commit_operator` only for mutating GitHub workflow steps:

- staging explicit files,
- creating the commit,
- pushing the current branch,
- creating or completing a GitHub pull request against `main`.

The operator must follow `Commit.md`, this workflow, and the commit-preparation skill. It must not proceed unless the reviewer output is `READY` or it has reproduced the same output contract from repository evidence.

## Phase 4: Review Findings

Review the commit reviewer output.

If the reviewer returns `BLOCKED`, stop and report the blocker.

If the reviewer returns `NOT READY`, fix only clear, in-scope blockers or stop and report why the fixes cannot be made safely.

Do not continue to commit preparation while unrelated files, sensitive data, generated artifacts, or unclassified files remain.

## Phase 5: Run Required Verification

Use `QUALITY.md` for verification commands.

The current minimum quality command is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The current full local quality gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

On Windows PowerShell, use `.\gradlew.bat` with the same arguments.

For documentation-only and agent-instruction-only changes, run the minimum quality command when practical. If it is skipped, report the reason and do not claim it passed.

## Phase 6: Stage Explicit Files

Stage only files that are required for the current task.

Do not stage:

- unrelated files,
- generated build output,
- `.gradle/`,
- `build/`,
- IDE workspace metadata,
- temporary logs,
- local databases,
- local trace dumps,
- credentials, tokens, or private local configuration.

## Phase 7: Final Diff Review

After staging, run:

```bash
git diff --cached --stat
git diff --cached
```

Review the staged diff before committing.

## Phase 8: Prepare Commit Message

Prepare the commit message from the final staged diff and actual verification evidence.

Use the format from `Commit.md`:

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

Do not invent verification results, affected components, or impact.

## Phase 9: Commit Decision

Commit only if:

- all required gates pass or skipped gates have acceptable documented reasons,
- the reviewer returned `READY`,
- all staged files are in scope,
- the staged diff was reviewed,
- the commit message is traceable,
- the user or active workflow permits committing.

Do not push or create a pull request during this phase.

## Phase 10: Push Command and GitHub Pull Request

When the user enters exactly `push`, treat it as permission to complete publication:

1. rerun this workflow,
2. create the commit from the reviewed staged diff,
3. push the current non-`main` branch to `origin`,
4. create or complete a GitHub pull request from the current branch against `main`.

Before pushing, verify:

- the current branch is not `main`,
- no unstaged changes exist,
- commit readiness is `READY`,
- required verification passed,
- `origin/main` exists after fetching from `origin`,
- GitHub access is available,
- no duplicate open pull request will be created for the same branch and base.
- `commit_operator` is the role performing the mutating Git and GitHub actions.

The pull request title must come from the final commit message title.

The pull request body must include:

- summary,
- changed files or areas,
- verification commands and results,
- impact,
- risks and limitations,
- explicit note that no push to `main` or merge was performed.

Do not force-push. Do not merge. Do not enable auto-merge. Do not retarget the pull request away from `main`.

## Stop Conditions

Stop and report when:

- `AGENTS.md`, `QUALITY.md`, `Commit.md`, or the commit-preparation skill cannot be read,
- `.agents/skills/commit-preparation/workflow.commit-preparation.md` cannot be read when using the reusable workflow,
- `.codex/agents/commit_operator.toml` cannot be read when commit, push, or pull-request execution is requested,
- branch or worktree state is unclear,
- staged and unstaged changes conflict,
- files cannot be classified,
- unrelated files are present,
- generated artifacts appear unexpectedly,
- sensitive data appears in the diff,
- required quality gates fail,
- quality gates were skipped without justification,
- the commit reviewer returns `NOT READY` or `BLOCKED`,
- the commit message would require guessing,
- the user requested `push` but GitHub pull request creation is unavailable or would require guessing.

## Output

Return:

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

Required execution order:

```text
1. Read AGENTS.md
2. Read QUALITY.md
3. Read Commit.md
4. Read .agents/skills/commit-preparation/SKILL.md
5. Read .agents/skills/commit-preparation/workflow.commit-preparation.md
6. Read .codex/agents/commit_reviewer.toml
7. Read .codex/agents/commit_operator.toml when mutating execution is requested
8. Inspect git status and diffs
9. Ask commit_reviewer to review commit readiness
10. Fix only clear, in-scope blockers
11. Run required verification from QUALITY.md
12. Inspect final staged diff
13. Prepare commit message
14. Use commit_operator to commit only if all required gates pass and user/task permits committing
15. On `push`, use commit_operator to push the branch and create or complete a GitHub pull request against main
```
