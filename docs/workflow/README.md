# Repository Workflow

This directory contains the active repository workflow for governed, slice-based
agent work.

Root `AGENTS.md` and `QUALITY.md` remain authoritative. This workflow is
planning and routing material only. It does not authorize implementation until
the user runs the explicit workflow execution command.

## Active Workflow

- [workflow.md](workflow.md) - Microservices Ecosystem Conversion workflow for
  converting the current modular Forensic Analytics platform into independently
  buildable, startable, testable, containerized services through contract-first
  migration slices.

## Supporting Files

- [three-amigos-decision-record.md](three-amigos-decision-record.md) records the
  requirement gate result used before authoring this workflow.
- [current-state-baseline.md](current-state-baseline.md) records the verified
  repository state used as the migration baseline.
- [governance-conflict-review.md](governance-conflict-review.md) records draft
  adjustments, repository conflicts and execution risks.
- [slice-dependency-map.md](slice-dependency-map.md) lists slice order,
  dependencies and parallelization limits.
- [agent-handoff-matrix.md](agent-handoff-matrix.md) maps slices to owner and
  review roles.
- [quality-gate-plan.md](quality-gate-plan.md) records verification commands from
  `QUALITY.md` and workflow-specific checks.
- [execution-summary.md](execution-summary.md) records workflow creation status
  and open execution prerequisites.
- [prompts/microservices-ecosystem-conversion.md](prompts/microservices-ecosystem-conversion.md)
  provides the execution prompt for this workflow.

## Execution Rule

Use `workflow execute` only when this active workflow should be implemented
through the configured subagent or role-review process.
