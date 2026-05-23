# Slice Dependency Map

## Topology

```text
S00 Execution Preflight
  -> S01 Current Caller And Dependency Revalidation
    -> S02 Contract And Runtime Parity Gate
      -> S03 Repository Source Parity And Handoff Readiness
      -> S04 Ingestion Service Parity And Handoff Readiness
      -> S05 JavaParser Service Parity And Handoff Readiness
      -> S06 Joern Service Parity And Handoff Readiness
      -> S10 Observability And Logging Replacement Readiness

S03 + S04 + S05 + S06
  -> S07 Orchestration Service Parity And Application Split Readiness
    -> S08 Query Report API And Runtime Replacement Readiness
      -> S09 CLI Client Parity And Decoupling Readiness

S07 + S08 + S10
  -> S11 Persistence Ownership And Replacement Readiness
    -> S12 Service-Local Domain And Application Readiness
      -> S13 Service Testbed Parity And Monolith Coupling Readiness
        -> S14 Retirement Readiness Reconciliation
          -> S15 Testbed Architecture And Hardening Relocation
          -> S16 Testbed Runtime Scenario Replacement Or Deprecation
          -> S17 Repository Checkout And Ingestion Testbed Replacement
          -> S18 Public API, Boot And Persistence Ownership Exit

S15 + S16 + S17 + S18
  -> S19 Candidate-Specific Gradle Deregistration And Source Tree Removal
    -> S20 Closure, Rollback Notes And Release Readiness
```

## Dependency Rationale

| Slice | Dependency Reason |
|---|---|
| S00 | Freezes branch, context and quality baseline before execution. |
| S01 | Caller proof must be current before any path is marked removable. |
| S02 | Runtime callers can move only after external contracts and parity gates are verified. |
| S03 | Repository source parity and handoff readiness depend on service contract and source ownership. |
| S04 | Ingestion parity and handoff readiness depend on intake contract and raw payload ownership. |
| S05 | JavaParser parity depends on static source-fact contracts and diagnostics. |
| S06 | Joern parity depends on Joern artifact and diagnostics contracts. |
| S07 | Orchestration readiness depends on worker service boundaries from S03-S06. |
| S08 | Public API and runtime replacement readiness depends on orchestrator and public API contracts. |
| S09 | CLI client readiness depends on public API behavior. |
| S10 | Observability/logging replacement readiness can start after contracts but must finish before central module removal. |
| S11 | Persistence replacement readiness depends on orchestration, public API and observability ownership. |
| S12 | Service-local domain/application readiness depends on all service-local replacements. |
| S13 | Testbed parity readiness depends on service-local regression parity. |
| S14 | Readiness reconciliation depends on S13 and must complete with no deletion when blockers remain. |
| S15 | Testbed architecture and hardening relocation depends on S14 blocker inventory. |
| S16 | Runtime scenario replacement or deprecation depends on S14 blocker inventory. |
| S17 | Repository checkout and ingestion replacement depends on S14 blocker inventory. |
| S18 | Public API, boot and persistence ownership exit depends on S14 and S11 ownership evidence. |
| S19 | Final deletion depends on S15 through S18 and every path-specific migration/deprecation slice. |
| S20 | Closure depends on final quality gate and documentation alignment. |

## Parallelization Opportunities

S03 through S06 may be parallelized after S02 only when S3D confirms disjoint
file locks and stable contracts. S10 can run after S02 while S07/S08 proceed
only when observability file locks do not overlap. S15 through S18 can run in
parallel only when S3D confirms disjoint `services/testbed`, contract and
architecture locks. Default execution remains one slice at a time.

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
| Retirement readiness and blocker reconciliation | S14 |
| Testbed architecture and hardening relocation | S15 |
| Testbed runtime scenario replacement or deprecation | S16 |
| Repository checkout and ingestion replacement | S17 |
| Public API, boot and persistence ownership exit | S18 |
| Final module deregistration | S19 |
| Closure and release readiness | S20 |
