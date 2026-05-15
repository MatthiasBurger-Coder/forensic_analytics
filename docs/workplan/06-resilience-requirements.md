# Resilience Requirements

The implementation must apply `.agents/skills/resilience-engineering/SKILL.md`.

## Frontend

Required:

- request timeout handling;
- cancellation on route unmount;
- bounded retry policy for idempotent GET requests;
- exponential backoff with jitter for retryable GET failures;
- no automatic retry for non-idempotent POST analysis start unless idempotency exists;
- backend unavailable view;
- loading states;
- empty states;
- error states;
- global ErrorBoundary;
- route-level ErrorBoundary;
- duplicate submit prevention;
- safe diagnostics rendering;
- no secret leakage;
- stale data handling;
- manual retry button.

Polling:

- poll job status with a bounded interval;
- stop polling on terminal states;
- stop polling on route unmount;
- show stale status if backend becomes unavailable after data was loaded;
- allow manual refresh.

Terminal UI states from the task:

```text
SUCCESS
FAILED
CANCELED
CLEANED
```

Mapping from backend state must be explicit and tested.

## Backend

Required:

- separate validation errors from dependency failures;
- map exceptions into sanitized REST error responses;
- preserve request/correlation IDs when available;
- keep framework wiring out of domain and application models;
- do not retry repository checkout or analysis start in the controller;
- keep missing evidence explicit in response DTOs;
- do not fabricate timestamps, commits, source roots or diagnostics.

If a command endpoint becomes retryable, add an idempotency key or equivalent mechanism first.

## Docker And Runtime

Required:

- explicit container startup behavior;
- nginx SPA fallback;
- deterministic static file serving;
- documented API base URL behavior;
- no secrets in logs or generated frontend bundles;
- health/readiness checks only when backed by meaningful runtime checks.

## Failure Categories

Frontend error mapping should distinguish:

- `VALIDATION_ERROR`
- `NOT_FOUND`
- `BACKEND_UNAVAILABLE`
- `TIMEOUT`
- `RETRY_EXHAUSTED`
- `UNEXPECTED_ERROR`

The user-visible message must be safe and actionable. Full internal causes belong only in protected logs or test-visible diagnostic structures.
