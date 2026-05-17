# Slice Dependency Map

## Workflow Version

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| workflowTitle | Align Forensics Tracing Description With The Analytics EPIC |
| executionBranch | `docs/workflow-forensics-tracing-analytics-epic-alignment-20260516` |
| sourceWorkflow | `docs/workflow/workflow.md` |

## Dependency Graph

```mermaid
flowchart TD
  S00["Slice 00: Repository, branch and identity preflight"]
  S01["Slice 01: Producer fact extraction"]
  S02["Slice 02: Analytics EPIC gap analysis"]
  S03["Slice 03: Three Amigos requirement review"]
  S04["Slice 04: Contract and boundary comparison"]
  S05["Slice 05: Draft EPIC v0.2"]
  S06["Slice 06: Producer-neutral contracts"]
  S07["Slice 07: Documentation sync"]
  S08["Slice 08: Leakage and sensitive-data audit"]
  S09["Slice 09: Requirement acceptance gate"]
  S10["Slice 10: Quality, commit and optional push"]

  S00 --> S01
  S00 --> S02
  S01 --> S03
  S02 --> S03
  S01 --> S04
  S02 --> S04
  S03 --> S05
  S04 --> S05
  S05 --> S06
  S06 --> S07
  S07 --> S08
  S08 --> S09
  S09 --> S10
```

## Execution Rule

The slice table in `docs/workflow/workflow.md` is the source of truth. This
diagram is a human-readable projection and must stay consistent with the slice
metadata.

## Parallelization

Read-only work may be parallelized:

- Slice 01 and Slice 02 after Slice 00.
- Contract file reading for Slice 04 may begin during Slice 02.

Write-capable work is serial because EPIC, arc42, ADR and README files can
overlap during requirement alignment.

## Lock Rules

- `docs/epics/**` is locked by Slice 05 and Slice 06.
- `docs/README.md`, `docs/arc42/**`, `docs/adr/**` and `docs/architecture/**`
  are locked by Slice 07.
- Leakage fixes after Slice 08 must touch only files already changed by prior
  slices unless a reviewer approves the additional documentation path.
- Product code, frontend code, services, contracts, deployment files, examples,
  data and build logic are blocked paths.
