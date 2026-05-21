# Three Amigos Decision Record

## Decision

`READY_FOR_WORKFLOW`

## Requirement Summary

Create an executable governance workflow for reducing repeated full governance
cost while preserving strict repository safety. The workflow covers:

- execution profile routing;
- quality-impact classification;
- workflow context packs;
- machine-readable slice metadata;
- dedicated S3D execution orchestration;
- persistent skill registry evidence;
- branch strategy unification;
- flowchart integrity auditing;
- workflow-executor resolution cleanup;
- process-performance profiling.

## Requirement Classification

| Type | Classification |
|---|---|
| Functional requirement | Add process capabilities that classify requests, gates and workflow metadata. |
| Architecture constraint | Preserve three process strands, branch-first workflow creation, workflow-execute S3/S3D safety and `QUALITY.md` authority. |
| Quality requirement | Reduce unnecessary checks only when affected files cannot influence product build, runtime behavior, contracts, tests, architecture or quality rules. |
| Documentation requirement | Keep AGENTS, QUALITY, routing, process docs, skill registry, workflow docs, arc42 and ADR references synchronized. |
| Security requirement | Do not allow context packs, caches or metrics to record secrets, raw evidence, prompt content or source payloads. |
| Observability requirement | Add process metrics as diagnostics, not as evidence that a workflow is correct. |
| Non-goal | Product runtime behavior, product APIs, persistence, Docker/runtime, Java source and frontend source are out of scope. |

## Mandatory Role Findings

| Role | Finding |
|---|---|
| Senior Requirement Engineer | The request is clear and process-governance scoped. It does not change the product EPIC. Acceptance criteria can be expressed as verified skills, routing rules, documentation synchronization and quality checks. |
| Senior System Architect | The workflow targets accepted governance architecture in ADR-0015, ADR-0016, ADR-0020 and ADR-0021. The main risks are accidental weakening of gates, project-specific leakage into `.codex`, and branch-rule conflicts. |
| Senior Java Backend Developer | Backend product impact is N/A. Any Java source, contract, build or product test change is outside this workflow and must stop unless the workflow is refined. |
| Senior React Frontend Developer | Frontend product impact is N/A. Any React source or frontend adapter change is outside this workflow and must stop unless the workflow is refined. |
| Senior Tester | The workflow is testable through diff checks, JSON validation, targeted registry/routing inspections, flowchart integrity review and quality-impact rules. Gradle is required only if a slice affects build-influencing files, which this workflow forbids by default. |

## Dependency And Deadlock Review

The request contains multiple governance slices with shared files. The workflow
therefore requires concrete slice IDs, file locks, architecture locks and
serial final synchronization.

Primary dependency controls:

- S01 `execution-profile-router` precedes quality-impact and most routing work.
- S02 `quality-impact-classifier` precedes quality matrix updates.
- S03 `context-pack` and S04 `slice metadata` precede the dedicated S3D split.
- S05 `S3D execution orchestrator` waits for machine-readable metadata.
- S11 final synchronization waits for all governance slices.

Potential deadlocks are controlled by S3D file locks and by the rule that
`workflow execute` must not rewrite `workflow create` artifacts during
execution.

## Architecture And Evidence Integrity Validation

This is not evidence-processing code. The forensic evidence principle still
applies to process artifacts:

- Context packs and registry caches are summaries, not source-of-truth files.
- Metrics are operational diagnostics, not proof of correctness.
- Planned skills and routes must not be described as implemented until their
  slices are executed.
- Unknown or stale hashes stop reuse and require rereading governing files.

## Quality And Verification Validation

`QUALITY.md` remains authoritative.

Verified commands:

- Minimum:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`
- Full local:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`

For this governance-only workflow, every slice requires `git diff --check`.
JSON-producing slices also require `python3 -m json.tool`. Product build gates
are not required unless the workflow scope changes into source, tests, build
logic, contracts or `QUALITY.md`.

## Open Questions

No blocking question remains for workflow creation.

Non-blocking decisions are assigned to slices:

- whether workflow-executor front-matter renaming is safe or whether explicit
  resolution text is safer;
- whether root `AGENTS.md` must change for branch unification or whether a
  narrower cross-reference is enough;
- whether closing Flowchart Integrity Audit requires a new ADR or only arc42
  risk-status synchronization.

## Final Decision

`READY_FOR_WORKFLOW`

The workflow may be handed to `workflow execute` after the workflow package is
accepted on branch `architecture/workflow-governance-performance-20260521`.
