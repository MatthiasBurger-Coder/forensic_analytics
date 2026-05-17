# Process Governance

This directory documents repository process governance for automated agents.

Root `AGENTS.md` remains authoritative for mandatory agent behavior. `QUALITY.md` remains authoritative for verification commands and quality-gate expectations.

## Process Strands

Repository agent work is organized into three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

The strands must not be mixed. Shared governance roles execute inside the active strand and apply that strand's file scope, review duties and publication rules.

## Documentation Governance

`DOCROOT` is the global documentation-governance check. It verifies process
documentation, role model, organigramm, arc42 structure, governance rules,
workflow conventions and hard boundaries across the repository.

Local documentation nodes update concrete artifacts inside the active strand:

- `S1_DOC`: skills-agents documentation for skills, agents, roles, prompts,
  routing, organigramm, skill registry and related process docs.
- `S2_DOC`: workflow-create documentation for requirement gates,
  `docs/workflow/workflow.md`, workflow handoff and checked arc42 impact.
- `S3_DOC`: workflow-execute documentation for slice execution, quality gates,
  rollback, commit results and execution reports.

`DOCROOT` is not a fourth process strand and must not replace local
documentation updates. Local nodes write concrete artifacts; `DOCROOT` checks
global consistency.

## Process Documents

- [Skills Update Command](skills-update.md) defines the exact `skills update` entrypoint and the `skills-agents` strand.
- [Skill and Agent Creation](skill-agent-creation.md) defines creation, update, audit and linkage rules for skills, agents, roles and prompts.
- [Push Auto Governance](push-auto.md) defines the guarded `push auto` publication mode for skills, agents, process governance and governance-only workflow documentation.
- [Branch Governance](branch-governance.md) defines branch isolation and publication-mode boundaries.
- [Governance Workflow Diagrams](../governance/workflow/) define the
  two-level Governance Flowchart V2 overview and detail diagrams.

## Publication Modes

Publication modes are separate:

- Slice checkpoint push belongs to `workflow execute` and pushes only the current workflow branch after a successful slice quality gate.
- `push` is the normal branch push and pull-request process after explicit user approval.
- `push auto` is owned by the `skills-agents` publication guard and may merge a PR only after guard checks pass, including governance-only `docs/workflow/**` documentation when no blocked implementation files changed.

Publication outcomes are explicit:

- `PUB_DONE`: publication completed and verified.
- `PUB_PR_RESULT`: PR open or updated without automatic merge.
- `PUB_PUSH_FAILED`: push failed and routes to rollback or escalation.
- `PUB_REJECTED`: governance, scope, branch or guard rejection.

Rollback is governed through `CP_ROLLBACK`; it is a decision node and must not
be treated as blind `git reset --hard`.

Slice checkpoint push is not `push auto`.
`push` is not `push auto`.
`skills update` is not `push auto`.
