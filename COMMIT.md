# Codex Commit Workflow - `forensic_analytics`

## Purpose

This file defines the commit workflow for the **Forensic Analytics** repository.

Use this workflow when Codex is asked to inspect, fix, commit, push, or clean up branches in this repository. The workflow is intentionally worktree-aware because this project may be open in multiple local worktrees at the same time.

The goal is to:

1. inspect the repository and all relevant worktrees,
2. respect `AGENTS.md` and `QUALITY.md`,
3. create or use a safe branch for the current worktree,
4. run the correct quality gates,
5. review the exact final diff,
6. stage only relevant files,
7. create a precise commit message from the actual diff,
8. push the branch safely,
9. and clean up local branches only when no worktree still uses them.

---

## Project Context

This repository contains the Forensic Analytics platform: a Java 25 / JUnit 6 multi-module Gradle project for evidence-first forensic analysis.

Core areas include:

- domain and application models for forensic analysis,
- ingestion of static, semantic, runtime, and diagnostic evidence,
- repository source adapters,
- Joern Docker semantic analysis adapters,
- persistence adapters,
- CLI and bootstrap entry points,
- graph, replay, report, and LLM-oriented future work,
- architecture, quality, coverage, and dependency-verification checks.

Do not copy assumptions from producer or tooling repositories into this repository. Build-tool plugins may produce evidence for Analytics, but this repository is the central analysis platform and must preserve evidence integrity, deterministic output, and clear uncertainty handling.

---

## Instruction Precedence

Before modifying, committing, or pushing, inspect the repository root for project-level instruction files.

Apply this precedence order:

1. `AGENTS.md`
2. `QUALITY.md`
3. `.github/workflows/*` if present
4. `settings.gradle.kts`
5. `build.gradle.kts`
6. `gradle/verification-metadata.xml`
7. this `COMMIT.md`
8. general assumptions or heuristics

Rules:

- `AGENTS.md` is the primary source of truth for agent behavior, architecture boundaries, stop conditions, commit rules, and repository safety rules.
- `QUALITY.md` is the binding quality contract.
- CI workflow files, when present, are the remote pipeline source of truth.
- If this file conflicts with `AGENTS.md` or `QUALITY.md`, stop and follow the higher-priority document.
- Do not guess missing commands, symbols, tasks, branches, or repository contracts.

---

## Worktree-Aware Git Rules

Git will not allow a local branch to be deleted while any worktree is checked out on that branch. This is expected and must be handled deliberately.

### Required Worktree Inspection

Always run these commands before switching, deleting, or pushing branches:

```powershell
git status --short --branch
git branch --show-current
git worktree list --porcelain
git branch -vv
git remote -v
```

Record:

- current branch or detached state,
- whether the current worktree is clean,
- whether `main` is already checked out in another worktree,
- whether the intended branch is already used by another worktree,
- whether the remote branch exists,
- whether the current branch tracks a remote branch.

### Branch Creation From A Detached Worktree

If the current worktree is detached, do not commit while detached.

Create a branch first:

```powershell
git switch -c codex/<short-task-slug>
```

Use the repository branch prefix `codex/` unless the user explicitly requests a different branch name.

If that branch name already exists, inspect where it is used:

```powershell
git branch -vv
git worktree list --porcelain
```

Choose a different `codex/` branch name rather than stealing a branch that another worktree is using.

### When `main` Is Used By Another Worktree

If `git switch main` fails with a message like:

```text
fatal: 'main' is already used by worktree at '<path>'
```

do not force anything and do not remove the other worktree.

Use one of these safe options:

```powershell
git switch --detach origin/main
```

or create a new work branch from the current detached `origin/main`:

```powershell
git switch -c codex/<short-task-slug>
```

Use the separate `main` worktree for direct `main` work only if the user explicitly asks and the worktree is clean.

### Local Branch Cleanup After PR Merge

After the user reports that a PR was merged and the remote branch was deleted:

1. Fetch and prune:

```powershell
git fetch --prune origin
```

2. If the current worktree is still on the merged branch, leave it first.

If `main` is available in this worktree:

```powershell
git switch main
git pull --ff-only
```

If `main` is already used by another worktree:

```powershell
git switch --detach origin/main
```

3. Confirm the branch is merged or contained in `origin/main`:

```powershell
git branch --contains <branch-name>
git log --oneline --decorate -n 5
```

4. Delete the local branch only after no worktree uses it:

```powershell
git branch -d <branch-name>
```

If Git refuses deletion because the branch is used by a worktree, inspect `git worktree list --porcelain`, switch that worktree away from the branch, and retry.

Do not use `git branch -D` unless the user explicitly asks for forced deletion and the branch contents have been verified as safely merged or intentionally discarded.

---

## Repository Inspection

Start in the repository root. Run and inspect:

```powershell
pwd
git rev-parse --show-toplevel
git status --short --branch
git diff --name-status
git diff --cached --name-status
git worktree list --porcelain
```

Inspect these files when present:

```text
AGENTS.md
QUALITY.md
settings.gradle.kts
build.gradle.kts
gradle/wrapper/gradle-wrapper.properties
gradle/verification-metadata.xml
.github/workflows/*.yml
.github/workflows/*.yaml
```

Determine and record:

1. whether this is a single-module or multi-module Gradle build,
2. which modules are included,
3. whether the Gradle wrapper exists,
4. which Java version is active,
5. which Gradle version the wrapper declares,
6. which quality commands `QUALITY.md` documents,
7. which CI workflows exist, if any,
8. whether staged changes already existed,
9. whether unrelated user changes are present,
10. whether another worktree owns `main` or the current task branch.

Do not modify files before this inspection is complete unless a higher-priority repository document explicitly instructs otherwise.

---

## Read Repository Rules

### `AGENTS.md`

Read `AGENTS.md` before committing. Extract and follow:

- architecture rules,
- evidence integrity rules,
- stop-and-report rules,
- file-writing rules,
- commit rules,
- branch or push restrictions,
- testing and quality expectations.

### `QUALITY.md`

Read `QUALITY.md` before committing. Extract and follow:

- minimum quality command,
- full local quality gate,
- coverage expectations,
- dependency-verification expectations,
- SonarCloud expectations,
- architecture and test expectations.

### CI Workflows

If `.github/workflows` exists, inspect all workflow YAML files and record:

- workflow names,
- triggers,
- Java setup version,
- Gradle commands,
- dependency verification behavior,
- SonarCloud steps,
- publishing steps,
- whether the local quality gate covers the CI checks.

If no CI workflows exist in this worktree, report that explicitly. Do not invent CI behavior.

---

## Toolchain Verification

Before quality checks, verify:

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat tasks --all --console=plain
```

Unix-like shell equivalents:

```bash
java -version
./gradlew --version
./gradlew tasks --all --console=plain
```

Rules:

- Java must be Java 25 unless repository-level files define a stricter compatible baseline.
- Use the Gradle wrapper.
- Do not replace the wrapper.
- Do not upgrade dependencies, plugins, Java, Gradle, JaCoCo, graph libraries, or LLM SDKs as part of commit preparation unless the task explicitly requires it.

---

## Quality Gates Before Commit

Run the documented `QUALITY.md` minimum command first:

```powershell
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
```

Then run the full local quality gate documented in `QUALITY.md`:

```powershell
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Unix-like shell equivalents:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Do not claim that a command passed unless it was actually executed successfully.

For documentation-only changes, still prefer at least the minimum quality command unless there is a documented reason not to run it. If skipping the full gate because the change is process-only, state the reason explicitly and do not claim full-gate success.

When Gradle plugin metadata, task inputs, task outputs, or plugin implementation classes are changed and `validatePlugins` exists, also run:

```powershell
.\gradlew.bat validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

If `validatePlugins` does not exist because this is not a Gradle plugin repository, report that plugin validation is not applicable.

---

## Quality Failure Handling

When a quality command fails, collect and report:

- command executed,
- failing task,
- failing test class or method when available,
- assertion or exception summary,
- whether the failure was introduced by the current change,
- whether it appears pre-existing,
- whether it is environment-related,
- whether it is external-service-related,
- remaining blocker.

Fix failures caused by the current change when realistically possible within scope.

Do not:

- disable failing tests,
- weaken ArchUnit or SOLID rules,
- lower coverage thresholds,
- disable dependency verification,
- add broad dependency-verification trust rules when exact checksum metadata is sufficient,
- commit generated build output,
- silently accept flaky failures.

---

## Optional SonarCloud Gate

If `SONAR_TOKEN` or `sonar.token` is available and the repository documents local Sonar execution, run:

```powershell
.\gradlew.bat sonar --dependency-verification strict --console=plain --stacktrace
```

If credentials are missing:

- report that SonarCloud was skipped locally,
- do not treat missing credentials as a code failure,
- do not claim Sonar success,
- do not remove or weaken Sonar configuration.

---

## Final Diff Review

Only after quality-gate handling, inspect the final state again:

```powershell
git status --short --branch
git diff
git diff --cached
git diff --name-status
git diff --cached --name-status
git diff --check
git diff --cached --check
```

Rules:

- Review the real final diff, not only filenames.
- Separate staged and unstaged changes deliberately.
- Stage only files required for the task.
- Do not include unrelated user changes.
- Do not commit `.gradle`, `build`, IDE workspace files, local logs, temporary files, local databases, trace dumps, or generated reports unless explicitly required.
- If pre-existing staged changes were present, do not rewrite or unstage them without a clear reason.

---

## Commit Message Creation

Create the commit message from the actual final diff only.

Do not use vague messages such as:

```text
update code
fix stuff
small improvements
quality fixes
cleanup
```

Allowed commit types:

```text
feat
fix
refactor
chore
test
docs
perf
build
ci
```

Preferred structure:

```text
<type>: <short precise summary>

What:
- ...

Why:
- ...

Changes:
- ...

Impact:
- ...

Testing:
- ...

Limitations:
- ...
```

Mention affected areas when relevant:

- domain,
- application,
- adapters,
- ingestion,
- persistence,
- graph,
- replay,
- LLM,
- reports,
- CLI,
- bootstrap,
- tests,
- build tooling,
- dependency verification,
- documentation.

Do not invent verification, evidence, or impact that is not visible in the diff or command output.

---

## Stage, Commit, And Push

Only after final diff review:

1. create a work branch if needed,
2. stage only relevant files,
3. review staged diff,
4. create the commit,
5. capture the commit hash,
6. push the current branch.

Typical PowerShell sequence:

```powershell
git switch -c codex/<short-task-slug> # only if not already on a suitable branch
git add <relevant-files>
git diff --cached --stat
git diff --cached
git commit -m "<type>: <short precise summary>" -m "<body>"
git rev-parse --short HEAD
git branch --show-current
git push -u origin <branch-name>
```

Rules:

- Do not commit while detached.
- Do not commit before reviewing the staged diff.
- Do not push before the commit succeeds.
- Do not push directly to `main` unless the user explicitly asks and the repository policy allows it.
- Do not force-push unless explicitly instructed.
- If push is rejected because the remote changed, report the exact reason and do not force-push.
- If no relevant changes exist, do not create an empty commit unless explicitly instructed.

---

## Post-Merge Cleanup

When the user reports that the PR was merged and the remote branch was deleted:

```powershell
git fetch --prune origin
git status --short --branch
git worktree list --porcelain
```

If the current worktree is on the branch to delete, leave it:

```powershell
git switch --detach origin/main
```

Use `git switch main && git pull --ff-only` only when `main` is not already checked out by another worktree.

Then delete the local branch:

```powershell
git branch -d <branch-name>
```

If deletion fails with:

```text
cannot delete branch '<branch-name>' used by worktree at '<path>'
```

then:

1. inspect `git worktree list --porcelain`,
2. switch the listed worktree away from that branch,
3. retry `git branch -d <branch-name>`.

Do not use `git worktree remove`, `git branch -D`, or destructive reset commands unless the user explicitly asks and the branch/worktree state has been verified.

---

## Final Execution Report

After commit/push or cleanup, report:

1. repository root used,
2. current worktree path and branch/detached state,
3. whether `AGENTS.md` was found,
4. whether `QUALITY.md` was found,
5. quality commands documented in `QUALITY.md`,
6. CI workflows found or explicit note that none exist,
7. Java version used,
8. Gradle wrapper version used,
9. exact local quality commands executed and results,
10. SonarCloud result or skip reason,
11. final changed files,
12. final commit message,
13. branch name,
14. commit hash,
15. whether push succeeded,
16. whether post-merge cleanup succeeded,
17. any remaining blocker.

Do not claim success for failed or skipped steps.

---

## Compact Quality Summary

Current repository baseline:

```text
AGENTS.md      = highest repository-local agent rules
QUALITY.md     = binding quality contract
Java           = 25
Gradle         = project wrapper
JUnit          = 6
Minimum gate   = ./gradlew test --dependency-verification strict --console=plain --stacktrace
Full gate      = ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
SonarCloud     = optional locally unless credentials are available
Branch prefix  = codex/
```

---

## Stop Conditions

Stop and report instead of guessing when:

- `AGENTS.md` or `QUALITY.md` contradicts this file,
- repository root cannot be identified,
- Java 25 or Gradle wrapper expectations cannot be satisfied,
- the required Gradle task cannot be found,
- a required source symbol, file, schema, endpoint, graph label, runtime event field, or documented behavior cannot be verified,
- an architecture boundary would need to be violated,
- evidence would need to be invented or uncertainty hidden,
- generated files appear unexpectedly in the diff,
- staged changes existed before your work and their ownership is unclear,
- quality gates fail for a reason that cannot be fixed safely within scope,
- `main` or the task branch is used by another worktree and switching/deletion would affect that worktree,
- the remote rejects the push.

When stopping, include:

- what was attempted,
- what was found,
- why continuing would be unsafe,
- the exact command or file that exposed the blocker,
- the smallest safe next step.
