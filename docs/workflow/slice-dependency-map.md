# Slice Dependency Map

## Governance Flowchart V2 Dependencies

```mermaid
flowchart TD
  S00["Slice 00: Repository and governance inventory"]
  S01["Slice 01: Branch governance confirmation"]
  S02["Slice 02: Feedback loop limits"]
  S03["Slice 03: S3 STOP paths"]
  S04["Slice 04: S3_CLASSIFY default path"]
  S05["Slice 05: Typed Error Router"]
  S06["Slice 06: S3D execution orchestration"]
  S07["Slice 07: Publication modes cleanup"]
  S08["Slice 08: Commit checkpoint rollback"]
  S09["Slice 09: Commit traceability"]
  S10["Slice 10: D8 and Q11 separation"]
  S11["Slice 11: Guard name sharpening"]
  S12["Slice 12: Documentation governance separation"]
  S13["Slice 13: Two-level flowcharts"]
  S14["Slice 14: Agent and skill linkage"]
  S15["Slice 15: arc42 ADR governance docs"]
  S16["Slice 16: Final integrity check"]

  S00 --> S01 --> S02
  S02 --> S03 --> S04 --> S06
  S02 --> S05 --> S06
  S03 --> S05
  S05 --> S07 --> S08 --> S09
  S05 --> S10
  S02 --> S11 --> S12
  S03 --> S13
  S04 --> S13
  S05 --> S13
  S06 --> S13
  S07 --> S13
  S08 --> S13
  S12 --> S13
  S13 --> S14 --> S15 --> S16
  S09 --> S16
  S10 --> S16
```

## Execution Rule

Each slice must complete its role review, diff review and quality gate before it can be committed.

Each slice checkpoint commit must represent exactly one slice.

The checkpoint push target is:

```text
origin/architecture/workflow-governance-flowchart-v2-20260517
```

Slice checkpoint push must not:

- push to `main`
- create or merge a PR
- run `push auto`
- clean up branches
- force-push

## Parallelization Candidates

Parallel execution is allowed only when S3D proves file, contract and architecture-boundary locks are disjoint.

Candidate groups:

- Slice 07 and Slice 11 after Slice 05, if publication and guard-name files are disjoint.
- Slice 09 and Slice 10 after Slice 08, if traceability and reporting files are disjoint.
- Slice 14 read-only inventory may start during Slice 13, but write activity must wait until Slice 13 is complete.
