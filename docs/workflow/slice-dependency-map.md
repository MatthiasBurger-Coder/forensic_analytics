# Slice Dependency Map

## Sequential Order

```text
00 Repository And Rule Verification
  -> 01 Three Amigos And Skill Registry Gate
  -> 02 Service Decomposition Skill
  -> 03 Contract Governance Skill
  -> 04 Microservice Migration Safety Gate Skill
  -> 05 Microservice Runtime Readiness Skill
  -> 06 Senior System Architect Authority
  -> 07 Workflow Authoring And Execution Rules
  -> 08 Three Amigos Microservice Decision Record
  -> 09 Root Microservice Governance
  -> 10 Architecture And Contract Governance Documentation
  -> 11 Quality And Execution Documentation
  -> 12 Consistency Review
  -> 13 Final Verification And Commit Preparation
```

## Parallelization

Read-only specialist reviews may run in parallel after Slice 00.

Write-capable work is sequential by default because slices share governance,
skill, prompt, role and documentation files. Parallel write work is allowed only
when all of these are true:

- file ownership is disjoint
- shared terminology and contract rules are stable
- the Senior Workflow Architect records the handoff
- the Senior System Architect confirms no architecture conflict
- `git diff --check` is run after integration

## Critical Path

The critical path is:

```text
00 -> 01 -> 02/03/04/05 -> 06/07/08 -> 09 -> 10 -> 11 -> 12 -> 13
```

The four new skill slices can be reviewed conceptually in parallel, but their
file edits should be integrated sequentially to avoid skill-authority drift.
