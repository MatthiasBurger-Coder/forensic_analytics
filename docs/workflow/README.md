# Repository Workflow

This directory contains the active repository workflow for governed, slice-based
agent work.

Root `AGENTS.md` and `QUALITY.md` remain authoritative. This workflow is
planning and routing material only. It does not authorize implementation until
the user runs the explicit workflow execution command.

## Active Workflow

- [workflow.md](workflow.md) - Microservice Skill Sharpening workflow for future
  microservice migration governance, skills, role prompts, quality rules and
  documentation synchronization.

## Supporting Files

- [three-amigos-decision-record.md](three-amigos-decision-record.md) records the
  requirement gate result used before authoring this workflow.
- [skill-target-map.md](skill-target-map.md) maps the user-requested skill paths
  to the repository's verified skill and role layout.
- [microservice-governance-rules.md](microservice-governance-rules.md) records
  the service-boundary, contract-first and runtime-independence rules that later
  slices must preserve.
- [conflict-review.md](conflict-review.md) records known repository conflicts and
  non-blocking risks found during read-only specialist review.
- [slice-dependency-map.md](slice-dependency-map.md) lists execution order,
  dependencies and parallelization limits.
- [agent-handoff-matrix.md](agent-handoff-matrix.md) maps slices to owner and
  review roles.
- [quality-gate-plan.md](quality-gate-plan.md) records verification commands from
  `QUALITY.md` and workflow-specific checks.
- [execution-summary.md](execution-summary.md) records workflow creation status
  and open execution prerequisites.
- [prompts/microservice-skill-sharpening.md](prompts/microservice-skill-sharpening.md)
  provides the execution prompt for this workflow.

## Execution Rule

Use `workflow execute` only when this active workflow should be implemented
through the configured subagent or role-review process.
