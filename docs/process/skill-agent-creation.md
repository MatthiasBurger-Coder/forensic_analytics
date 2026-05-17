# Skill and Agent Creation

Skill and agent changes belong to the `skills-agents` process strand.

This process applies when creating, updating, refactoring, auditing or reconnecting:

- skills
- agents
- roles
- prompts
- Codex agent definitions
- routing rules
- organigramm
- skill registry
- process documentation

## Scope

Allowed files:

- `AGENTS.md`
- `.agents/**`
- `.codex/**`
- `docs/agents/**`
- `docs/process/**`
- `docs/governance/**`
- `docs/skill-audit/**`
- `docs/arc42/**` only for governance consequences
- `docs/adr/**` only for governance consequences

Forbidden files:

- `src/**`
- `services/**`
- `contracts/**`
- `docker/**`
- `build.gradle*`
- `settings.gradle*`
- `gradle/**`
- `proto/**`
- `forensic-ui/**`

If a skill or agent change appears to require product implementation, build logic, service contracts, Docker/runtime or frontend changes, stop and report:

```text
STOP: This branch reconstruction must not change product implementation files.
```

## Required Flow

1. Skill / agent intake
2. Integrity review
3. Linkage and owner review
4. Conflict and duplicate review
5. Organigramm review
6. Skill registry review
7. AGENTS.md impact review
8. Process documentation review
9. Final skills-agents gate
10. Optional release preparation for `push auto`

The skills-agents flow is intentionally linear: review failures STOP and report. If an automatic correction loop is explicitly authorized, it is capped at `maxRetries = 3` and then escalates to the Root Architect.

## Integrity Review

Each changed skill, role, prompt or agent definition must have:

- a clear owner or governance role
- a defined process strand
- a documented input and output
- no dead references to missing files
- no duplicate or contradictory responsibility with another active skill
- no hidden product implementation authority

## Linkage and Owner Review

Routing rules, prompts, skills, agent definitions and documentation must point to the same owner model. When a new skill or role is added, update the skill registry and organigramm in the same strand.

## Conflict Review

Stop when two active skills, roles or prompts claim incompatible authority over the same workflow decision and no precedence rule exists.

## Final Gate

The final skills-agents gate must confirm:

- `AGENTS.md` remains authoritative
- `QUALITY.md` remains authoritative
- the change is documentation or governance only
- no forbidden files changed
- `push auto` is still limited to `skills-agents`
- the branch is not `main`, `master`, `develop` or another shared branch
- `git diff --check` passes before commit

Passing this gate may prepare the change for optional `push auto`. It does not run `push auto`.
