# Slice Dependency Map

```mermaid
flowchart TD
  S00["Slice 00: execution preflight"]
  S01["Slice 01: boundary and contract gap freeze"]
  S02["Slice 02: Gateway HTTP and gRPC BTM contracts"]
  S03["Slice 03: artifact and target ownership"]
  S04["Slice 04: Gateway service bootstrap"]
  S05["Slice 05: external Git repository workspace flow"]
  S06["Slice 06: Java AST worker handoff"]
  S07["Slice 07: repository snapshot and build artifact worker contract"]
  S08["Slice 08: Joern worker handoff"]
  S09["Slice 09: instrumentation target planning"]
  S10["Slice 10: BTM gRPC file delivery"]
  S11["Slice 11: orchestration contract and artifact-readiness bridge"]
  S12["Slice 12: source-fact byte retrieval and Java AST handoff contract"]
  S13["Slice 13: source-fact artifact contract and artifact IO hardening"]
  S14["Slice 14: end-to-end repository-to-BTM orchestration"]
  S15["Slice 15: runtime readiness and local service landscape"]
  S16["Slice 16: graph replay and report service decision"]
  S17["Slice 17: frontend and CLI Gateway integration"]
  S18["Slice 18: retire or isolate replaced monolith paths"]
  S19["Slice 19: remove obsolete shared implementation modules"]
  S20["Slice 20: full quality gate and migration acceptance"]

  S00 --> S01 --> S02
  S02 --> S03
  S02 --> S04
  S04 --> S05
  S05 --> S06
  S03 --> S07
  S05 --> S07
  S06 --> S07
  S07 --> S08
  S06 --> S09
  S08 --> S09
  S03 --> S10
  S09 --> S10
  S02 --> S11
  S03 --> S11
  S05 --> S11
  S06 --> S11
  S07 --> S11
  S08 --> S11
  S09 --> S11
  S10 --> S11
  S11 --> S12
  S12 --> S13
  S13 --> S14
  S14 --> S15
  S14 --> S16
  S14 --> S17
  S15 --> S18
  S16 --> S18
  S17 --> S18
  S18 --> S19 --> S20
```

## Parallelization Notes

- Slices 00 through 03 are serial because they stabilize contracts and
  ownership.
- Slice 04 can proceed after Slice 02.
- Slice 07 is serial after Slices 03, 05 and 06 because it creates the
  source-package, complete build-output package, byte-access and Joern
  materialization contract required by Joern handoff.
- Slice 08 waits for Slice 07 and must not receive Repository Analysis private
  workspace identifiers.
- Slice 10 waits for artifact ownership and target planning.
- Slice 11 closes the orchestration owner, Gateway public API security,
  Java AST byte-access and deterministic readiness preconditions found during
  the blocked end-to-end review.
- Slice 12 waits for Slice 11 and verifies source-fact byte retrieval,
  Repository Analysis to Java AST handoff closure and deterministic local
  fixture readiness.
- Slice 13 waits for Slice 12 and hardens the source-fact artifact payload
  contract plus artifact IO before end-to-end orchestration consumes produced
  bytes.
- Slice 14 waits for Slice 13 and implements the end-to-end owner-API
  orchestration path.
- Slice 15 waits for Gateway behavior from Slice 14 and proves local runtime
  evidence for the implemented service path.
- Slice 16 waits for Slice 14 and either creates graph/report roots or records
  explicit deferral.
- Slice 17 waits for Slice 14 and verifies frontend/CLI calls through
  Gateway/public APIs only.
- Module retirement and removal are serial and late by design.
- Checkpoint commits and pushes are not a separate terminal slice. They run
  after every successful `workflow execute` slice.
