# Slice Dependency Map

## Dependency Graph

```mermaid
flowchart TD
    S01["S01 Root stack, network, build context"]
    S02["S02 repository-source-service"]
    S03["S03 ingestion-service"]
    S04["S04 java-parser-analysis-service"]
    S05["S05 joern-analysis-service"]
    S06["S06 analysis-orchestrator-service"]
    S07["S07 query-report-api-service"]
    S08["S08 cli-client"]
    S09["S09 observability-stack"]
    S10["S10 testbed"]
    S11["S11 forensic-ingestion-service"]
    S12["S12 forensic-gateway-service"]
    S13["S13 analysis-store-service"]
    S14["S14 repository-analysis-service"]
    S15["S15 java-ast-analysis-service"]
    S16["S16 joern-cpg-analysis-service"]
    S17["S17 btm-generation-service"]
    S18["S18 graph-replay-service"]
    S19["S19 report-generation-service"]
    S20["S20 forensic-ui"]
    S21["S21 deployment runbook"]
    S22["S22 final verification"]

    S01 --> S02
    S01 --> S03
    S01 --> S04
    S01 --> S05
    S02 --> S06
    S03 --> S06
    S04 --> S06
    S05 --> S06
    S02 --> S07
    S06 --> S07
    S07 --> S08
    S01 --> S09
    S01 --> S10
    S01 --> S11
    S01 --> S14
    S01 --> S15
    S01 --> S16
    S01 --> S17
    S14 --> S13
    S15 --> S13
    S16 --> S13
    S17 --> S13
    S13 --> S12
    S01 --> S18
    S01 --> S19
    S07 --> S20
    S02 --> S21
    S03 --> S21
    S04 --> S21
    S05 --> S21
    S06 --> S21
    S07 --> S21
    S08 --> S21
    S09 --> S21
    S10 --> S21
    S11 --> S21
    S12 --> S21
    S13 --> S21
    S14 --> S21
    S15 --> S21
    S16 --> S21
    S17 --> S21
    S18 --> S21
    S19 --> S21
    S20 --> S21
    S21 --> S22
```

## Parallel Groups

| Group | Slices | Rule |
|---|---|---|
| P0 | S01 | Must run first; owns root network strategy and `.dockerignore` build-context guard. |
| P1 | S02, S03, S04, S05 | Target owner/worker service fragments with disjoint files. |
| P2 | S06 | Waits for target owner/worker fragments. |
| P3 | S07 | Waits for repository-source and orchestrator fragments. |
| P4 | S08, S09, S10, S18, S19 | Tool, observability, testbed, and planned-root slices with disjoint files. |
| P5 | S11, S14, S15, S16, S17 | Transitional worker fragments with disjoint files. |
| P6 | S12, S13 | Transitional gateway/store path after worker fragments. |
| P7 | S20 | Waits for public API fragment. |
| P8 | S21 | Waits for all service/root/UI slices. |
| P9 | S22 | Final verification only. |

## Lock Rules

- Each service fragment owns exactly one file under
  `deployment/docker-compose/services/`.
- Root stack, `.dockerignore`, `deployment/README.md`, and shared Compose
  README edits are serialized through S01, S21, and S22.
- Contract files are read-only unless a later slice explicitly discovers a
  verified contract mismatch and stops for a contract governance decision.
- Private owner volumes must not be added to non-owner service fragments.
