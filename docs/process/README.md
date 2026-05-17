# Process Governance

This directory documents repository process governance for automated agents.

Root `AGENTS.md` remains authoritative for mandatory agent behavior. `QUALITY.md` remains authoritative for verification commands and quality-gate expectations.

## Process Strands

Repository agent work is organized into three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

The strands must not be mixed. Shared governance roles execute inside the active strand and apply that strand's file scope, review duties and publication rules.

## Process Documents

- [Skills Update Command](skills-update.md) defines the exact `skills update` entrypoint and the `skills-agents` strand.
- [Skill and Agent Creation](skill-agent-creation.md) defines creation, update, audit and linkage rules for skills, agents, roles and prompts.
- [Push Auto Governance](push-auto.md) defines the guarded `push auto` publication mode for the `skills-agents` strand.
- [Branch Governance](branch-governance.md) defines branch isolation and publication-mode boundaries.

## Publication Modes

Publication modes are separate:

- Slice checkpoint push belongs to `workflow execute` and pushes only the current workflow branch after a successful slice quality gate.
- `push` is the normal branch push and pull-request process after explicit user approval.
- `push auto` belongs only to `skills-agents` and may merge a PR only after guard checks pass.

Slice checkpoint push is not `push auto`.
`push` is not `push auto`.
`skills update` is not `push auto`.
