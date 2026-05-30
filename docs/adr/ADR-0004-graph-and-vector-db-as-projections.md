# ADR-0004: Graph DB and Vector DB are projections

## Status

Accepted

## Context

The platform needs graph navigation and semantic search, but storage technologies are not finally selected.

## Decision

Graph DB and Vector DB are projections derived from the canonical model. They are not the primary source of truth.

## Consequences

- Storage technology remains replaceable.
- Projection rebuilds are possible.
- The domain model remains independent from database-specific structures.
