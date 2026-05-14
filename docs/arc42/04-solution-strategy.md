# 4. Solution Strategy

## 4.1 Strategy Overview

The Forensics Platform separates data production from central analysis. Build plugins and tool adapters produce facts. The central platform normalizes, persists, correlates and analyzes them.

## 4.2 Core Strategy

1. Use Gradle and Maven plugins only as fact producers.
2. Normalize all inputs into a canonical analysis model.
3. Use stable IDs for classes, methods, callsites, branches, rules and runtime events.
4. Generate Byteman rules from an explicit instrumentation plan.
5. Collect runtime events with stable `ruleId` and `methodKey` references.
6. Build incidents from exception events.
7. Reconstruct replay timelines by correlation ID, trace ID, thread ID and sequence.
8. Build graph and vector projections from the canonical model.
9. Provide curated evidence packages to the LLM.
10. Keep repair automation gated by tests, quality gates and review.

## 4.3 MVP Strategy

The MVP focuses on read-only analysis:

- Static fact import
- Canonical model persistence
- Joern result import or attachment
- Byteman rule generation with stable rule IDs
- JSONL runtime event import
- Exception incident creation
- CorrelationID-based replay
- Simple graph projection
- LLM root-cause explanation without code modification

## 4.4 Non-MVP Scope

The following items are explicitly postponed:

- Automated patch generation
- Automated pull request creation
- Automated staging or production deployment
- Full Vector DB integration
- Production-ready multi-tenant architecture
- Complete graph UI with all layers
