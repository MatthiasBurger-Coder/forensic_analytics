# Process Governance

This directory defines the repository process model for Codex agents, skills,
workflow planning, workflow execution and publication readiness.

Root `AGENTS.md` remains the mandatory rule source. `QUALITY.md` remains the
quality-gate source. Files in this directory explain how those rules are
applied to the three allowed process strands.

## Three Process Strands

There are exactly three governed process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

The strands must not be mixed. Shared governance roles such as Documentation
Governance, Senior System Architect, Skill Registry Maintainer and Process
Governance Maintainer execute inside the active strand. They do not create a
fourth process strand and they must not carry file changes from one strand into
another.

## Strand Boundaries

| Strand | Purpose | Required output | Explicitly forbidden |
|---|---|---|---|
| `skills-agents` | Create, change, audit, classify, document and release skills, roles, prompts and Codex agent definitions | Approved skill or agent governance change with registry, organigramm and documentation evidence | Backend, frontend, Docker/runtime and analytics implementation |
| `workflow create` | Requirement, architecture, planning and documentation for a future execution workflow | Checked `docs/workflow/workflow.md` and checked or updated arc42 documentation | Product implementation, backend code, frontend code, Docker/runtime code and analytics code |
| `workflow execute` | Execute only a checked workflow created by `workflow create` | Slice evidence, quality results, execution report and synchronized documentation | Scope expansion without returning to `workflow create` |

## Documentation Map

- [branch-governance.md](branch-governance.md) defines branch isolation for
  governed work.
- [skill-agent-creation.md](skill-agent-creation.md) defines the
  `skills-agents` strand.
- [push-auto.md](push-auto.md) defines the `push auto` guard for the
  `skills-agents` strand.
- [workflow-create.md](workflow-create.md) defines the `workflow create` strand.
- [workflow-execute.md](workflow-execute.md) defines the `workflow execute`
  strand.
- [three-amigos-requirement-gate.md](three-amigos-requirement-gate.md) defines
  the requirement gate used by `workflow create`.

The agent organigramm is documented in
[`docs/agents/organigramm.md`](../agents/organigramm.md). The process registry
is documented in [`docs/agents/skill-registry.md`](../agents/skill-registry.md).

## Completion Rule

A strand is complete only when its documentation duty is complete. Documentation
is part of the definition of done, not a later cleanup.
