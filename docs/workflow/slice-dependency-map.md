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
  S07["Slice 07: Joern worker handoff"]
  S08["Slice 08: instrumentation target planning"]
  S09["Slice 09: BTM gRPC file delivery"]
  S10["Slice 10: end-to-end repository-to-BTM orchestration"]
  S11["Slice 11: runtime readiness and local service landscape"]
  S12["Slice 12: graph replay and report service decision"]
  S13["Slice 13: frontend and CLI Gateway integration"]
  S14["Slice 14: retire or isolate replaced monolith paths"]
  S15["Slice 15: remove obsolete shared implementation modules"]
  S16["Slice 16: full quality gate and migration acceptance"]

  S00 --> S01 --> S02
  S02 --> S03
  S02 --> S04
  S04 --> S05
  S05 --> S06
  S05 --> S07
  S06 --> S08
  S07 --> S08
  S03 --> S09
  S08 --> S09
  S04 --> S10
  S05 --> S10
  S06 --> S10
  S07 --> S10
  S08 --> S10
  S09 --> S10
  S10 --> S11
  S10 --> S12
  S10 --> S13
  S11 --> S14
  S12 --> S14
  S13 --> S14
  S14 --> S15 --> S16
```

## Parallelization Notes

- Slices 00 through 03 are serial because they stabilize contracts and
  ownership.
- Slice 04 can proceed after Slice 02.
- Slices 06 and 07 can run in parallel after Slice 05 if they have disjoint
  write scopes and stable contracts.
- Slice 09 waits for artifact ownership and target planning.
- Slice 11 waits for Gateway behavior from Slice 10 and proves local runtime
  evidence for the implemented service path.
- Slice 12 waits for Slice 10 and either creates graph/report roots or records
  explicit deferral.
- Slice 13 waits for Slice 10 and verifies frontend/CLI calls through
  Gateway/public APIs only.
- Module retirement and removal are serial and late by design.
- Checkpoint commits and pushes are not a separate terminal slice. They run
  after every successful `workflow execute` slice.
