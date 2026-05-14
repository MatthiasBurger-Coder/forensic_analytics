# 01 - Orchestrator Domain Vocabulary

Status: completed slice.

## Objective

Define the domain and application vocabulary for distributed analysis orchestration before adding queue, worker, store, or server behavior.

## Verified Current Baseline

- `DefaultRunRepositoryAnalysisUseCase` runs repository analysis synchronously.
- The current synchronous order is repository source resolve -> source scan -> semantic analysis -> rule generation -> result store.
- `RepositoryAnalysisStatus` currently only has `COMPLETED`.
- No implemented analysis job lifecycle, worker status model, retry model, or dead-letter model was found.

## Future Target

- Planned vocabulary separates analysis run, analysis job, worker task, source snapshot, canonical evidence, artifact reference, projection, retry attempt, and terminal state.
- Planned statuses must be introduced only after source, tests, and documentation agree on exact semantics.
- Orchestration vocabulary lives inward of adapters and does not depend on queue, database, server, graph, or LLM provider APIs.

## Completed Vocabulary Baseline

- Implemented inward terms verified before this slice: `AnalysisRunId`, `RepositoryMetadata`, `RepositorySource`, `ArtifactReference`, and `RepositoryAnalysisStatus.COMPLETED`.
- Source snapshot terms completed by slice 02: `SourceSnapshotId`, `SourceSnapshotMetadata`, `SourceSnapshot`, and `SourceSnapshotCompleteness`.
- Planned gaps remain explicit: analysis job, worker task, retry attempt, dead-letter state, queue lifecycle, graph projection, report projection, LLM projection, analysis store, artifact store, and server runtime terms are not implemented by this slice.
- `RepositoryAnalysisStatus` remains limited to `COMPLETED`; queued, running, retryable, failed, dead-lettered, partial, and skipped states are intentionally left for slice 03 or later lifecycle work.

## Subagent Roles

- Architecture reviewer: validate hexagonal boundaries and naming.
- Implementation worker: add vocabulary only after tests define expected behavior.
- Quality reviewer: check deterministic status transitions and package boundaries.
- Documentation reviewer: update docs only for verified public terms.

## Implementation Steps

1. Inspect current analysis commands, result objects, ports, and tests.
2. Identify which concepts already exist and which are planned gaps.
3. Write regression tests for any new lifecycle or status behavior before implementation.
4. Add the smallest domain or application vocabulary needed by the next slice.
5. Avoid compatibility aliases or fallback statuses unless explicitly requested and tested.

## Affected Files or Modules to Inspect

- `forensic-analytics-application`
- `forensic-analytics-domain`
- `forensic-analytics-application/src/main/java/de/burger/forensics/analytics/application/analysis/DefaultRunRepositoryAnalysisUseCase.java`
- `forensic-analytics-application/src/main/java/de/burger/forensics/analytics/application/analysis/result/RepositoryAnalysisStatus.java`
- `forensic-analytics-application/src/test`
- `docs/adr/ADR-0002-canonical-analysis-model.md`

## Evidence and Provenance Rules

- Status names must not imply completed evidence when analysis is only queued, running, partial, failed, skipped, or awaiting retry.
- Unknown or unavailable worker output must remain explicit.
- LLM output and projections must not become status evidence.

## Stop Conditions

Stop and report if:

- a planned status name conflicts with an existing status or documented behavior;
- exact lifecycle semantics cannot be verified through tests or accepted docs;
- a queue or storage technology would be required to define core vocabulary.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

For documentation-only vocabulary updates, run:

```bash
git diff --check
```

## Done Criteria

- Vocabulary is documented or implemented in the correct inward layer.
- New statuses or lifecycle terms have tests.
- Current synchronous behavior remains unchanged unless the task explicitly changes it.
