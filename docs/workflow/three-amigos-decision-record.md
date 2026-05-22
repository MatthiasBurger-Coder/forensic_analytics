# Three Amigos Decision Record

## Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-legacy-module-retirement-20260522-v1` |
| Requirement ID | `FA-MSA-001-LMR` |
| Parent requirement | `FA-MSA-001` |
| Decision | `REQUIRES_REFINEMENT` resolved by workflow-create refinement |
| Confidence | 92 percent for target, 64 percent for previous S03 deletion shape |
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
| Senior Requirement Engineer | REQUIRES REFINEMENT FOR S03. The end goal is correct, but early retirement slices must become parity, handoff and caller-migration slices before removal. |
| Senior System Architect | REQUIRES REFINEMENT. S03 mixed service parity, caller migration and physical deletion; removal must move to the final S14 gate. |
| Senior Java Backend Developer | REQUIRES REFINEMENT. Consumers such as the orchestrator, Boot, Bootstrap and testbed must be rewired or retired before adapter deletion. |
| Senior React Frontend Developer | NO DIRECT FRONTEND IMPLEMENTATION. S08/S09 must preserve or version public API and CLI behavior before caller migration. |
| Senior Tester | REQUIRES REFINEMENT. Existing regression coverage may be removed only after service-local parity or explicit deprecation tests exist. |

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
S07/S08/S10 -> S11 -> S12 -> S13 -> S14(final removal) -> S15
```

## Open Questions

- Whether local/file repository input remains target behavior or becomes
  explicit deprecation is resolved inside S03 before repository-source removal.
- During execution, any missing service owner, contract field, rollback path or
  test parity proof stops the relevant slice.

## Blockers

No blocker prevents workflow refinement. Direct module deletion is blocked
until S14 records caller-free evidence, replacement or deprecation parity,
rollback notes and required quality-gate success.

## Decision

`READY_FOR_WORKFLOW` after refinement. Resume execution at S03 as a parity and
handoff readiness slice, not as a deletion slice.
