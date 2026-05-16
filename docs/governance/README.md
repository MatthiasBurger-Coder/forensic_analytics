# Engineering Governance

## Purpose

This document explains the reusable engineering governance system for Codex-based work in Forensic Analytics.

The governance system keeps these artifacts synchronized:

- EPIC and requirements
- arc42 architecture documentation
- ADR references
- workflows
- quality gates
- resilience requirements
- skills
- roles
- Codex agent definitions

Root `AGENTS.md` remains the authority for engineering rules. Root `QUALITY.md` remains the authority for verification commands.

## Governance Flow

Use this flow for requirement-sensitive or architecture-sensitive work:

```text
User request
  -> requirement classification
  -> EPIC drift check
  -> arc42 and ADR impact check
  -> dedicated workflow branch creation and checkout
  -> workflow regeneration or update
  -> slice execution
  -> quality verification
  -> documentation and role/skill synchronization
```

## Workflow Lifecycle

New workflows must be generated through `.agents/skills/workflow-authoring/SKILL.md`.

Before creating a new workflow:

1. Verify the Git repository context.
2. Check the working tree status.
3. Stop if the current branch is detached, unclear, or if unrelated or unclear uncommitted changes exist.
4. Create and checkout a dedicated workflow branch, unless the current branch already matches the current workflow.
5. Check local and remote branch-name collisions, choosing the next clear unique suffix when needed.
6. Verify the active workflow branch.
7. Verify the repository root and target path.
8. Delete `docs/workflow` completely, unless the user explicitly asks to preserve an existing workflow.
9. Recreate `docs/workflow`.
10. Regenerate the complete workflow structure.

This prevents stale slices, obsolete workflows, conflicting plans and historical leftovers from remaining active.

Read-only verification, requirement intake, routing-rule inspection and role selection may occur before branch creation. Mutating workflow creation must not.

Never create or modify workflow artifacts, including `workflow.md`, `docs/workflow/**`, workplans, slice definitions, workflow-specific documentation changes, implementation tasks, or write-capable agent assignments, on `main`, `master`, `develop`, or any shared branch.

## Requirement Lifecycle

Requirement-sensitive work must use `.agents/skills/requirement-engineering/SKILL.md`.

The Senior Requirement Engineer asks:

```text
Does the implementation still match the EPIC?
```

If drift is detected, review:

- EPIC
- arc42
- ADR references
- `QUALITY.md`
- `docs/workflow`
- related skills
- related roles

Planned behavior, implemented behavior, assumptions and unresolved conflicts must stay separate.

## arc42 Synchronization

Architecture-sensitive work must use `.agents/skills/arc42-architecture-governance/SKILL.md`.

Review arc42 when:

- service boundaries change
- plugin versus server responsibilities change
- runtime behavior changes
- deployment topology changes
- persistence ownership changes
- UI communication strategy changes
- resilience decisions change
- scalability constraints change
- observability requirements change

ADRs record decisions and context. arc42 records active architectural consequences. Workflows route implementation according to those consequences.

## Architecture Governance

The governance system requires:

- no silent architecture drift
- no undocumented service boundary changes
- no undocumented resilience changes
- no undocumented deployment changes
- no stale EPIC assumptions
- no stale workflow slices

Stop and report when ownership, architecture conflicts or requirement intent cannot be verified.

## Quality Governance

Quality commands must come from `QUALITY.md`.

The documented minimum command is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The documented full local gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Do not invent Gradle tasks or substitute undocumented scripts.

## Examples

### Plugin Responsibility Moved To Server

When responsibility moves from plugin to server:

- update or review the EPIC
- update arc42 building block and runtime views
- review ADR references about plugin producer boundaries
- regenerate or update the workflow
- update affected skills and roles only where ownership guidance is verified

### UI Communication Strategy Changed

When UI communication changes:

- verify the current UI communication assumption
- update the EPIC if user-facing behavior changed
- update arc42 runtime and deployment views
- review resilience requirements for timeouts, retries and degraded states
- update workflow verification steps

### New Resilience Requirement Introduced

When a new resilience requirement appears:

- classify it as a non-functional requirement
- update or review the EPIC
- update arc42 crosscutting concepts and quality requirements
- update affected workflow slices
- document unresolved operational assumptions
