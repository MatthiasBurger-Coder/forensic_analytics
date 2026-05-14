# Workflow: Commit Preparation and Commit Review

## Purpose

Describe how Codex prepares, reviews, verifies, and decides commit readiness in the Forensic Analytics repository.

Use the git-commit-preparation skill.
Use the git-commit-message-preparation skill for the proposed commit message.
Use the git_commit_reviewer worker/subagent for read-only review.
Use the git_commit_operator worker/subagent for commit, push, and GitHub pull-request execution.
Use the git-clean skill after a successful `push auto` merge.
Do not commit if the commit reviewer returns NOT READY or BLOCKED.
Do not commit if required quality gates fail.
Do not push or create a pull request unless the user enters `push`, enters `push auto`, or explicitly requests that action.
Do not merge a pull request, delete a remote branch, or run post-merge cleanup unless the user enters exactly `push auto`.

## Inputs

- Current task or issue context.
- Current Git worktree.
- `AGENTS.md`.
- `QUALITY.md`.
- `.agents/skills/git-commit-preparation/SKILL.md`.
- `.agents/skills/git-commit-message-preparation/SKILL.md`.
- `.agents/skills/git-clean/SKILL.md` when `push auto` is requested.
- `.agents/skills/git-commit-preparation/workflow.git-commit-preparation.md`.
- `.codex/agents/git_commit_reviewer.toml`.
- `.codex/agents/git_commit_operator.toml`.
- Relevant workflow or task-specific prompt when present.

## Preconditions

- Work only in the current worktree.
- Verify the current branch.
- On Windows hosts, execute repository commands through WSL from the repository's WSL-mounted worktree path.
- On Linux hosts, execute repository commands through native shell access.
- Stop and report if WSL is unavailable on Windows or cannot access the worktree.
- Do not commit while detached.
- Do not modify unrelated files.
- Do not stage or commit generated output, local runtime data, credentials, or IDE metadata.
- The reviewer is not allowed to modify files.

## Phase 1: Read Rules

Read, in order:

```text
1. AGENTS.md
2. QUALITY.md
3. .agents/skills/git-commit-preparation/SKILL.md
4. .agents/skills/git-commit-message-preparation/SKILL.md
5. .agents/skills/git-commit-preparation/workflow.git-commit-preparation.md
6. .codex/agents/git_commit_reviewer.toml
7. .codex/agents/git_commit_operator.toml
8. .agents/skills/git-clean/SKILL.md when `push auto` is requested
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

Ask the `git_commit_reviewer` worker/subagent to perform a read-only commit-readiness review.

The reviewer must:

- read `AGENTS.md`, `QUALITY.md`, `.agents/skills/git-commit-preparation/SKILL.md`, and `.agents/skills/git-commit-message-preparation/SKILL.md`,
- read `.agents/skills/git-commit-preparation/workflow.git-commit-preparation.md` when using the reusable workflow,
- inspect status and diffs,
- classify every changed file,
- check task scope,
- check verification evidence,
- return the output contract defined in `.agents/skills/git-commit-preparation/SKILL.md`.

The reviewer must not modify files, stage files, create commits, or push branches.

## Phase 3a: Run Commit Operator Only For Mutations

Use `git_commit_operator` only for mutating GitHub workflow steps:

- staging explicit files,
- creating the commit,
- pushing the current branch,
- creating or completing a GitHub pull request against `main`.
- merging the pull request only when the user enters exactly `push auto`,
- deleting the merged pull request's remote head branch only after merge verification,
- invoking the git-clean workflow only after the pull request is verified as merged.

The operator must follow this workflow and the git-commit-preparation skill. It must not proceed unless the reviewer output is `READY` or it has reproduced the same output contract from repository evidence.

## Phase 4: Review Findings

Review the commit reviewer output.

If the reviewer returns `BLOCKED`, stop and report the blocker.

If the reviewer returns `NOT READY`, fix only clear, in-scope blockers or stop and report why the fixes cannot be made safely.

Do not continue to commit preparation while unrelated files, sensitive data, generated artifacts, or unclassified files remain.

## Phase 5: Route Blockers To Repair Agents

When a blocker is clear and in scope, route it to the appropriate role:

- `implementation_worker` for implementation defects.
- `quality_reviewer` or `quality_archunit_reviewer` for tests, coverage, dependency verification, or build failures.
- `documentation_reviewer` for documentation drift.
- `architecture_reviewer` for architecture-boundary risks.
- `security_reviewer` for credentials, tokens, or sensitive data.
- `source_analysis_reviewer` for static source-analysis risks.
- `ingestion_handoff_reviewer` for ingestion or handoff contract risks.
- `joern_semantics_reviewer` for Joern semantic artifact risks.
- `analytics_persistence_reviewer` for persistence or deterministic artifact risks.
- `replay_graph_llm_reviewer` for replay, graph, reporting, or LLM evidence-package risks.
- `git-commit-message-preparation` for commit-message defects.

After repair, rerun `git_commit_reviewer`.

Do not route speculative or unclear blockers to mutation workers. Report those instead.

## Phase 6: Run Required Verification

Use `QUALITY.md` for verification commands.

The current minimum quality command is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The current full local quality gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

For documentation-only and agent-instruction-only changes, run the minimum quality command when practical. If it is skipped, report the reason and do not claim it passed.

## Phase 7: Stage Explicit Files

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

## Phase 8: Final Diff Review

After staging, run:

```bash
git diff --cached --stat
git diff --cached
```

Review the staged diff before committing.

## Phase 9: Prepare Commit Message

Prepare the commit message from the final staged diff and actual verification evidence.

Use `.agents/skills/git-commit-message-preparation/SKILL.md`.

Do not invent verification results, affected components, or impact.

## Phase 10: Commit Decision

Commit only if:

- all required gates pass or skipped gates have acceptable documented reasons,
- the reviewer returned `READY`,
- all staged files are in scope,
- the staged diff was reviewed,
- the commit message is traceable,
- the user or active workflow permits committing.

Do not push or create a pull request during this phase.

## Phase 11: Push Command and GitHub Pull Request

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
- `git_commit_operator` is the role performing the mutating Git and GitHub actions.

The pull request title must come from the final commit message title.

The pull request body must include:

- summary,
- changed files or areas,
- verification commands and results,
- impact,
- risks and limitations,
- explicit note that no push to `main` or merge was performed.

Do not force-push. Do not merge. Do not enable auto-merge. Do not retarget the pull request away from `main`.

## Phase 12: Push Auto Merge, Branch Deletion, And Cleanup

When the user enters exactly `push auto`, treat it as permission to complete publication and post-merge cleanup:

1. Run the full Phase 11 push workflow.
2. Verify that the pull request head branch is the current branch and the base branch is `main`.
3. Verify GitHub reports the pull request as mergeable.
4. Verify required checks are successful, or that no required checks are configured.
5. Merge the pull request through GitHub.
6. Re-fetch the pull request and verify `merged: true` before deleting any branch.
7. Delete only the merged pull request's remote head branch.
8. Run `.agents/skills/git-clean/SKILL.md`.
9. Report the merge commit, remote branch deletion result, clean result, and final `main` status.

For `push auto`, the pull request body must include the same content as Phase 11 plus an explicit note that the workflow intends to merge the PR, delete the merged PR head branch, and run clean after merge verification.

Do not merge when checks failed, checks are pending, mergeability is unknown, GitHub access is unavailable, the PR does not target `main`, or the PR head branch differs from the current branch.

Do not delete a branch until the PR is verified as merged.

Do not delete `main`, force-delete branches, push directly to `main`, enable GitHub auto-merge, or retarget the PR.

## Stop Conditions

Stop and report when:

- `AGENTS.md`, `QUALITY.md`, or the git-commit-preparation skill cannot be read,
- `.agents/skills/git-commit-message-preparation/SKILL.md` cannot be read when drafting the commit message,
- `.agents/skills/git-commit-preparation/workflow.git-commit-preparation.md` cannot be read when using the reusable workflow,
- `.agents/skills/git-clean/SKILL.md` cannot be read when `push auto` is requested,
- `.codex/agents/git_commit_operator.toml` cannot be read when commit, push, or pull-request execution is requested,
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
- the user requested `push auto` but mergeability, required-check status, merge result, remote branch deletion target, or clean execution cannot be verified.

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

Recommended remediation:
- <role or skill to address each blocker, or "None">

Proposed commit message:
<full commit message>
```

Required execution order:

```text
1. Read AGENTS.md
2. Read QUALITY.md
3. Read .agents/skills/git-commit-preparation/SKILL.md
4. Read .agents/skills/git-commit-message-preparation/SKILL.md
5. Read .agents/skills/git-commit-preparation/workflow.git-commit-preparation.md
6. Read .codex/agents/git_commit_reviewer.toml
7. Read .codex/agents/git_commit_operator.toml when mutating execution is requested
8. Read .agents/skills/git-clean/SKILL.md when `push auto` is requested
9. Inspect git status and diffs
10. Ask git_commit_reviewer to review commit readiness
11. Route only clear, in-scope blockers to the appropriate repair role
12. Rerun git_commit_reviewer after repairs
13. Run required verification from QUALITY.md
14. Inspect final staged diff
15. Prepare commit message with git-commit-message-preparation
16. Use git_commit_operator to commit only if all required gates pass and user/task permits committing
17. On `push`, use git_commit_operator to push the branch and create or complete a GitHub pull request against main
18. On `push auto`, use git_commit_operator to push, create or reuse the PR, verify mergeability and checks, merge the PR, verify merged state, delete the remote head branch, and run clean
```
