# Distributed Analysis Orchestrator Workplan

Status: active workflow documentation. Slices 00, 01, and 02 are completed. Later slices remain planned and do not describe implemented runtime behavior.

## Purpose

This workplan prepares future subagents to evolve the current synchronous repository analysis flow into a distributed analysis orchestration model while preserving forensic evidence integrity, deterministic output, and hexagonal architecture boundaries.

The planned pipeline is:

```text
forensic-analytics-server
    -> analysis-orchestrator
    -> analysis-job-queue
    -> repository-analysis-worker
       / ast-analysis-worker
       / joern-analysis-worker
       / btm-generation-worker
       / graph-analysis-worker
       / report-worker
    -> workspace-manager
    -> analysis-store / artifact-store
```

## Verified Current Baseline

- Current modules from `settings.gradle.kts`: `forensic-analytics-domain`, `forensic-analytics-application`, `forensic-analytics-engine`, `forensic-analytics-adapter-repository-source`, `forensic-analytics-adapter-javaparser`, `forensic-analytics-adapter-joern-docker`, `forensic-analytics-cli`, `forensic-analytics-testbed`, `forensic-analytics-persistence`, `forensic-analytics-ingestion-grpc`, `forensic-analytics-ingestion-request`, and `forensic-analytics-bootstrap`.
- `DefaultRunRepositoryAnalysisUseCase` currently runs synchronously: repository source resolve -> source scan -> semantic analysis -> rule generation -> result store.
- `RepositoryAnalysisStatus` currently only has `COMPLETED`.
- No implemented queue, retry, dead-letter, worker modules, graph adapter module, report module, durable analysis-store, artifact-store port, or immutable source snapshot model was found.
- Existing workspace/project storage areas are `evidence_original`, `evidence_processed`, `analysis_results`, `reports`, and `logs`.
- `IsolatedProjectStoragePathResolver` confines project and shared paths under workspace/project roots.
- `forensic-analytics-ingestion-grpc` is an inbound adapter. It must not own persistence, Joern execution, replay, LLM behavior, or the final plugin payload schema.
- ADR constraints: plugins are producers; the canonical model comes first; runtime events are sensitive by default; graph and vector databases are projections.

## Target Architecture Direction

- Keep orchestration in application-facing use cases and ports. Concrete workers, queues, storage technologies, server transports, and external tools stay in adapters or infrastructure.
- Treat queue, retry, dead-letter, database, graph, vector, and artifact storage choices as undecided until an ADR and dependency review select them.
- Store canonical facts and job state before building graph, report, LLM, or vector projections.
- Keep each worker deterministic for the same typed input, source snapshot, artifact references, and configuration.
- Represent unavailable facts as unknown, incomplete, unresolved, skipped, or failed. Do not fabricate runtime facts or static relationships.

## Slice Execution Order

| Order | Slice | Status | Purpose |
| --- | --- | --- | --- |
| 00 | [Documentation Baseline Alignment](00-documentation-baseline-alignment.md) | Completed | Keep documentation entry points and workflow ownership aligned. |
| 01 | [Orchestrator Domain Vocabulary](01-orchestrator-domain-vocabulary.md) | Completed | Define planned orchestration terms before queue and worker changes. |
| 02 | [Source Snapshots and Workspaces](02-source-snapshots-and-workspaces.md) | Completed | Define immutable source snapshot vocabulary and workspace-managed raw source artifact paths. |
| 03 | [Analysis Job Queue and Retry](03-analysis-job-queue-and-retry.md) | Planned | Plan queue-neutral job lifecycle, retry, and dead-letter behavior. |
| 04 | [Typed Worker Contracts](04-typed-worker-contracts.md) | Planned | Plan worker input/output contracts for distributed execution. |
| 05 | [Analysis Store and Artifact Store](05-analysis-store-and-artifact-store.md) | Planned | Plan canonical storage and artifact reference boundaries. |
| 06 | [Graph, Report, and LLM Projections](06-graph-report-and-llm-projections.md) | Planned | Plan projections derived from canonical stored evidence. |
| 07 | [Server API and Distributed Runtime](07-server-api-and-distributed-runtime.md) | Planned | Plan server-facing orchestration API and runtime wiring. |
| 08 | [Quality, CI, and Rollout](08-quality-ci-and-rollout.md) | Planned | Plan verification, rollout order, and CI protection. |

## Completed Slice Evidence

- 00 completed: `docs/README.md` points to `docs/workplan`, and `docs/workplan/README.md` indexes the master workflow and all slice files.
- 01 completed: orchestration vocabulary is documented with verified implemented terms and planned gaps; no queue lifecycle, retry, dead-letter, or worker status was introduced in this slice.
- 02 completed: source snapshot domain vocabulary now models deterministic snapshot identity, optional revision metadata, artifact provenance, completeness, and limitations; workspace path resolution maps source snapshot artifacts into `evidence_original`.

## Subagent Coordination Rules

- Assign one implementation slice at a time to a write-capable worker.
- Give each subagent the slice file, `AGENTS.md`, `QUALITY.md`, relevant ADRs, and the exact write scope.
- Require read-only verification before edits.
- Require the subagent to report inspected files, changed files, commands run, and remaining risks.
- Do not let a slice select PostgreSQL, Neo4j, Kafka, RabbitMQ, Redis, S3, MinIO, or any other concrete technology without a dedicated ADR and dependency review.
- Do not let gRPC ingestion own orchestration, persistence, Joern execution, replay, LLM, graph projection, or report generation.

## Global Evidence and Provenance Rules

- Preserve source of evidence, source snapshot identity, analysis run identity, worker identity, artifact references, ordering, and completeness state where available.
- Keep raw evidence separate from processed evidence, projections, reports, and generated hypotheses.
- Treat runtime event values as sensitive by default.
- Ensure graph, report, LLM, and vector outputs are projections from canonical evidence, not primary evidence.
- Make retries idempotent. Retried workers must not duplicate canonical facts or overwrite original evidence.

## Global Stop Conditions

Stop and report before implementation if:

- a referenced class, port, module, task, status, field, artifact location, or storage concept cannot be verified;
- documentation and source disagree about the current baseline;
- a planned slice requires a technology choice that has no ADR or dependency review;
- a worker contract would need to infer missing runtime facts or unresolved static symbols;
- implementing the slice would require edits outside the approved write scope.

## Verification Baseline

For documentation-only changes, run at least:

```bash
git diff --check
```

For implementation slices, start with the narrowest relevant test and then use the repository quality command from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Before release or merge of implementation slices, run the full local gate when feasible:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```
