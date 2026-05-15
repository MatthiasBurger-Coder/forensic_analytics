# Implementation Slices

Each slice starts with read-only verification of the files it will touch.

## Dependency Overview

```text
01 inspection
  -> 02 workplan-authoring skill
  -> 03 workplan lifecycle rules
  -> 04 requirement-engineering skill
  -> 05 arc42 governance skill
  -> 06 engineering-governance skill
  -> 07 senior workplan architect role
  -> 08 senior requirement engineer role
  -> 09 requirement drift rules
  -> 10 architecture governance rules
  -> 11 stop rules
  -> 12 validation checklists
  -> 13 align existing skills
  -> 14 documentation
  -> 15 quality gate
  -> 16 commit and push
```

Slices 04 and 05 may proceed in parallel after Slice 03 if write scopes remain disjoint. Slices 07 and 08 may proceed in parallel after Slices 02 through 06 define the shared terminology.

## Slice 01 - Repository Inspection

Inspect:

- `.agents`
- `.codex`
- `docs/workplan`
- `docs/arc42`
- `docs/adr`
- `QUALITY.md`
- `AGENTS.md`
- existing skills
- existing roles

Document:

- existing governance rules
- existing workplan behavior
- existing architecture governance
- missing synchronization points

Write scope:

- `docs/workplan/00-verified-baseline.md`

Done criteria:

- required target artifacts are confirmed present or missing
- existing role and skill conventions are documented
- no files outside workplan are modified

## Slice 02 - Create workplan-authoring Skill

Create:

```text
.agents/skills/workplan-authoring/SKILL.md
```

The skill must cover:

- workplan creation
- slice structure
- subagent assignment
- dependencies
- quality gates
- non-goals
- architecture constraints
- resilience requirements
- verification steps

Done criteria:

- skill has YAML frontmatter
- skill references `AGENTS.md` and `QUALITY.md`
- skill does not redefine conflicting repository rules

## Slice 03 - Add Workplan Lifecycle Rules

Update:

```text
.agents/skills/workplan-authoring/SKILL.md
```

Add mandatory rules:

- delete `docs/workplan` before creating a new workplan
- regenerate the full workplan structure
- never partially overwrite old workplan slices
- never keep stale workplan artifacts unless explicitly archived
- define slice numbering
- define slice dependency handling
- define parallelizable slices
- define stop conditions
- define uncertainty escalation rules

Done criteria:

- lifecycle rules are explicit
- stop conditions are unambiguous
- stale artifact handling is deterministic

## Slice 04 - Create requirement-engineering Skill

Create:

```text
.agents/skills/requirement-engineering/SKILL.md
```

The skill must cover:

- EPIC lifecycle
- requirement drift detection
- functional versus non-functional requirements
- architecture impact analysis
- requirement traceability
- requirement classification
- constraint management
- assumption tracking
- continuous comparison between implementation and EPIC assumptions

Done criteria:

- skill includes drift checklist
- skill distinguishes implemented behavior, planned behavior and unresolved assumptions
- skill routes architecture drift to arc42 governance

## Slice 05 - Create arc42 Governance Skill

Create:

```text
.agents/skills/arc42-architecture-governance/SKILL.md
```

The skill must cover:

- when arc42 must be updated
- how architecture decisions propagate
- how runtime changes affect documentation
- how deployment views evolve
- how service boundaries are documented
- how resilience decisions are documented
- how ADR references are reviewed

Done criteria:

- skill maps governance checks to existing arc42 sections
- skill preserves ADR history rules
- skill requires verified evidence for documentation changes

## Slice 06 - Create engineering-governance Skill

Create:

```text
.agents/skills/engineering-governance/SKILL.md
```

The skill acts as umbrella governance and must cover:

- EPIC, arc42 and workplan synchronization
- governance checkpoints
- quality synchronization
- resilience synchronization
- architecture consistency checks
- documentation consistency checks

Done criteria:

- skill references the three governance skills created earlier
- skill does not duplicate root `AGENTS.md` as a competing authority
- skill includes synchronization obligations

## Slice 07 - Create Senior Workplan Architect Role

Create:

```text
.agents/roles/senior-workplan-architect/SKILL.md
```

Responsibilities:

- create executable workplans
- split work into slices
- assign subagents
- detect planning risks
- define dependencies
- validate implementation order
- coordinate architecture-safe execution

Mandatory rule:

- always delete `docs/workplan` before generating a new workplan unless explicitly instructed otherwise

Done criteria:

- role references workplan-authoring and engineering-governance skills
- role includes stop conditions
- role states expected outputs

## Slice 08 - Create Senior Requirement Engineer Role

Create:

```text
.agents/roles/senior-requirement-engineer/SKILL.md
```

Responsibilities:

- maintain EPIC consistency
- detect requirement drift
- update EPIC when needed
- update arc42 when needed
- check architecture consistency
- track new constraints
- track scalability requirements
- track resilience requirements
- track UX requirements

Mandatory internal question:

```text
Does the implementation still match the EPIC?
```

Done criteria:

- role references requirement-engineering, arc42 governance and engineering-governance skills
- role includes drift response rules
- role documents unresolved conflict behavior

## Slice 09 - Add Requirement Drift Rules

Update:

- `.agents/skills/requirement-engineering/SKILL.md`
- `.agents/roles/senior-requirement-engineer/SKILL.md`

Explicitly check:

- service ownership changes
- plugin versus server responsibility changes
- new runtime assumptions
- new orchestration assumptions
- new persistence assumptions
- new deployment assumptions
- new UI assumptions
- new resilience assumptions

If drift is detected:

- update EPIC
- update arc42
- update workplan
- document unresolved conflicts

Done criteria:

- drift handling is written as mandatory behavior
- uncertainty is not silently resolved

## Slice 10 - Add Architecture Governance Rules

Update:

- `.agents/skills/arc42-architecture-governance/SKILL.md`
- `.agents/skills/engineering-governance/SKILL.md`

Require:

- no silent architecture drift
- no undocumented service boundary changes
- no undocumented resilience changes
- no undocumented deployment changes
- no stale EPIC assumptions

Done criteria:

- architecture governance rules are explicit
- update triggers map to EPIC, arc42, ADRs and workplan

## Slice 11 - Add Stop Rules

Update all new governance roles and skills.

Stop and report if:

- architecture conflicts are unclear
- EPIC contradicts implementation
- multiple workplans conflict
- service ownership is ambiguous
- resilience expectations are unclear

Done criteria:

- every new role and skill contains compatible stop conditions
- no skill tells agents to guess governance decisions

## Slice 12 - Add Validation Checklists

Update:

- `.agents/skills/engineering-governance/SKILL.md`
- related role files if useful

Add checklists for:

- EPIC consistency
- arc42 consistency
- workplan consistency
- resilience consistency
- architecture consistency
- service boundary consistency
- quality gate consistency

Done criteria:

- checklists are reusable
- checklist outputs can be pasted into workplan or review notes

## Slice 13 - Align Existing Skills

Safely update existing skills and roles only where obvious.

Possible references:

- apply workplan-authoring governance rules
- apply requirement-engineering governance rules
- apply arc42 governance synchronization

Do not rewrite unrelated content.

Document uncertain conflicts instead.

Done criteria:

- only verified integration points are changed
- flat role files are not migrated unless explicitly required
- any skipped or uncertain alignment is documented

## Slice 14 - Documentation

Create or update documentation explaining:

- governance flow
- workplan lifecycle
- requirement lifecycle
- EPIC synchronization
- arc42 synchronization
- architecture governance

Include examples:

- plugin responsibility moved to server
- UI communication strategy changed
- new resilience requirement introduced

Done criteria:

- documentation is in English
- documentation distinguishes planned and implemented governance
- examples do not imply runtime functionality was implemented

## Slice 15 - Quality Gate

Run:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```

If feasible also run:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Document exact results.

Done criteria:

- exact commands and outcomes are recorded
- failures include failing task, suspected cause and blocker status

## Slice 16 - Commit And Push

If currently on `main`, create a dedicated work branch first.

Commit message must contain:

```text
Why:
What:
How:
Verification:
Impact:
Limitations:
```

Push automatically only if repository rules and user instructions allow it.

Done criteria:

- git status is reviewed before staging
- staged files match the workplan scope
- line-ending-only noise is not staged
- commit and push status is reported
