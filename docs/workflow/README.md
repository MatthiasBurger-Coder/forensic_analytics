# Repository Workflow

This directory contains the active repository workflow for governed, slice-based
agent work.

Root `AGENTS.md` and `QUALITY.md` remain authoritative. This workflow is
planning and routing material; it does not authorize implementation until the
user runs the repository's explicit workflow execution command.

## Active Workflow

- [workflow.md](workflow.md) - Git Branch Strategy workflow for `workflow create`
  branch naming, branch collision checks, workflow-scope classification,
  subagent routing and quality-gate documentation.

## Supporting Files

- [branch-strategy-rules.md](branch-strategy-rules.md) records the branch-prefix
  decision order and naming rules.
- [branch-readiness-checklist.md](branch-readiness-checklist.md) lists the checks
  that must pass before workflow artifacts are changed.
- [git-state-review.md](git-state-review.md) records the verified branch state
  for this `workflow create` run.
- [git-governance-conflict-matrix.md](git-governance-conflict-matrix.md)
  records known governance overlaps and stop conditions.
- [agent-handoff-matrix.md](agent-handoff-matrix.md) maps slices to owner and
  review roles.
- [deadlock-prevention-rules.md](deadlock-prevention-rules.md) defines branch
  and file-ownership controls.
- [quality-gate-plan.md](quality-gate-plan.md) records verification commands from
  `QUALITY.md`.
- [execution-summary.md](execution-summary.md) records workflow creation status
  and open execution prerequisites.

## Execution Rule

Use `workflow execute` only when this active workflow should be implemented
through the configured subagent or role-review process.
