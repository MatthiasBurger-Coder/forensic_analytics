# Verified Baseline

This file records the repository facts verified before creating this workplan.

## Inspected Files And Directories

The read-only inspection covered:

- `AGENTS.md`
- `QUALITY.md`
- `.agents/AGENTS.md`
- `.agents/orchestrator/`
- `.agents/roles/`
- `.agents/skills/`
- `.codex/config.toml`
- `.codex/agents/`
- `docs/workplan/`
- `docs/arc42/`
- `docs/adr/`

## Repository Rules

The root `AGENTS.md` is the mandatory source for agent behavior, architecture boundaries, evidence integrity, documentation language and stop conditions.

`QUALITY.md` is the authoritative quality contract. It defines the minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

and the full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

On Windows hosts, repository commands must run through WSL from the WSL-mounted worktree.

## Existing Agent Material

`.agents/AGENTS.md` defines the agent directory model:

- `orchestrator/` describes slice coordination, routing and conflict handling.
- `roles/` defines role responsibilities and required reference skills.
- `skills/<skill-name>/SKILL.md` contains discoverable Codex skills.
- `.codex/agents/` contains project-scoped custom subagent TOML files.

Existing roles are currently Markdown files under `.agents/roles/`, such as:

- `senior-documentation-engineer.md`
- `senior-system-architect.md`
- `senior-swarm-orchestrator.md`
- `senior-java-backend.md`
- `senior-tester.md`

Existing skills are discoverable directories under `.agents/skills/`, each with a `SKILL.md` file and YAML frontmatter.

## Requested Governance Artifacts

Before this workplan was executed, these requested artifacts did not exist:

- `.agents/skills/workplan-authoring`
- `.agents/skills/requirement-engineering`
- `.agents/skills/arc42-architecture-governance`
- `.agents/skills/engineering-governance`
- `.agents/roles/senior-workplan-architect`
- `.agents/roles/senior-requirement-engineer`

This workplan intentionally treated them as new artifacts to create during execution. The executed governance slice has now added these artifacts.

## Existing Workplan Behavior

The previous `docs/workplan` described an executed resilient React UI MVP plan. It was not the governance workplan requested here.

The previous workplan already included useful execution patterns:

- read-only verification before implementation
- ordered implementation slices
- subagent and parallelization planning
- architecture target documentation
- resilience requirements
- quality-gate documentation
- commit and push planning

This workplan regenerates `docs/workplan` completely so no stale UI-MVP slice remains active.

## Existing Architecture Governance

`docs/arc42/README.md` states that the EPIC remains the product and requirement baseline and that arc42 transforms that baseline into architectural structure.

`docs/adr/README.md` states that decisions are derived from the EPIC baseline and refined during implementation.

The current repository already contains architecture, quality and evidence-integrity rules, but it does not yet contain a reusable governance layer that explicitly synchronizes EPIC, arc42, ADRs, workplans, requirements, skills and roles.

## Missing Synchronization Points

The inspection found these missing governance links:

- no reusable workplan-authoring skill exists
- no reusable requirement-engineering skill exists
- no arc42 synchronization skill exists
- no umbrella engineering-governance skill exists
- no Senior Workplan Architect role exists
- no Senior Requirement Engineer role exists
- no explicit rule in a reusable skill requires deleting `docs/workplan` before new workplan generation
- no explicit requirement drift checklist is available as reusable agent guidance
- no reusable checklist ties EPIC, arc42, ADR references, `QUALITY.md`, `docs/workplan`, skills and roles together

## Stop Points

Stop and report before implementing the governance artifacts if:

- the root repository rules conflict with a planned governance rule
- the target role directory format conflicts with `.agents/AGENTS.md` and cannot be justified by the task
- EPIC location or current EPIC source cannot be verified for synchronization work
- an arc42 section mentioned by a new skill cannot be found
- a quality-gate command cannot be verified in `QUALITY.md`
- multiple active workplans are discovered outside `docs/workplan` and their authority is unclear
