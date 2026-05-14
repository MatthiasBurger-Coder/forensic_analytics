# 05 - Analysis Store and Artifact Store

Status: completed contract-baseline slice.

## Objective

Define canonical analysis storage and artifact storage boundaries without selecting database or object-store technology.

## Verified Current Baseline

- `DefaultRunRepositoryAnalysisUseCase` stores the completed synchronous result through `RepositoryAnalysisResultStore`.
- Existing workspace/project storage concepts include `evidence_original`, `evidence_processed`, `analysis_results`, `reports`, and `logs`.
- `AnalysisStorePort` and `ArtifactStorePort` now define technology-neutral application storage boundaries.
- Graph and vector databases are documented as projections, not the source of truth.

## Future Target

- Planned `analysis-store` owns canonical analysis run state, job state, evidence metadata, diagnostics, and provenance.
- Planned `artifact-store` owns immutable artifact bytes or file references for source snapshots, processed evidence, Joern artifacts, generated rules, reports, logs, and projection inputs.
- Stores expose application ports. Concrete SQL, file, object-store, graph, or vector products remain undecided until ADR and dependency review.
- Graph, report, LLM, and vector outputs reference canonical analysis-store records and artifact-store entries.

## Completed Contract Baseline

- `AnalysisStorePort` stores and reads job records by run and job identity.
- `ArtifactStorePort` stores and reads artifact records with run, job, worker kind, attempt, storage area, purpose, sensitivity, and typed artifact category.
- `InMemoryAnalysisStore` and `InMemoryArtifactStore` provide minimal persistence adapters for deterministic contract and idempotency tests.
- Artifact writes are idempotent for identical canonical records; conflicting metadata for the same analysis run and artifact path fails explicitly through `ArtifactStoreConflictException`.
- No database, object store, graph store, vector store, file-writing adapter, or runtime wiring was selected or introduced.

## Subagent Roles

- Architecture reviewer: verify store ports and projection boundaries.
- Implementation worker: add store interfaces and minimal adapters only within approved scope.
- Quality reviewer: test deterministic identity, idempotent writes, and provenance retention.
- Security reviewer: review sensitive runtime value handling and retention boundaries.
- Documentation reviewer: update store documentation after behavior is implemented.

## Implementation Steps

1. Inspect existing result store ports, persistence adapters, workspace storage, and tests.
2. Define canonical records and artifact references before choosing backend technology.
3. Add tests for idempotent writes, duplicate handling, missing artifact behavior, and stable ordering.
4. Keep raw evidence, processed evidence, analysis results, reports, and logs in explicit storage areas.
5. Add concrete persistence only after ADR and dependency review approve the technology.

## Affected Files or Modules to Inspect

- `forensic-analytics-application`
- `forensic-analytics-domain`
- `forensic-analytics-persistence`
- `forensic-analytics-testbed`
- `forensic-analytics-domain/src/main/java/de/burger/forensics/analytics/domain/workspace/ProjectStorageArea.java`
- `forensic-analytics-persistence/src/main/java/de/burger/forensics/analytics/persistence/storage/IsolatedProjectStoragePathResolver.java`
- `docs/adr/ADR-0002-canonical-analysis-model.md`
- `docs/adr/ADR-0003-runtime-events-are-sensitive.md`
- `docs/adr/ADR-0004-graph-and-vector-db-as-projections.md`

## Evidence and Provenance Rules

- Canonical store records must distinguish observed facts, derived facts, unresolved gaps, failures, projections, and generated hypotheses.
- Artifact references must be stable and must not hide missing files.
- Runtime values remain sensitive by default and must not be indexed into unsafe projections.

## Stop Conditions

Stop and report if:

- current result storage contracts cannot be reconciled with planned canonical storage;
- backend selection is requested without ADR and dependency review;
- graph or vector storage is treated as the canonical source of truth;
- artifact writes could escape workspace/project roots.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run targeted persistence and storage path tests first when available.

## Done Criteria

- Store boundaries are ports first and technology-neutral.
- Canonical records and artifacts preserve provenance.
- Idempotent write behavior and explicit conflict failure are tested.
- Existing storage path confinement remains verified by existing storage-path tests.
