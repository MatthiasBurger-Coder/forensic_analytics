# 04 - Typed Worker Contracts

Status: planned slice.

## Objective

Plan typed input and output contracts for distributed workers so each worker consumes explicit evidence references and produces deterministic results.

## Verified Current Baseline

- Current analysis runs synchronously inside `DefaultRunRepositoryAnalysisUseCase`.
- Existing ports cover repository source resolution, source scanning, semantic analysis, rule generation, and result storage.
- No implemented `repository-analysis-worker`, `ast-analysis-worker`, `joern-analysis-worker`, `btm-generation-worker`, `graph-analysis-worker`, or `report-worker` modules were found.

## Future Target

- Planned workers map to typed contracts:
  - `repository-analysis-worker` prepares source snapshot and repository metadata.
  - `ast-analysis-worker` scans source facts.
  - `joern-analysis-worker` creates semantic artifacts and semantic graph facts.
  - `btm-generation-worker` generates runtime instrumentation rules or planned rule artifacts.
  - `graph-analysis-worker` builds graph projections from canonical evidence.
  - `report-worker` builds reports from canonical evidence and projections.
- Worker contracts carry analysis run ID, job ID, source snapshot reference, input artifact references, output artifact references, worker version, diagnostics, and completeness state.
- Worker implementation modules remain adapters or infrastructure; domain/application contracts remain provider-neutral.

## Subagent Roles

- Architecture reviewer: validate worker boundary direction.
- Implementation worker: add one worker contract group per approved slice.
- Quality reviewer: test serialization, idempotency, and deterministic output ordering.
- Documentation reviewer: keep worker docs aligned with implemented contracts.

## Implementation Steps

1. Inspect existing analysis ports and their test fixtures.
2. Define typed request/result contracts for one worker at a time.
3. Add contract tests that reject missing required evidence references.
4. Adapt current synchronous orchestration through contracts only after behavior remains equivalent.
5. Add worker modules only after `settings.gradle.kts` changes are explicitly approved in a future implementation task.

## Affected Files or Modules to Inspect

- `forensic-analytics-application`
- `forensic-analytics-domain`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-engine`
- `forensic-analytics-testbed`
- `settings.gradle.kts`

## Evidence and Provenance Rules

- Worker output must reference its exact inputs.
- Static facts, semantic facts, runtime rules, graph projections, reports, and LLM hypotheses must remain distinguishable.
- Unsupported files, unresolved symbols, skipped Joern output, and missing artifacts must be explicit diagnostics.

## Stop Conditions

Stop and report if:

- an existing port cannot be mapped to a typed worker contract without changing semantics;
- a worker would need to infer runtime execution from static source facts;
- a module addition is needed but write scope does not include build files;
- serialization format or schema ownership is unclear.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run targeted contract and adapter tests first when available.

## Done Criteria

- Each implemented worker contract has tests and explicit provenance.
- Worker inputs and outputs are typed, deterministic, and provider-neutral.
- No concrete queue or worker runtime is required for contract tests.
