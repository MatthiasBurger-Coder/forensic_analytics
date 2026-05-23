# Three Amigos Decision Record

## Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-legacy-module-retirement-20260522-v2` |
| Requirement ID | `FA-MSA-001-LMR` |
| Parent requirement | `FA-MSA-001` |
| Decision | `READY_FOR_WORKFLOW` after S14 workflow-create refinement |
| Confidence | 94 percent for refined no-deletion S14 readiness gate and S15-S20 follow-up sequence |
| Execution profile | `FULL_PATH` |

## Normalized Requirement

Create an executable workflow that retires all remaining
`forensic-analytics-*` legacy modules after the platform has been migrated to
the accepted FA-MSA-001 microservice target landscape. The workflow must not
delete modules until caller-free evidence, service-local parity, rollback or
deprecation notes and the applicable `QUALITY.md` gate exist.

2026-05-22 refinement: workflow execution of S03 proved the previous
early-retirement shape was unsafe. The target requirement is now normalized as:
by closure, no productive `forensic-analytics-*` Gradle module remains
registered or referenced; every removed behavior must first be implemented in
the owning target service or explicitly deprecated with tests, migration notes
and rollback evidence.

2026-05-22 S14 refinement: workflow execution reached the final deletion gate
and all S14 reviewers stopped deletion. Productive services are clean under the
checked main-source and productive-build scans, but `services:testbed` still
keeps 13 test-scoped legacy dependencies and legacy imports as
rollback/regression evidence. S14 is therefore refined into a
`NO_REMOVAL_SAFE` readiness reconciliation gate. S15 through S18 now own the
remaining testbed, runtime, public API, boot/bootstrap and persistence exit
work. S19 is the first deletion-capable slice, and S20 is closure.

## Requirement Classification

- Functional requirement: migrate remaining legacy runtime behavior to
  service-local boundaries.
- Architecture constraint: no shared Java implementation modules and no direct
  service-to-service Gradle project dependencies.
- Quality-gate requirement: targeted checks per slice, repository minimum gate
  for product changes and full local gate before final removal.
- Data ownership requirement: every persisted or stored category has one owner
  before `forensic-analytics-persistence` is removed.
- Deployment requirement: service runtime readiness must be proven before boot
  and bootstrap paths are removed.
- Security requirement: repository workspaces, diagnostics and runtime payloads
  stay isolated and redacted.
- Observability requirement: service-local diagnostics replace shared logging
  or observability Java modules.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | READY AFTER S14 REFINEMENT. S14 must record readiness and blockers, not delete; follow-up slices must resolve testbed/runtime blockers before removal. |
| Senior System Architect | READY AFTER S14 REFINEMENT. S14 conflicts with architecture docs when deletion-oriented; removal moves to S19 after S15-S18. |
| Senior Java Backend Developer | READY WITH STOP CONDITIONS. Testbed scenarios cannot be directly ported without fabricating parity; target-service behavior must be implemented or explicitly deprecated first. |
| Senior React Frontend Developer | NO DIRECT FRONTEND IMPLEMENTATION. S08/S09 must preserve or version public API and CLI behavior before caller migration. |
| Senior Tester | READY WITH TESTBED GATES. Existing regression coverage may be removed only after service-local parity or explicit deprecation tests exist. |

## Specialist Findings

| Specialist | Finding |
|---|---|
| Microservice Senior Expert | Legacy modules are not microservices. Every target service must own local domain/application/adapter/bootstrap code and avoid shared Java modules. |
| Contract-First API Steward | Runtime and CLI callers must move behind REST/OpenAPI, gRPC, event or file contracts before in-process adapters are removed. |
| Data Ownership and Persistence Steward | `forensic-analytics-persistence` removal is unsafe until service-local ownership and replacement behavior are proven. |
| Senior DevOps Engineer | `settings.gradle.kts` deregistration and source-tree deletion belong near the end, after all targeted service checks and the full quality gate. |
| Senior Security/Sandbox Engineer | Repository checkout and Joern work require isolation and leakage checks before legacy paths are retired. |

## Architecture And Evidence Integrity Validation

- ADR-0017 defines FA-MSA-001 target services and forbids shared Java
  implementation modules.
- `settings.gradle.kts` still registers all legacy modules.
- Build files still contain direct `project(":forensic-analytics-*")`
  dependencies.
- Current coupling documentation records non-empty production and test callers.
- Static analysis facts, runtime trace facts, reports and LLM hypotheses must
  remain distinct throughout migration.

## Dependency And Deadlock Validation

The refined workflow is acyclic. S03 through S06 are service parity and handoff
readiness slices after S02, not deletion slices. S10 can also run after S02.
Execution defaults to one slice at a time unless the orchestrator confirms
disjoint locks.

```text
S00 -> S01 -> S02 -> S03/S04/S05/S06/S10
S03/S04/S05/S06 -> S07 -> S08 -> S09
S07/S08/S10 -> S11 -> S12 -> S13 -> S14(no-deletion readiness)
S14 -> S15/S16/S17/S18 -> S19(final removal) -> S20
```

## Open Questions

- Whether local/file repository input remains target behavior or becomes
  explicit deprecation is resolved inside S03 before repository-source removal.
- During execution, any missing service owner, contract field, rollback path or
  test parity proof stops the relevant slice.

## Blockers

No blocker prevents workflow refinement. Direct module deletion remains blocked
until S15 through S18 remove or deprecate the remaining testbed/runtime/public
API/ownership blockers and S19 records caller-free evidence, rollback notes and
required quality-gate success.

## Decision

`READY_FOR_WORKFLOW` after S14 refinement. Resume execution at S14 as a
no-deletion retirement readiness reconciliation slice, then continue through
S15-S18 before any S19 removal candidate is attempted.
