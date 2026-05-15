# Workplan Lifecycle

## Regeneration Rule

Every new workplan starts by deleting `docs/workplan` completely.

After deletion, the new workplan must regenerate the full `docs/workplan` structure. A workplan author must not partially overwrite old slices or keep stale planning artifacts unless the user explicitly asks to archive them.

## Required Workplan Structure

A workplan must include:

- verified baseline
- target outcome
- explicit non-goals
- architecture constraints
- resilience requirements
- ordered slices
- slice dependencies
- parallelization opportunities
- subagent ownership map
- quality-gate expectations
- stop conditions
- uncertainty escalation rules
- commit and push plan when requested

## Slice Numbering

Use stable two-digit slice numbers:

```text
Slice 01
Slice 02
Slice 03
...
```

Do not renumber executed slices unless regenerating the entire workplan.

## Slice Dependencies

Each slice must define:

- prerequisites
- affected files
- owner or role
- parallelization status
- done criteria
- verification commands
- stop conditions

Dependency-sensitive slices must wait for shared contracts to stabilize before multiple workers edit dependent files.

## Parallelizable Slices

A slice may be parallelized only when:

- write scopes are disjoint
- shared terminology is already fixed
- role ownership is clear
- expected outputs are explicit
- verification can be run independently

If multiple slices need the same file, sequence them.

## Subagent Assignment

Assign subagents by repository role and risk:

- workplan structure and slice planning: Senior Workplan Architect
- requirement and EPIC drift: Senior Requirement Engineer
- architecture boundaries and arc42: Senior System Architect or arc42 governance skill
- documentation consistency: Senior Documentation Engineer
- quality gate and verification: Senior Tester or quality skills
- branch, commit and push readiness: git commit preparation skills

Subagents must receive exact files, allowed write scopes, expected output and stop conditions.

## Stop Conditions

Stop and report if:

- architecture conflicts are unclear
- EPIC contradicts implementation
- multiple workplans conflict
- service ownership is ambiguous
- resilience expectations are unclear
- quality gate authority is unclear
- planned file paths cannot be verified
- a required role or skill target conflicts with repository rules

Never silently guess governance decisions.

## Uncertainty Escalation

Document unresolved uncertainty in the workplan and route it to the correct governance owner. Do not convert uncertainty into a decision without verified source evidence or explicit user instruction.
