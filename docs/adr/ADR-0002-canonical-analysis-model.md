# ADR-0002: Use a canonical analysis model

## Status

Accepted

## Context

The platform must correlate JavaParser facts, Joern results, Byteman rules and runtime events.

## Decision

All external inputs are normalized into a canonical analysis model with stable identifiers.

## Consequences

- Correlation becomes explicit.
- Storage projections remain replaceable.
- Ambiguous mappings can be represented safely.
