# Git Branch Strategy Workflow For `workflow create`

## Status

Planned active workflow. This document converts the requested Git Branch
Strategy draft for `workflow create` into the active repository workflow under
`docs/workflow`.

This workflow is planning and routing material only. It does not update
`AGENTS.md`, `.agents/skills`, `.agents/prompts`, `.codex`, ADRs or governance
documentation until `workflow execute` is requested and the configured subagent
or role-review process approves each slice.

## Verified Baseline

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Windows path: `D:/Projects/forensic_analytics`
- Active branch: `feature/workflow-git-branch-strategy-20260516`
- Default integration branch: `main`, verified through `git remote show origin`
- Branch collision check: no local or remote branch existed before creation for
  `feature/workflow-git-branch-strategy-20260516`
- `origin/HEAD` is not configured; use explicit `origin/main` when a default
  remote reference is required.
- Authoritative agent rules: `AGENTS.md`
- Authoritative quality rules: `QUALITY.md`
- Workflow execution rules: `.codex/workflow/workflow-execution-rules.md`
- Project routing rules: `.agents/orchestrator/routing-rules.md`
- Project swarm rules: `.agents/orchestrator/swarm-orchestrator.md`
- Existing active workflow replaced: Skill Landscape Expansion workflow under
  `docs/workflow/**`
- New active workflow location: `docs/workflow/**`
- Related local branch requiring execution-time reconciliation:
  `feature/workflow-branch-isolation-20260516`

## Requirement Source / Gate Decision

The user supplied a `workflow.md` draft titled "Git Branch Strategy for
`workflow create`" on 2026-05-16. The draft defines the repository branch
strategy to use when creating new workflows.

Three Amigos decision:

```text
READY_FOR_WORKFLOW
```

Gate findings:

- Business goal: make `workflow create` branch naming predictable,
  governance-compliant and collision-safe.
- Technical goal: update repository workflow, agent, skill and prompt rules so
  `workflow create` uses only `feature/`, `fix/`, `docs/` or `architecture/`
  prefixes.
- EPIC traceability: no EPIC was named. This is an open traceability gap, but it
  is non-blocking because the request targets repository governance rather than
  platform runtime functionality.
- Architecture impact: no production architecture or microservice boundary
  changes are planned. Governance and agent workflow documents are affected.
- Quality impact: docs and governance changes require `git diff --check` and the
  quality gate from `QUALITY.md` before commit/push readiness is claimed.

## Branch Prefix Decision For This Workflow

Detected workflow branch prefix: `feature`

Reason: the user-provided workflow defines `feature/` as the default for
`workflow create` and gives Git Branch Strategy extension as a default feature
example. The workflow is not a concrete bug fix, is not documentation-only and
does not change platform architecture or service structure. Architecture review
flagged that governance changes could be interpreted as `architecture/`; this is
recorded as a resolved branch-scope decision because the user request explicitly
requires the `feature/workflow-git-branch-strategy-20260516` form.

Proposed and active branch name:

```text
feature/workflow-git-branch-strategy-20260516
```

## Target Outcome

- Every new `workflow create` starts on a dedicated workflow branch before any
  workflow artifacts are created or modified.
- Default branch naming for `workflow create` is:

```text
feature/workflow-<short-topic>-<yyyyMMdd>
```

- `fix/`, `docs/` and `architecture/` are used only when the workflow scope
  clearly matches that special category.
- Other prefixes such as `feat/`, `refactor/`, `test/`, `build/`, `ci/`,
  `quality/`, `agent/` and `chore/` are not used for `workflow create` unless
  repository governance is explicitly changed to allow them.
- Branch names are dated, readable, unique locally and unique remotely.
- Existing user or agent branches are not overwritten or silently mixed.
- Scope decision, branch name and branch conflict checks are documented before
  implementation begins.

## Non-Goals

- Do not implement runtime business functionality.
- Do not change production Java source, tests, graph schema, replay behavior,
  persistence, LLM integration or build-tool adapters.
- Do not silently merge or rebase `feature/workflow-branch-isolation-20260516`.
  That branch must be reconciled explicitly during workflow execution before
  overlapping governance files are changed.
- Do not introduce a new repository-wide branch strategy for all development
  branches. This workflow targets `workflow create` only.
- Do not create compatibility aliases or fallback branch names outside the four
  allowed workflow prefixes.
- Do not commit or push implementation slices unless workflow execution reaches
  commit readiness and the required gates are clean.

## Architecture And Governance Constraints

- Root `AGENTS.md` and `QUALITY.md` remain authoritative.
- `.codex` files stay reusable and must not embed project-specific branch policy
  unless they are explicitly intended as portable workflow behavior.
- Project-specific rules belong in `AGENTS.md`, `QUALITY.md`, `.agents/**` or
  repository governance documentation.
- `workflow create` must verify the Git repository and branch state before
  mutating workflow artifacts.
- Subagents must verify the active workflow branch before modifying files.
- The Senior System Architect must review `architecture/` classifications and
  any governance change that could alter architecture authority.
- The Senior Git Workspace Specialist or equivalent role must review branch
  creation, collision handling and line-ending risk.
- The Senior Tester or Quality Gate Orchestrator must review quality commands
  against `QUALITY.md`.

## Resilience And Git Safety Requirements

- Treat local uncommitted changes as user-owned unless proven otherwise.
- Stop when branch ownership, branch purpose or local changes are unclear.
- Use explicit `origin/main` because `origin/HEAD` is not configured.
- Verify local and remote branch-name collisions before branch creation.
- Use readable suffixes for collision fallback names; do not overwrite local or
  remote branches.
- On Windows-hosted worktrees, run Git and Gradle through WSL and stop if broad
  line-ending-only noise appears.
- Preserve a clear audit trail: status before branch creation, branch decision,
  branch collision checks, active branch verification and quality evidence.

## Repository Target Structure

This workflow creation owns only `docs/workflow/**`.

Implementation slices may change these files after `workflow execute`, subject
to slice ownership and verification:

```text
AGENTS.md
.agents/orchestrator/routing-rules.md
.agents/orchestrator/swarm-orchestrator.md
.agents/prompts/workflow-create.md
.agents/roles/senior-workflow-architect/SKILL.md
.agents/skills/git-branch-strategy/SKILL.md
.agents/skills/workflow-authoring/SKILL.md
.agents/skills/workflow-conflict-resolution/SKILL.md
.agents/skills/workflow-slice-execution/SKILL.md
.agents/skills/release-branch-governance/SKILL.md
.agents/skills/release-branch-governance/branch-rules.md
.codex/agents/senior_workflow_architect.toml
docs/adr/**
docs/arc42/**
docs/governance/**
docs/workflow/**
```

Execution must first inspect the related branch
`feature/workflow-branch-isolation-20260516` because it already modifies many
of these governance files.

## Quality Gates

Minimum verification from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate from `QUALITY.md`:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Every slice must run the narrowest relevant local check first. Documentation
workflow changes add at least:

```bash
git diff --check
```

This does not replace `QUALITY.md`. The full local quality gate remains required
before claiming commit/push readiness unless a documented exception is accepted
for a non-implementation workflow artifact change. Failed required gates are
blocking.

## Slice Dependency Order

```text
00 -> 01 -> 02 -> 03 -> 04 -> 05 -> 06
```

All implementation slices are sequential by default because they share
governance files. Read-only specialist reviews may run in parallel. Any
write-capable parallel work requires disjoint write scopes and explicit handoff
records.

## Role Ownership Map

| Slice | Primary owner | Review roles |
| --- | --- | --- |
| 00 | Senior Workflow Architect | Senior Git Workspace Specialist, Senior System Architect, Senior Tester |
| 01 | Senior Git Workspace Specialist | Senior System Architect, Senior Documentation Engineer |
| 02 | Senior Workflow Architect | Senior Agent Orchestrator, Senior Git Workspace Specialist |
| 03 | Senior Git Workspace Specialist | Senior Tester, Workflow Conflict Resolution |
| 04 | Senior Agent Orchestrator | Senior System Architect, Senior Documentation Engineer |
| 05 | Senior Tester | Senior Git Workspace Specialist, Senior System Architect |
| 06 | Senior Documentation Engineer | Senior System Architect, Senior Agent Orchestrator |

Callable subagents may be used only when the active request authorizes delegated
execution. During `workflow execute`, use matching role files as local checklists
when callable subagents are unavailable.

## Slice 00 - Baseline And Reconciliation Gate

Purpose: verify the branch, working tree, default branch and related local
governance branches before any implementation changes.

Prerequisites: active branch is
`feature/workflow-git-branch-strategy-20260516`; worktree is clean.

Affected files: read-only review of `AGENTS.md`, `QUALITY.md`, `.agents/**`,
`.codex/**`, `docs/adr/**`, `docs/arc42/**`, `docs/governance/**` and
`docs/workflow/**`.

Allowed write scope: `docs/workflow/git-state-review.md` and
`docs/workflow/execution-summary.md` only when recording execution results.

Dependencies: none.

Done criteria:

- `feature/workflow-branch-isolation-20260516` is inspected and either merged,
  rebased, superseded or explicitly kept separate before overlapping files are
  edited.
- Branch scope, branch prefix and active branch are documented.
- No unrelated local changes are present.

Verification commands:

```bash
git status --short --branch
git branch --show-current
git remote show origin
git diff --name-status origin/main..feature/workflow-branch-isolation-20260516
git diff --check
```

Stop conditions:

- The related branch cannot be inspected.
- Overlapping governance changes cannot be reconciled safely.
- Branch status is dirty, detached or unclear.

## Slice 01 - Repository-Governance Rule For `workflow create`

Purpose: add the workflow-specific branch schema to repository governance.

Prerequisites: Slice 00 completed and overlapping branch-first changes are
reconciled.

Affected files:

- `AGENTS.md`
- `.agents/skills/git-branch-strategy/SKILL.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/release-branch-governance/SKILL.md`
- `.agents/skills/release-branch-governance/branch-rules.md`
- governance or ADR files when a decision record already exists or is required

Allowed write scope: only the verified governance files above.

Dependencies: Slice 00.

Done criteria:

- `workflow create` defaults to
  `feature/workflow-<short-topic>-<yyyyMMdd>`.
- `fix/`, `docs/` and `architecture/` are documented as special cases only for
  clearly matching scopes.
- Disallowed workflow prefixes are listed and cannot be mistaken as valid for
  `workflow create`.

Verification commands:

```bash
git diff -- AGENTS.md .agents/skills/git-branch-strategy/SKILL.md .agents/skills/workflow-authoring/SKILL.md .agents/skills/release-branch-governance
git diff --check
```

Stop conditions:

- Existing branch or release governance contradicts the new rule and cannot be
  resolved with at least 95 percent confidence.
- A rule would permit work on `main`, `master`, `develop` or an unrelated branch.

## Slice 02 - Workflow-Scope Classification

Purpose: define how `workflow create` chooses one of the four allowed prefixes
before branch creation.

Prerequisites: Slice 01 completed.

Affected files:

- `AGENTS.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/prompts/workflow-create.md`
- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`

Allowed write scope: the verified classification and orchestration files.

Dependencies: Slice 01.

Done criteria:

- Decision order is documented: `fix`, `docs`, `architecture`, then `feature`.
- `feature/` is the fallback for unclear non-special cases.
- Required workflow output records detected prefix, reason and proposed branch
  name before branch creation.
- Branch names always include `workflow-` and end with `yyyyMMdd`.

Verification commands:

```bash
rg -n "workflow create|feature/workflow|fix/workflow|docs/workflow|architecture/workflow" AGENTS.md .agents .codex docs
git diff --check
```

Stop conditions:

- A special scope must be used but cannot be classified safely.
- Existing prompts or skills permit unsupported prefixes for `workflow create`.

## Slice 03 - Branch Conflict Check

Purpose: document and enforce local and remote branch collision checks before
creating workflow branches.

Prerequisites: Slice 02 completed.

Affected files:

- `AGENTS.md`
- `.agents/skills/git-branch-strategy/SKILL.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/workflow-conflict-resolution/SKILL.md`
- `.agents/prompts/workflow-create.md`

Allowed write scope: branch conflict rules, prompt text and workflow-conflict
documentation only.

Dependencies: Slice 02.

Done criteria:

- Local check uses `git branch --list <branch-name>`.
- Remote check uses `git ls-remote --heads origin <branch-name>`.
- Existing branches are reused only when they clearly belong to the same task.
- Fallback names remain readable and governance-compliant.
- STOP behavior is defined for unclear branch ownership.

Verification commands:

```bash
git branch --list feature/workflow-git-branch-strategy-20260516
git ls-remote --heads origin feature/workflow-git-branch-strategy-20260516
git diff --check
```

Stop conditions:

- A branch collision cannot be classified as same-task or different-task.
- Remote access fails and the workflow cannot verify branch uniqueness.

## Slice 04 - Subagent Assignment By Workflow Scope

Purpose: map workflow scopes to required and optional specialist roles.

Prerequisites: Slice 03 completed.

Affected files:

- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/roles/**`
- `.agents/skills/workflow-authoring/SKILL.md`

Allowed write scope: routing and workflow-authoring governance only.

Dependencies: Slice 03.

Done criteria:

- Every workflow scope has at least one owner role.
- `architecture/` workflows always involve Senior System Architect review.
- `fix/` workflows always involve Senior Tester or Quality Gate review.
- `feature/` workflows route to the relevant domain, governance or workflow
  specialist.
- Parallel work requires explicit ownership and disjoint write scopes.

Verification commands:

```bash
rg -n "workflow scope|feature/|fix/|docs/|architecture/" .agents/orchestrator .agents/skills .agents/roles
git diff --check
```

Stop conditions:

- A scope has no responsible role.
- Two roles own the same output with conflicting authority.

## Slice 05 - Quality Assurance By Workflow Scope

Purpose: define minimum verification expectations for each workflow scope while
preserving `QUALITY.md` authority.

Prerequisites: Slice 04 completed.

Affected files:

- `AGENTS.md`
- `QUALITY.md` only if a verified inconsistency requires it
- `.agents/skills/quality-gate-orchestrator/SKILL.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/release-branch-governance/**`
- `docs/governance/**`

Allowed write scope: quality-governance text only.

Dependencies: Slice 04.

Done criteria:

- Each workflow scope has clear minimum checks.
- `QUALITY.md` remains the source of truth for required Gradle commands.
- Documentation-only and architecture-only special cases still record
  verification evidence.
- Skipped checks require explicit rationale.

Verification commands:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```

Full commit-readiness command:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- A required quality command cannot be verified from `QUALITY.md` or Gradle.
- A failed required quality gate is treated as non-blocking.

## Slice 06 - Documentation, Prompts And Skills Synchronization

Purpose: make the new rule discoverable to users, Codex prompts and subagents.

Prerequisites: Slice 05 completed.

Affected files:

- `AGENTS.md`
- `.agents/prompts/workflow-create.md`
- `.agents/skills/**` files touched by earlier slices
- `.codex/agents/senior_workflow_architect.toml` if the current
  project-specific agent prompt already owns workflow creation behavior
- `docs/adr/**`, `docs/arc42/**`, `docs/governance/**` when touched or required
- `docs/workflow/**`

Allowed write scope: synchronization updates for the same branch-strategy rule.

Dependencies: Slice 05.

Done criteria:

- Branch-governance rule is discoverable.
- Prompts and skills do not contradict the rule.
- Existing skill rules are updated instead of duplicated.
- Old contradictory rules are removed only when the correct replacement is
  verified with at least 95 percent confidence.
- Execution summary records changed files, checks, limits and open items.

Verification commands:

```bash
rg -n "workflow create|workflow-<short-topic>|feature/workflow|fix/workflow|docs/workflow|architecture/workflow|codex/" AGENTS.md .agents .codex docs
git diff --check
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Stop conditions:

- Contradictory old rules cannot be resolved safely.
- Documentation would claim a command passed without executed evidence.

## Parallelization Plan

Implementation slices are sequential because they overlap `AGENTS.md`,
workflow-authoring skills, branch-governance skills and prompts. Read-only
specialist review may run in parallel for each slice. Write-capable subagents
may be used only when their file ownership is disjoint and the active branch is
verified before editing.

## Documentation Synchronization Points

- After Slice 01: synchronize branch-governance decision records.
- After Slice 03: synchronize prompts and branch conflict documentation.
- After Slice 05: synchronize quality and release/branch governance language.
- After Slice 06: update `docs/workflow/execution-summary.md` with final changed
  files, quality evidence and limitations.

## Stop Conditions

Stop and report when:

1. The active branch is not the expected workflow branch.
2. `feature/workflow-branch-isolation-20260516` cannot be reconciled before
   overlapping governance files are edited.
3. Uncommitted changes appear and their ownership is unclear.
4. The default branch cannot be determined.
5. A local or remote branch collision cannot be classified safely.
6. Existing workflow, skill or agent rules conflict and cannot be resolved with
   at least 95 percent confidence.
7. A change would permit workflow artifact edits on `main`, `master`, `develop`
   or an unrelated branch.
8. A required quality command cannot be run or fails.
9. Continuing would require guessing branch ownership, Gradle task names, prompt
   ownership or governance authority.

## Uncertainty Escalation Rules

- Treat unmerged local branches as user-owned until proven otherwise.
- Record unresolved branch, prompt or skill conflicts in
  `docs/workflow/git-governance-conflict-matrix.md`.
- Ask the user before merging, rebasing or superseding unrelated local branches.
- When a special prefix is unclear, use `feature/` only if doing so does not hide
  a known fix, docs-only or architecture-only scope.
- Do not infer that static workflow plans are implemented behavior.

## Commit And Push Plan

For workflow creation, commit only regenerated `docs/workflow/**` files after
diff review and applicable quality checks.

For workflow execution, commit only after all slices pass their required checks
and the final diff is reviewed. Commit messages must document:

1. What changed.
2. Why it changed.
3. How it was implemented.
4. Affected files.
5. Tests and quality checks executed.
6. Limits and open items.

Push must explicitly target the workflow branch:

```bash
git push -u origin feature/workflow-git-branch-strategy-20260516
```

Do not use a plain push while the branch tracks `origin/main`.

## Whole Workflow Definition Of Done

- `workflow create` branch rule is implemented in the verified governance files.
- `feature/` is documented as the default workflow-create prefix.
- `fix/`, `docs/` and `architecture/` are documented as special cases only.
- Unsupported prefixes are not documented as valid for `workflow create`.
- Branch-name generation, local collision check and remote collision check are
  documented and tested by review or command evidence.
- Subagent routing and quality-gate expectations match repository rules.
- `QUALITY.md` commands were executed or any unavailable checks were documented
  with reason.
- Final diff contains only the approved workflow scope.
