# ADR-0001: Plugins trigger server-side analysis, not the platform

## Status

Accepted

## Context

The Forensics Platform uses Gradle and Maven integrations. These plugins know the local build context and can identify repository, branch, commit, module and runtime-launch information.

Parser execution, Joern execution, AST analysis, BTM generation, normalization, persistence, correlation, replay, graph building and LLM analysis belong to the central Forensics Platform server.

## Decision

Gradle and Maven plugins are producer-side integration adapters. They trigger analysis on the Forensics Platform server by sending repository, branch, commit, build and execution context through the supported server API.

When runtime debugging requires instrumentation, the server creates the BTM files from its analysis and instrumentation plan. The plugin may receive those server-generated BTM files and bind them to the target implementation through the runtime agent so runtime information can be collected during debugging.

The plugin must not collect raw analysis facts, execute parsers, execute Joern, generate BTM files, normalize evidence, persist canonical analysis data, build graph projections, run replay logic or become the analysis platform.

## Consequences

- Plugins remain smaller and focused.
- The central application owns repository analysis, the canonical model and generated analysis artifacts.
- Maven and Gradle can evolve independently as request and runtime-binding adapters.
- Runtime data collection uses server-generated BTM files when instrumentation is required.
