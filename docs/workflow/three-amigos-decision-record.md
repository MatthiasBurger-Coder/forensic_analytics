# Three Amigos Decision Record

## Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-final-legacy-source-retirement-20260523-v1` |
| Requirement ID | `FA-MSA-001-LMR-FINAL` |
| Parent requirement | `FA-MSA-001` |
| Decision | `READY_FOR_WORKFLOW` |
| Confidence | 93 percent |
| Execution profile | `FULL_PATH` |

## Normalized Requirement

Create an executable workflow that removes the remaining tracked
`forensic-analytics-*` legacy source trees now that the active Gradle build is
service-only. The workflow must preserve evidence integrity, avoid shared Java
implementation modules, classify stale documentation and contract references,
and prove the repository remains buildable and testable after deletion.

## Requirement Classification

- Functional requirement: remove tracked legacy source trees.
- Architecture constraint: services remain autonomous and do not share Java
  implementation modules.
- Quality-gate requirement: service tests, root minimum gate and full local
  gate are required before closure.
- Documentation requirement: arc42, architecture maps, README, testing docs,
  contracts and workflow docs must stop claiming stale legacy runtime/build
  facts.
- Contract requirement: compatibility labels in OpenAPI/CLI/gRPC material must
  be classified before removal or rewrite.
- Security and evidence requirement: source-tree deletion must not fabricate
  runtime readiness, trace evidence, persistence ownership or regression
  coverage.

## Five-Role Review

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The request is clear enough for workflow creation. The active EPIC/ADR direction matches service-local migration, but docs have drifted from current build state. |
| Senior System Architect | The previous workflow is stale. The legacy directories are tracked source trees, not active Gradle modules. Deletion must be paired with documentation, Docker and contract reference cleanup. |
| Senior Java Backend Developer | Active services have no direct legacy Gradle or Java import coupling. Compile risk is low, but deletion removes module-local tests and therefore requires service regression confirmation. |
| Senior React Frontend Developer | `forensic-ui` exists and has no direct legacy module references. Frontend work is not required unless public API shape changes. |
| Senior Tester | All stale `:forensic-analytics-*` gates must be removed. Use service/root gates, leakage scans, `git diff --check`, minimum gate and full local gate. |

## Specialist Findings

| Specialist | Finding |
|---|---|
| Microservice Senior Expert | The current `services:*` model must remain free of shared Java implementation modules and direct service-to-service project dependencies. |
| Contract Governance | Contract compatibility vocabulary may remain only when explicitly classified; behavior-changing contract edits require contract review. |
| Data Ownership and Persistence | Persistence ownership claims must not be inferred from source deletion; unresolved ownership must stay documented as a gap or explicit deprecation. |
| Senior DevOps | `./gradlew projects` verified the service-only model. Docker and `.dockerignore` references to `forensic-analytics-boot-app` must be removed or rewritten before closure. |
| Senior Documentation Engineer | arc42 and architecture docs require closure updates after deletion so they do not claim legacy modules remain active rollback/build units. |

## Architecture And Evidence Integrity Validation

- The active build includes only `services:*` projects.
- The legacy directories remain tracked by Git.
- Productive service build and source scans found no active legacy coupling.
- Documentation and runtime-support files still contain legacy references.
- Removing tracked source trees is safe only after references are classified
  and regression coverage is confirmed.
- Service runtime readiness, Docker readiness and persistence ownership must be
  documented from verified service evidence, not from deletion alone.

## Dependency And Deadlock Validation

The workflow is acyclic:

```text
S00 -> S01
S01 -> S02
S01 -> S03
S02 + S03 -> S04
S04 -> S05
S05 -> S06
```

S02 and S03 may run in parallel only after S3D confirms disjoint file locks.
S04 through S06 are sequential.

## Open Questions

- Which legacy contract words are historical compatibility vocabulary and
  which are stale implementation references?
- Which module-local tests are superseded by service tests, and which behavior
  is explicitly deprecated?
- Whether a final ADR is added or an existing ADR is superseded is decided in
  S05 by the ADR steward and Senior System Architect.

## Blockers

No blocker prevents workflow creation. Execution must stop if any open question
above becomes behavior-changing, contract-changing, or quality-gate relevant
and cannot be resolved from repository evidence.

## Decision

`READY_FOR_WORKFLOW`.
