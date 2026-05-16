# Repository Workflow

This directory contains the active repository workflow for governed, slice-based
agent work.

Root `AGENTS.md` and `QUALITY.md` remain authoritative. This workflow is
planning and routing material only. It does not authorize implementation until
the user runs the explicit workflow execution command.

## Active Workflow

- [workflow.md](workflow.md) - Skill and Agent Integrity Correction workflow for
  branch-safe, Three-Amigos-led, architecture-governed agent and skill
  governance.

## Supporting Files

- [three-amigos-decision-record.md](three-amigos-decision-record.md) records the
  requirement gate result used before authoring this workflow.
- [skill-agent-inventory-baseline.md](skill-agent-inventory-baseline.md) records
  the verified starting inventory and path conventions for the workflow.
- [governance-conflict-review.md](governance-conflict-review.md) records
  repository conflicts, path mismatches and non-blocking risks found during
  read-only review.
- [slice-dependency-map.md](slice-dependency-map.md) lists execution order,
  dependencies and parallelization limits.
- [agent-handoff-matrix.md](agent-handoff-matrix.md) maps slices to owner and
  review roles.
- [quality-gate-plan.md](quality-gate-plan.md) records verification commands from
  `QUALITY.md` and workflow-specific checks.
- [execution-summary.md](execution-summary.md) records workflow creation status
  and open execution prerequisites.
- [prompts/skill-agent-integrity-correction.md](prompts/skill-agent-integrity-correction.md)
  provides the execution prompt for this workflow.

## Execution Rule

Use `workflow execute` only when this active workflow should be implemented
through the configured subagent or role-review process.
