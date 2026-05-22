# Three Amigos Decision Record

## Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-legacy-module-retirement-20260522-v1` |
| Requirement ID | `FA-MSA-001-LMR` |
| Parent requirement | `FA-MSA-001` |
| Decision | `READY_FOR_WORKFLOW` |
| Confidence | 92 percent |
| Execution profile | `FULL_PATH` |

## Normalized Requirement

Create an executable workflow that retires all remaining
`forensic-analytics-*` legacy modules after the platform has been migrated to
the accepted FA-MSA-001 microservice target landscape. The workflow must not
delete modules until caller-free evidence, service-local parity, rollback or
deprecation notes and the applicable `QUALITY.md` gate exist.

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
| Senior Requirement Engineer | APPROVE FOR WORKFLOW. The user intent is clear: if the listed modules cannot be deleted now, create a workflow to make deletion safe. |
| Senior System Architect | APPROVE FOR WORKFLOW with `FULL_PATH`. Direct deletion is blocked by current Gradle and source references; retirement must be sliced by service boundary. |
| Senior Java Backend Developer | APPROVE FOR WORKFLOW. Slices must verify exact ports, use cases, adapters, tests and callers before moving or removing any Java code. |
| Senior React Frontend Developer | N/A for workflow creation; impact check required for query/report API shape changes. |
| Senior Tester | APPROVE FOR WORKFLOW. Final deregistration requires full local quality gate and regression parity for removed testbed coverage. |

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

The workflow is acyclic. S03 through S06 are potential parallel slices after
S02, but execution defaults to one slice at a time unless the orchestrator
confirms disjoint locks.

```text
S00 -> S01 -> S02 -> S03/S04/S05/S06 -> S07 -> S08/S10 -> S09/S11 -> S12 -> S13 -> S14 -> S15
```

## Open Questions

- None block workflow creation.
- During execution, any missing service owner, contract field, rollback path or
  test parity proof stops the relevant slice.

## Blockers

No blocker prevents workflow creation. Direct module deletion is blocked until
the workflow records caller-free evidence and required quality-gate success.

## Decision

`READY_FOR_WORKFLOW`
