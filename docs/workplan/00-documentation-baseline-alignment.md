# 00 - Documentation Baseline Alignment

Status: completed slice.

## Objective

Align documentation entry points so future subagents can find the distributed analysis-orchestrator workplan without relying on the absent `docs/workflows` directory.

## Verified Current Baseline

- `docs/README.md` references `docs/workflows`.
- No `docs/workflows` directory was found.
- `docs/workplan` is the planned location for distributed analysis-orchestrator workflow files.
- Repository documentation must be English.

## Future Target

- `docs/README.md` points to `docs/workplan`.
- `docs/workplan/README.md` indexes all distributed analysis-orchestrator slices.
- Future workflow documents distinguish verified current baseline from planned behavior.

## Completed Outcome

- `docs/README.md` points to `docs/workplan`.
- `docs/workplan/README.md` indexes the master workflow and all distributed analysis-orchestrator slice files.
- `docs/workplan/workflow.md` records completed and planned slice status without claiming that later distributed runtime behavior is implemented.

## Subagent Roles

- Documentation reviewer: verify doc links and terminology.
- Implementation worker: update only approved documentation paths.
- Quality reviewer: run documentation sanity checks.

## Implementation Steps

1. Inspect `docs/README.md`, `docs/workplan`, `AGENTS.md`, and `QUALITY.md`.
2. Replace stale references to absent workflow directories with verified workplan links.
3. Add or update workplan index entries.
4. Keep all statements in English and avoid claiming planned concepts are implemented.

## Affected Files or Modules to Inspect

- `docs/README.md`
- `docs/workplan/README.md`
- `docs/workplan/workflow.md`
- `AGENTS.md`
- `QUALITY.md`

## Evidence and Provenance Rules

- Documentation must identify which facts were verified from repository files.
- Documentation must not present planned queues, workers, stores, or server modules as implemented.

## Stop Conditions

Stop and report if:

- another documentation index contradicts `docs/README.md`;
- a referenced workplan file is absent;
- the requested edit would require source, build, arc42, ADR, or epic changes.

## Verification Commands

```bash
git diff --check
```

If implementation code changes are added in a later task, also run:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Done Criteria

- `docs/README.md` links to `docs/workplan`.
- `docs/workplan/README.md` lists the master workflow and all slice files.
- No source, build, arc42, ADR, or epic files were changed.
