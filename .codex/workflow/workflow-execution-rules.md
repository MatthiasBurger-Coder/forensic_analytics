# Workflow Execution Rules

Use this workflow for non-trivial Forensic Analytics work.

Root `AGENTS.md` and `QUALITY.md` remain authoritative. This file coordinates the durable Codex team structure and must not override repository safety, evidence, architecture, or verification rules.

## Execution Phases

1. Read-only verification
   - Inspect the task, root `AGENTS.md`, `QUALITY.md`, affected files, build files, relevant documentation, `.agents/orchestrator/routing-rules.md`, and role or skill files.
   - Verify exact symbols, modules, Gradle tasks, contracts, schema fields, graph labels, event fields, and commands before implementation.

2. Slice detection
   - Identify the smallest meaningful implementation or documentation slice.
   - Record dependencies, affected files, role owners, verification commands, and stop conditions.

3. Role assignment
   - Route slices through `.agents/orchestrator/routing-rules.md`.
   - Use callable subagents only when delegated execution is authorized by the active request or workplan command.
   - If callable subagents are unavailable, perform an explicit local review with the matching role file and report that limitation.

4. Implementation
   - Apply only the smallest verified change.
   - Keep changes traceable to the requested task, observed defect, verified architecture rule, quality failure, or documented project decision.

5. Verification
   - Run the narrowest meaningful checks first.
   - Run the applicable quality gate from `QUALITY.md` when required by the slice or commit readiness.
   - Run `git diff` and `git diff --check` before claiming completion.

6. Reporting
   - Report changed files, main changes, executed commands, failures, quality-gate result, known limitations, and blockers.

## Workplan Execute Protocol

When the active command is `workplan execute`, use `.codex/skills/workplan-executor/SKILL.md` and `.agents/skills/workplan-executor/SKILL.md`.

Execution order:

1. Locate the active workplan.
2. Read the complete workplan.
3. Identify all slices and dependencies.
4. Assign roles or subagents.
5. Execute one slice at a time.
6. Run required tests and quality checks after each slice.
7. Inspect diffs after each slice.
8. Stop on unverifiable assumptions, architecture conflicts, missing commands, quality failures, or evidence-risking ambiguity.

## Stop Conditions

Stop and report when:

- a required file, class, method, task, schema, contract, field, graph label, or event name cannot be verified exactly;
- source and documentation conflict in a behavior-relevant way;
- a change would fabricate forensic evidence or hide uncertainty;
- a slice would introduce shared Java code modules between services;
- a quality command cannot be verified from `QUALITY.md` or Gradle files;
- continuing would require guessing.
