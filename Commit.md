# Commit.md — Commit Preparation and Commit Quality Rules

## Purpose

This document defines the mandatory commit preparation workflow for the **Forensic Analytics** repository.

It turns the repository rules from `AGENTS.md` and the quality contract from `QUALITY.md` into a concrete commit-readiness process for humans, Codex, and Codex subagents.

A commit is acceptable only when the changed files are task-related, reviewed, verified, and documented with a traceable commit message.

## Authority and Scope

`AGENTS.md` remains the primary source of truth for agent behavior, architecture boundaries, safety rules, testing expectations, and final reporting.

`QUALITY.md` remains the authoritative source of truth for verification commands, quality gates, coverage requirements, dependency verification, and failure policy.

The repository authority model is:

```text
AGENTS.md = primary source of truth for agent behavior and architecture rules
QUALITY.md = authoritative source of truth for verification and quality gates
Commit.md = commit preparation and commit-readiness workflow
```

This document does not replace either file. It defines how their rules are applied immediately before staging, committing, pushing, or opening a pull request.

The reusable Codex commit-preparation skill is:

```text
.agents/skills/commit-preparation/SKILL.md
```

The reusable Codex execution workflow for this contract is:

```text
.agents/skills/commit-preparation/workflow.commit-preparation.md
```

The Codex agent roles for this contract are:

```text
.codex/agents/commit_reviewer.toml = read-only readiness reviewer
.codex/agents/commit_operator.toml = mutating commit, push, and pull-request operator
```

This document applies to:

- implementation commits,
- test commits,
- documentation commits,
- refactoring commits,
- build or CI commits,
- agent workflow commits,
- quality-gate fixes,
- repository structure changes.

## Commit Principle

Prefer one small, coherent, reviewable commit.

A commit must contain only changes that belong to the same task, defect, feature slice, quality-gate fix, documentation correction, or architecture decision.

Do not mix unrelated cleanup, opportunistic refactoring, formatting-only changes, generated artifacts, local tooling files, or temporary output into a task commit.

## Codex Role Model

The commit process is intentionally split into three layers:

1. `Commit.md` defines the repository contract.
2. `.agents/skills/commit-preparation/SKILL.md` and `.agents/skills/commit-preparation/workflow.commit-preparation.md` apply the contract as a reusable Codex skill.
3. `.codex/agents/commit_reviewer.toml` and `.codex/agents/commit_operator.toml` divide review and execution responsibilities.

`commit_reviewer` is read-only. It may inspect rules, status, diffs, and verification evidence. It must not modify files, stage files, commit, push, or create pull requests.

`commit_operator` is mutating. It may stage explicit files, create a commit, push a branch, and create or complete a GitHub pull request only when the contract allows it. It must use the commit-preparation skill and must not proceed unless commit readiness is `READY`.

The preferred flow is:

```text
Commit.md contract
-> commit-preparation skill and workflow
-> commit_reviewer read-only readiness review
-> commit_operator execution only after READY
-> push and GitHub pull request only when the user enters push
```

## Mandatory Commit Workflow

### Phase 0: Preconditions

Before preparing a commit, verify:

1. The work is happening in the intended Git worktree.
2. The branch is the intended feature, fix, hotfix, release, or workflow branch.
3. The branch is not `main` unless the task explicitly permits direct changes to `main`.
4. The requested task is understood from explicit user input, an issue, a workflow document, or a project decision.
5. `AGENTS.md` and `QUALITY.md` have been read.
6. No unrelated local files are mixed into the working tree.

If any prerequisite is unclear, stop and report before staging or committing.

### Phase 1: Read Repository Rules

Before inspecting the final diff, read or re-check:

- `AGENTS.md`,
- `QUALITY.md`,
- this `Commit.md`,
- `.agents/skills/commit-preparation/SKILL.md`,
- `.agents/skills/commit-preparation/workflow.commit-preparation.md`,
- the active `workflow.md` or task-specific workflow if present,
- task-specific design notes, ADRs, or issue descriptions when they are part of the task.

Do not rely on remembered command names, task names, package names, quality rules, or architecture boundaries.

### Phase 2: Inspect Git State

Run and inspect:

```bash
git status --short --branch
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

If there are both staged and unstaged changes, inspect them separately.

Do not assume that staged changes are correct. Re-check staged content before committing.

### Phase 3: Classify Changed Files

Every changed file must be classified as one of:

- production source code,
- test source code,
- build or CI configuration,
- documentation,
- agent instruction or workflow file,
- fixture or sample data,
- generated output intentionally required by the task,
- unexpected or unrelated local file.

Unexpected or unrelated files must not be committed.

If a file cannot be classified confidently, stop and report.

### Phase 4: Scope Review

For every changed file, verify:

1. Why the file changed.
2. Which task requirement, defect, quality failure, or project decision required the change.
3. Whether the change affects domain, application, adapter, infrastructure, build, tests, documentation, or agent workflow behavior.
4. Whether public API, schema, graph labels, replay behavior, LLM prompts, report output, or runtime evidence semantics changed.
5. Whether documentation and tests were updated when behavior changed.

Remove or report files that do not belong to the task.

### Phase 5: Verification Before Commit

Run the narrowest meaningful verification first when applicable, then run the repository quality gate required by `QUALITY.md`.

The minimum verification command is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The full local quality gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

On Windows PowerShell, use `.\gradlew.bat` with the same arguments.

When Gradle plugin metadata, task inputs, task outputs, or plugin declarations change, also run:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

If Sonar credentials are available and the task requires or benefits from it, optionally run:

```bash
./gradlew sonar --dependency-verification strict --console=plain --stacktrace
```

Do not claim that a command passed unless it was actually executed.

If the full quality gate cannot be executed, report exactly why it could not be executed and which narrower checks were executed instead.

### Phase 6: Failure Handling

If any verification command fails:

1. Record the exact command.
2. Identify the failing task, test class, test method, assertion, exception, or dependency verification error where available.
3. Determine whether the failure was introduced by the current change, appears pre-existing, or is environment-related.
4. Fix failures caused by the current change when the cause is clear and within scope.
5. Rerun the failing command first.
6. Rerun the full local quality gate after fixing.
7. If the failure cannot be fixed safely, stop and report.

Do not commit when a required quality gate fails.

Do not weaken tests, coverage thresholds, dependency verification, architecture rules, evidence semantics, or replay integrity to make the gate pass.

### Phase 7: Staging Rules

Stage only exact task-related files.

Prefer explicit file staging:

```bash
git add path/to/file1 path/to/file2
```

Avoid broad staging unless the diff has been fully reviewed:

```bash
git add .
```

Before committing, rerun:

```bash
git diff --cached --stat
git diff --cached
```

Do not stage or commit:

- `.gradle/`,
- `build/`,
- IDE workspace metadata,
- temporary logs,
- local databases,
- local trace dumps,
- credentials,
- tokens,
- generated reports unless explicitly required,
- unrelated formatting changes,
- unrelated local files.

### Phase 8: Commit Message Requirements

The commit message must document:

1. What changed.
2. Why it changed.
3. How it changed.
4. Which files, layers, or components were affected.
5. Whether bugs were fixed.
6. Whether new behavior was introduced.
7. Whether refactoring, cleanup, structural, or architectural changes were made.
8. Whether tests were added or adjusted.
9. Whether behavior-relevant, evidence-relevant, schema-relevant, API-relevant, replay-relevant, graph-relevant, report-relevant, or LLM-relevant changes exist.
10. Which verification commands were executed.
11. Which limitations or blockers remain, if any.

Do not create vague commit messages such as:

```text
update files
fix stuff
cleanup
changes
wip
```

### Phase 9: Push Command and GitHub Pull Request

The prompt command `push` is explicit permission to complete the commit publication workflow.

When the user enters exactly `push`, Codex may:

1. rerun this commit-readiness workflow,
2. create the commit from the reviewed staged diff,
3. push the current non-`main` branch to `origin`,
4. create or complete a GitHub pull request from the current branch against `main`.

The `push` command is not permission to:

- push directly to `main`,
- force-push,
- merge a pull request,
- enable auto-merge,
- bypass a failed quality gate,
- include unrelated files,
- create a pull request against any branch other than `main`.

Before pushing, Codex must verify:

- `Commit.md`, `AGENTS.md`, and `QUALITY.md` were read,
- `.agents/skills/commit-preparation/SKILL.md` was read,
- `.agents/skills/commit-preparation/workflow.commit-preparation.md` was read when using the reusable Codex workflow,
- `commit_reviewer` returned `READY` or an equivalent readiness review was completed from this contract,
- `commit_operator` is the only Codex agent performing the mutating push and pull-request actions,
- the current branch is not `main`,
- the branch and upstream state are clear,
- no unstaged changes exist,
- the staged diff was inspected,
- commit readiness is `READY`,
- required verification passed,
- `origin/main` exists after fetching from `origin`,
- GitHub access is available for creating or updating the pull request.

If the current branch already has an open GitHub pull request against `main`, Codex should reuse it and update or report the existing pull request instead of creating a duplicate.

The pull request must be complete. Its title and body must be derived from the final commit message and actual verification evidence.

The pull request body must include:

- summary,
- changed files or areas,
- verification commands and results,
- impact,
- risks and limitations,
- explicit note that no push to `main` or merge was performed.

If commit creation succeeds but push or pull-request creation fails, Codex must stop and report the exact failing command or GitHub operation, the branch, the commit hash when available, and the smallest safe next step.

## Preferred Commit Message Format

Use this structure unless the task explicitly requires another format:

```text
<type>(<scope>): <short imperative summary>

Why:
- <reason for the change>

What:
- <main change 1>
- <main change 2>

How:
- <implementation approach>
- <important design or safety decision>

Verification:
- <command executed and result>
- <command executed and result>

Impact:
- <affected layer/component/API/schema/evidence behavior>
- <breaking changes or behavior-relevant changes, or "None">

Limitations:
- <known limitation, blocker, skipped optional check, or "None">
```

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

## Commit Readiness Checklist

A commit is ready only when all answers are yes:

- [ ] The branch and worktree are correct.
- [ ] `AGENTS.md`, `QUALITY.md`, and this `Commit.md` were considered.
- [ ] `.agents/skills/commit-preparation/SKILL.md` was considered when Codex prepares the commit.
- [ ] `.agents/skills/commit-preparation/workflow.commit-preparation.md` was considered when Codex runs the reusable workflow.
- [ ] `git status`, `git diff`, and `git diff --cached` were inspected.
- [ ] Every changed file is task-related.
- [ ] Unexpected files were removed from the commit or reported.
- [ ] Tests were added or updated when behavior changed.
- [ ] Documentation was updated when public behavior changed.
- [ ] Evidence, replay, graph, LLM, and report semantics remain explicit and traceable.
- [ ] The required quality gate was executed successfully, or an explicit blocker was reported.
- [ ] The commit message documents what, why, how, verification, impact, and limitations.

## Commit Reviewer Output Contract

A commit reviewer, human or agentic, must report:

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
- <behavior, architecture, evidence, test, or documentation risks>

Required fixes before commit:
- <fixes or "None">

Proposed commit message:
<full commit message>
```

The reviewer must not approve a commit when required verification failed, was skipped without justification, or when unrelated files are present.

## Stop Conditions

Stop and report before committing if:

- the branch or worktree is unclear,
- files are changed outside the requested scope,
- required classes, methods, tasks, schemas, graph labels, or event fields cannot be verified,
- documentation and code disagree in a behavior-relevant way,
- quality commands fail,
- tests are missing for behavior-relevant fixes and no clear justification exists,
- dependency verification fails,
- credentials or sensitive data appear in the diff,
- generated artifacts appear unexpectedly,
- the commit message would need to guess why a change exists.

This document explicitly forbids:

- vague commit messages,
- committing unrelated files,
- committing generated build output,
- committing `.gradle/`,
- committing `build/`,
- committing IDE workspace metadata,
- committing temporary logs,
- committing local databases,
- committing local trace dumps,
- committing credentials or tokens,
- claiming tests passed without execution evidence,
- committing when required quality gates fail.

## Final Rule

A commit is not a dump of modified files.

A commit is a reviewed, verified, traceable engineering decision.
