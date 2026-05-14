# 02 - Source Snapshots and Workspaces

Status: completed slice.

## Objective

Plan immutable source snapshots and workspace-managed paths so distributed workers analyze the same explicit input instead of re-reading mutable repository state.

## Verified Current Baseline

- No immutable source snapshot model was found.
- `ProjectStorageArea` defines `evidence_original`, `evidence_processed`, `analysis_results`, `reports`, and `logs`.
- `IsolatedProjectStoragePathResolver` confines resolved paths under workspace/project roots.
- The current repository analysis flow resolves repository source directly during synchronous execution.

## Future Target

- Planned workspace-manager creates and records immutable source snapshots before worker execution.
- Planned workers receive source snapshot references, not uncontrolled local paths.
- Snapshot metadata preserves repository identity, source roots, capture time when available, input origin, completeness state, and unresolved source limitations.
- Storage areas remain explicit; raw source evidence and processed artifacts stay separate.

## Completed Outcome

- `SourceSnapshotMetadata` preserves project identity, repository location, optional branch, optional commit hash, and optional capture time without inventing unavailable revision facts.
- `SourceSnapshot` preserves deterministic identity, source artifact provenance, source roots, completeness state, and explicit limitations.
- `SourceSnapshotCompleteness` distinguishes complete, incomplete, and unknown snapshot completeness without introducing worker or queue lifecycle states.
- `IsolatedProjectStoragePathResolver.sourceSnapshotArtifact` resolves snapshot artifacts under the existing `evidence_original` project storage area and keeps processed outputs in separate storage areas.
- No object store, database, queue product, graph projection, report projection, LLM projection, or distributed worker runtime was selected or introduced.
- The current synchronous repository analysis flow remains unchanged.

## Subagent Roles

- Architecture reviewer: verify workspace boundaries and domain/application ownership.
- Implementation worker: add snapshot model and ports only after current source acquisition contracts are verified.
- Quality reviewer: test path confinement, determinism, and missing metadata handling.
- Documentation reviewer: update workspace docs only after behavior exists.

## Implementation Steps

1. Inspect repository source ports, source metadata, workspace models, persistence storage, and tests.
2. Define snapshot identity and artifact reference behavior without choosing object-store technology.
3. Add tests for path confinement, deterministic snapshot references, and missing optional metadata.
4. Wire source snapshot creation ahead of planned distributed worker execution.
5. Preserve the current synchronous flow until the orchestrator migration slice explicitly changes it.

## Affected Files or Modules to Inspect

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-persistence`
- `forensic-analytics-domain/src/main/java/de/burger/forensics/analytics/domain/workspace/ProjectStorageArea.java`
- `forensic-analytics-persistence/src/main/java/de/burger/forensics/analytics/persistence/storage/IsolatedProjectStoragePathResolver.java`
- `docs/adr/ADR-0002-canonical-analysis-model.md`

## Evidence and Provenance Rules

- A worker must be able to trace every output to a specific source snapshot or report that no snapshot was available.
- Mutable checkout paths must not be treated as durable evidence.
- Missing commit, branch, or revision metadata must be represented as unavailable, not invented.

## Stop Conditions

Stop and report if:

- current source metadata cannot identify the analyzed input well enough for a snapshot contract;
- path confinement tests fail or are absent and cannot be added;
- implementation requires choosing an artifact storage backend without ADR and dependency review.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run narrower workspace or repository-source tests first when available.

## Done Criteria

- Snapshot behavior is explicit and deterministic.
- Workspace paths remain confined.
- Raw source evidence and processed artifacts remain separated.
- Current users still have a verified migration path from synchronous source resolution.
