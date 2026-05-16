# Slice Dependency Map

## Sequential Order

```text
00 Repository Preparation And Branch Verification
  -> 01 Inventory Existing Skill And Agent Files
  -> 02 Define Decision And Escalation Chain
  -> 03 Formalize Three Amigos Gate
  -> 04 Codify Workflow Create Branching
  -> 05 Correct Workflow / Workplan Executor Boundaries
  -> 06 Bound Agent Swarm Orchestrator Authority
  -> 07 Harden Microservice Expert And Invariants
  -> 08 Audit Missing Governance Skills
  -> 09 Document Development Model Alignment
  -> 10 Introduce Workflow Traceability Matrix
  -> 11 Add Governance Quality Gate
  -> 12 Consolidate Root AGENTS.md
  -> 13 Run Cross-Skill Consistency Audit
  -> 14 Final Verification, Commit And Push
```

## Parallelization

Read-only specialist reviews may run in parallel after Slice 00. Examples:

- Senior System Architect can review authority boundaries while Senior
  Documentation Engineer reviews inventory structure.
- Senior Tester can review quality-gate implications while Senior DevOps
  Engineer reviews branch and command requirements.
- Microservice Senior Expert can review invariant wording while Contract
  Governance Expert reviews allowed communication mechanisms.

Write-capable work is sequential by default because slices share governance,
skill, prompt, role and documentation files. Parallel write work is allowed
only when all of these are true:

- file ownership is disjoint
- shared terminology and contract rules are stable
- the Senior Workflow Architect records the handoff
- the Senior System Architect confirms no architecture conflict
- `git diff --check` is run after integration

## Critical Path

```text
00 -> 01 -> 02 -> 03 -> 04 -> 05 -> 06 -> 07 -> 08 -> 09 -> 10 -> 11 -> 12 -> 13 -> 14
```

## Dependency Notes

- Slice 02 must precede all role-boundary corrections.
- Slice 03 and Slice 04 must precede prompt and executor updates.
- Slice 07 must precede any microservice-related skill additions.
- Slice 08 must precede final `AGENTS.md` consolidation so duplicate skills are
  not introduced.
- Slice 13 must precede commit and push preparation.
