# Slice Dependency Map

## Topology

```text
S00 Execution Preflight
  -> S01 ADR And arc42 Target Landscape Reconciliation
    -> S02 Caller And Coupling Inventory Gate
      -> S03 Contract-First Communication Baseline
      -> S04 Data Ownership And Persistence Split
        -> S05 Repository Source Service Extraction
        -> S06 Ingestion Service Extraction
        -> S07 JavaParser Analysis Service Extraction
        -> S08 Joern Analysis Service Extraction
          -> S09 Analysis Orchestrator Service Boundary
          -> S10 Query Report API Service Boundary
            -> S11 CLI Client Decoupling
        -> S12 Observability Stack And Logging Decoupling
          -> S13 Testbed Decoupling
            -> S14 Legacy Shared Module Retirement
              -> S15 Runtime Readiness, Architecture Tests And Closure
```

## Dependency Rationale

| Slice | Dependency Reason |
|---|---|
| S00 | Freezes branch, context and quality baseline before execution. |
| S01 | Resolves the known drift between FA-MSA-001 names and accepted ADR/arc42 names before product migration. |
| S02 | Caller proof must exist before any old module or dependency is removed. |
| S03 | Services must communicate through explicit contracts before implementation depends on communication behavior. |
| S04 | Persistence and canonical-data ownership must be explicit before central persistence is removed. |
| S05-S08 | Worker/service extraction starts after contract and data ownership gates. |
| S09 | Orchestrator can coordinate only after initial worker boundaries are defined. |
| S10 | Query/report API depends on public contracts and orchestration ownership. |
| S11 | CLI can migrate after the public query/report API contract exists. |
| S12 | Observability/logging decoupling can run after architecture baseline and caller inventory. |
| S13 | Testbed decoupling depends on enough service boundaries to replace monolith-only coverage. |
| S14 | Legacy module retirement depends on service migration, caller-free proof and replacement tests. |
| S15 | Final readiness and acceptance criteria can close only after old module retirement. |

## Parallelization Opportunities

S05 through S08 have potential parallel implementation after S03 and S04 because
they target different service roots and old adapter modules. The active workflow
still executes one slice at a time unless the user explicitly authorizes
parallel subagent or worker execution. Parallel work must use disjoint file,
contract and architecture locks.

## Lock Summary

| Lock Area | Owning Slice |
|---|---|
| Target service naming and ADR/arc42 authority | S01 |
| Legacy caller inventory | S02 |
| Cross-service contracts | S03 |
| Data ownership and persistence | S04 |
| Repository workspaces and source snapshots | S05 |
| Raw ingestion and upload sessions | S06 |
| JavaParser static source facts | S07 |
| Joern semantic artifacts | S08 |
| Orchestration state | S09 |
| Public query/report API | S10 |
| CLI client behavior | S11 |
| Observability/logging | S12 |
| Testbed and integration environment | S13 |
| Old central module removal | S14 |
| Final readiness and acceptance evidence | S15 |
