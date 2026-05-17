# Workflow Execution Rules

Use this reusable workflow for non-trivial repository work.

Root `AGENTS.md` and `QUALITY.md`, when present, remain authoritative for project-specific safety, architecture, documentation, and verification rules. This file coordinates the reusable Codex team structure and must not override project rules.

## Execution Phases

1. Read-only verification
   - Inspect the task, root project instructions, quality documentation, affected files, build files, relevant documentation, reusable `.codex` role or skill files, and any discovered project-specific role or skill files.
   - Verify exact symbols, modules, build tasks, contracts, schema fields, event fields, and commands before implementation.

2. Slice detection
   - Identify the smallest meaningful implementation or documentation slice.
   - Record dependencies, affected files, role owners, verification commands, and stop conditions.

3. Role assignment
   - Route slices to the smallest suitable set of subagents or role reviews.
   - Prefer project-specific routing rules when the repository provides them.
   - Use callable subagents only when delegated execution is authorized by the active request or workflow command.
   - If callable subagents are unavailable, perform an explicit local review with the matching role file and report that limitation.

4. Implementation
   - Apply only the smallest verified change.
   - Keep changes traceable to the requested task, observed defect, verified architecture rule, quality failure, or documented project decision.

5. Verification
   - Run the narrowest meaningful checks first.
   - Run the applicable quality gate from project quality documentation when required by the slice or commit readiness.
   - Run `git diff` and `git diff --check` before claiming completion.

6. Reporting
   - Report changed files, main changes, executed commands, failures, quality-gate result, known limitations, and blockers.

## Workflow Execute Protocol

When the active command is `workflow execute`, use `.codex/skills/workflow-executor/SKILL.md` first, then use any discovered project-specific workflow-executor skill.

Execution order:

1. Locate the active workflow only at `docs/workflow/workflow.md` when project rules require that path.
2. Verify that the workflow is checked and records checked or updated arc42 documentation.
3. Load the checked or updated arc42 documentation.
4. Read the complete workflow.
5. Identify all slices and dependencies.
6. Classify slices into backend, frontend, Docker/runtime and documentation strands when project rules define those strands.
7. Assign roles or subagents.
8. Execute one slice at a time.
9. Run required tests and quality checks after each slice.
10. Inspect diffs after each slice.
11. Stage only files changed by the current slice.
12. Run `git diff --cached --check`.
13. Create a slice-scoped checkpoint commit.
14. Push the current workflow branch to `origin`.
15. Record the commit SHA and push result in the execution report.
16. Continue with the next slice only after the checkpoint push succeeded.
17. Stop on unverifiable assumptions, architecture conflicts, missing commands, quality failures, unclear staged diffs, failed checkpoint pushes, scope expansion, missing arc42 evidence, or ambiguity that could change behavior.

Slice checkpoint pushes are normal pushes to the current workflow branch. They
are not `push auto`, do not merge pull requests, do not clean up branches, do
not push to `main`, and must not force-push.

## Stop Conditions

Stop and report when:

- a required file, class, method, task, schema, contract, field, or event name cannot be verified exactly;
- source and documentation conflict in a behavior-relevant way;
- a change would fabricate evidence, test data, runtime facts, analysis output, or user-visible behavior;
- a slice would violate verified architecture or service-boundary rules;
- a quality command cannot be verified from repository documentation or build files;
- continuing would require guessing.
