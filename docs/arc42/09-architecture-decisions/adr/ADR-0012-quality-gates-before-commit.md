# ADR-0012: Require quality gates before commit and push

## Status

Accepted

## Context

Forensic Analytics depends on deterministic, evidence-preserving behavior. `QUALITY.md` defines the authoritative quality gates and applies to source, tests, documentation, build logic and workflows.

## Decision

Required quality gates must pass before commit and push readiness.

Failed required gates are blocking. Optional, unavailable or not-applicable checks may be documented as non-blocking only when they are not required by `QUALITY.md`, the active workflow or CI policy.

## Consequences

- `git diff --check` is useful for documentation slices but does not replace required Gradle gates.
- Commit and push governance must include exact command evidence.
- Quality failures need failure reports with owner and next action.
- Coverage, ArchUnit and dependency verification thresholds must not be weakened to pass a slice.
