# Governance Conflict Review

## Resolved During Workflow Creation

- The workflow branch
  `feature/workflow-skill-agent-integrity-correction-20260516` was created
  before workflow artifacts were modified.
- The previous active workflow under `docs/workflow/**` described Microservice
  Skill Sharpening. It has been regenerated for this Skill and Agent Integrity
  Correction workflow.
- Three Amigos readiness was completed before authoring this workflow.
- Callable subagents were not spawned during workflow creation because the
  verified workflow-authoring and orchestrator rules require explicit delegated
  execution authorization. Local role-review checklists were used instead.

## Path Mismatches Requiring Execution Care

- The user draft names flat skill files such as
  `.agents/skills/workplan-executor.md`. The verified repository convention is
  `.agents/skills/<skill-name>/SKILL.md`.
- The user draft names `.codex/prompts/workflow-create.md`. The verified
  repository contains `.agents/prompts/workflow-create.md`; `.codex/prompts/**`
  is not present.
- Root `README.md` is not present. `docs/README.md` is present.
- `workplan execute` appears in the user draft's expected end state, while the
  verified repository command is `workflow execute`. Slice execution must
  decide whether this is terminology drift or requires prompt documentation.

## Existing Governance Constraints

- ADR-0011 requires Three Amigos before workflow authoring.
- ADR-0015 requires skill registry and conflict auditing for governance changes
  that affect skill ownership.
- ADR-0016 requires workflow branches before workflow artifacts.
- `QUALITY.md` requires strict dependency verification for Gradle quality gates.
- Repository documentation must be English.

## Non-Blocking Risks

- No EPIC was named. The workflow records this as a traceability gap but not a
  workflow creation blocker because the requested change is governance-only.
- Several requested governance skills already exist under different names.
  Slice 08 must audit before creating duplicates.
- Documentation-only slices still risk authority drift because the same files
  are referenced by multiple roles. Handoffs must be explicit.

## Execution Stop Rules

Stop during `workflow execute` if:

- path mapping would require guessing;
- a requested file location conflicts with verified repository conventions;
- a role claims authority above its documented boundary;
- a quality command cannot be verified;
- a slice would modify production code without a new workflow decision.
