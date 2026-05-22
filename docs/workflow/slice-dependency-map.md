# Slice Dependency Map

## Topology

```text
S00 Execution Preflight
  -> S01 Current Caller And Dependency Revalidation
    -> S02 Contract And Runtime Parity Gate
      -> S03 Repository Source Legacy Retirement
      -> S04 Ingestion Legacy Retirement
      -> S05 JavaParser Legacy Retirement
      -> S06 Joern Legacy Retirement
      -> S10 Observability And Logging Decoupling

S03 + S04 + S05 + S06
  -> S07 Orchestration Engine And Application Split
    -> S08 Query Report API, REST, Bootstrap And Boot Retirement
      -> S09 CLI Client Decoupling

S07 + S08 + S10
  -> S11 Persistence Ownership Finalization
    -> S12 Shared Domain And Application Module Removal
      -> S13 Testbed Monolith Coupling Removal
        -> S14 Gradle Deregistration And Source Tree Removal
          -> S15 Closure, Rollback Notes And Release Readiness
```

## Dependency Rationale

| Slice | Dependency Reason |
|---|---|
| S00 | Freezes branch, context and quality baseline before execution. |
| S01 | Caller proof must be current before any path is marked removable. |
| S02 | Runtime callers can move only after external contracts and parity gates are verified. |
| S03 | Repository source adapter retirement depends on service contract and source ownership. |
| S04 | Ingestion module retirement depends on intake contract and raw payload ownership. |
| S05 | JavaParser adapter retirement depends on static source-fact parity. |
| S06 | Joern adapter retirement depends on Joern artifact and diagnostics parity. |
| S07 | Engine/application split depends on worker service boundaries from S03-S06. |
| S08 | REST/boot/bootstrap retirement depends on orchestrator and public API replacement. |
| S09 | CLI decoupling depends on public API behavior. |
| S10 | Observability/logging decoupling can start after contracts but must finish before central module removal. |
| S11 | Persistence retirement depends on orchestration, public API and observability ownership. |
| S12 | Shared domain/application removal depends on all service-local replacements. |
| S13 | Testbed retirement depends on service-local regression parity. |
| S14 | Final deletion depends on every path-specific migration slice. |
| S15 | Closure depends on final quality gate and documentation alignment. |

## Parallelization Opportunities

S03 through S06 may be parallelized after S02 only when S3D confirms disjoint
file locks and stable contracts. S10 can run after S02 while S07/S08 proceed
only when observability file locks do not overlap. Default execution remains
one slice at a time.

## Lock Summary

| Lock Area | Owning Slice |
|---|---|
| Current caller inventory | S01 |
| External service contracts | S02 |
| Repository workspaces and source snapshots | S03 |
| Raw ingestion and upload sessions | S04 |
| JavaParser static source facts | S05 |
| Joern semantic artifacts | S06 |
| Orchestration state and shared application split | S07 |
| Public REST/query/report API and runtime bootstrap | S08 |
| CLI client behavior | S09 |
| Logging and observability | S10 |
| Persistence and data ownership | S11 |
| Shared domain/application removal | S12 |
| Testbed regression parity | S13 |
| Final module deregistration | S14 |
| Closure and release readiness | S15 |
