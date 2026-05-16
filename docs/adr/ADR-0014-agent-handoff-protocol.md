# ADR-0014: Use an explicit agent handoff protocol

## Status

Accepted

## Context

The repository uses workflow slices, role reviews and callable subagents for non-trivial work. Parallel or delegated work can deadlock or overwrite changes without explicit ownership.

## Decision

Agent and role handoffs must use an explicit protocol with:

- source agent or role
- target agent or role
- slice ID
- input artifacts
- output artifacts
- assumptions
- known risks
- blockers
- validation status
- next action

Parallel work requires disjoint file ownership and merge order.

## Consequences

- Handoffs become auditable workflow artifacts.
- Blockers must be classified and owned.
- Reviewers may block but must provide resolution steps.
- No agent may wait for an artifact without a known owner.
