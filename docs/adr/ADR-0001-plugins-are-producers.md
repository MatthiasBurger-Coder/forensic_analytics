# ADR-0001: Plugins are producers, not the platform

## Status

Accepted

## Context

The Forensics Platform uses Gradle and Maven integrations. These plugins can collect build context, source roots, dependencies and raw analysis facts.

## Decision

Gradle and Maven plugins are fact producers and integration adapters. The central Forensics Platform owns normalization, persistence, correlation, replay, graph building, LLM analysis and repair orchestration.

## Consequences

- Plugins remain smaller and focused.
- The central application owns the canonical model.
- Maven and Gradle can evolve independently as adapters.
