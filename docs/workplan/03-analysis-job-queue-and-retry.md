# 03 - Analysis Job Queue and Retry

Status: completed contract-baseline slice.

## Objective

Define a queue-neutral analysis job lifecycle with retry and dead-letter semantics without selecting queue technology.

## Verified Current Baseline

- No concrete queue product, queue runtime, or worker module is implemented.
- A technology-neutral job contract now exists in `de.burger.forensics.analytics.domain.analysis`.
- `RepositoryAnalysisStatus` currently only has `COMPLETED`.
- `DefaultRunRepositoryAnalysisUseCase` executes all analysis steps in one synchronous call.

## Future Target

- Planned `analysis-job-queue` is a port-level capability, not a selected product.
- Planned job lifecycle records accepted, dispatchable, running, retryable, failed, dead-lettered, and completed states only after exact names and transitions are approved.
- Retry attempts are deterministic and idempotent for the same job input and artifact references.
- Dead-letter records preserve failure cause, worker kind, attempt count, input references, and evidence completeness state.

## Completed Contract Baseline

- `AnalysisJobState` defines the approved states: `ACCEPTED`, `DISPATCHABLE`, `RUNNING`, `RETRYABLE`, `FAILED`, `DEAD_LETTERED`, and `COMPLETED`.
- `AnalysisJobId`, `AnalysisJob`, `AnalysisJobFailure`, and `DeadLetterProvenance` model lifecycle identity, transition validation, retry failure provenance, attempt counts, input references, and completeness.
- Tests cover accepted-to-completed transitions, retry dispatch, dead-letter provenance, invalid transitions, and missing required references.
- No queue product, asynchronous dispatcher, worker process, server runtime, or changes to the synchronous repository analysis use case were introduced.

## Subagent Roles

- Architecture reviewer: verify queue abstractions stay inward and technology-neutral.
- Implementation worker: add port and lifecycle behavior only after tests define transitions.
- Quality reviewer: test retry limits, idempotency, and failure provenance.
- Documentation reviewer: document queue behavior only after implementation exists.

## Implementation Steps

1. Inspect analysis use cases, result store ports, command/result objects, and tests.
2. Define job lifecycle transitions in tests before adding implementation.
3. Add a technology-neutral queue port and in-memory adapter only if explicitly scoped.
4. Model retry and dead-letter outcomes without swallowing worker failures.
5. Keep concrete queue products out of the slice until an ADR and dependency review choose one.

## Affected Files or Modules to Inspect

- `forensic-analytics-application`
- `forensic-analytics-domain`
- `forensic-analytics-engine`
- `forensic-analytics-persistence`
- `forensic-analytics-application/src/main/java/de/burger/forensics/analytics/application/analysis/result/RepositoryAnalysisStatus.java`
- `forensic-analytics-application/src/test`
- `docs/adr`

## Evidence and Provenance Rules

- Job state must never imply worker output exists before it is stored.
- Retry must not duplicate canonical evidence, source snapshots, or artifacts.
- Dead-letter state must preserve enough provenance for audit and later replay of the failure.

## Stop Conditions

Stop and report if:

- status transition names or terminal semantics are unclear;
- a queue product is required before the port contract is proven;
- retry behavior would require mutating original evidence;
- failures would be hidden behind a generic completed status.

## Verification Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run targeted domain lifecycle tests first when available.

## Done Criteria

- Queue-neutral contract is implemented without a queue product.
- Retry and dead-letter behavior is tested.
- Job states distinguish accepted, dispatchable, running, retryable, failed, dead-lettered, and completed outcomes as approved.
- Existing synchronous behavior remains covered and unchanged during this contract-baseline slice.
